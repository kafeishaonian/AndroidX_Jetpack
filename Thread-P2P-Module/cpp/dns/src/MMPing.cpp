#include "../include/MMPing.h"
#include "../include/MMDNSCommon.h"
#include <unistd.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <arpa/inet.h>
#include <cstring>
#include <chrono>

namespace mmdns {

MMPing::MMPing()
    : icmpSocket_(-1), packetSize_(64), ttl_(64) {
    identifier_ = getpid() & 0xFFFF;
}

MMPing::~MMPing() {
    closeSocket();
}

bool MMPing::createSocket() {
    icmpSocket_ = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
    if (icmpSocket_ < 0) {
        Logger::log(LogLevel::ERROR, "MMPing", "Failed to create ICMP socket (需要root权限)");
        return false;
    }
    
    // 设置TTL
    if (setsockopt(icmpSocket_, IPPROTO_IP, IP_TTL, &ttl_, sizeof(ttl_)) < 0) {
        Logger::log(LogLevel::WARN, "MMPing", "Failed to set TTL");
    }
    
    return true;
}

void MMPing::closeSocket() {
    if (icmpSocket_ >= 0) {
        close(icmpSocket_);
        icmpSocket_ = -1;
    }
}

PingResult MMPing::ping(const std::string& ip, int count, int timeoutMs) {
    PingResult result;
    result.ip = ip;
    result.sent = count;
    
    if (!createSocket()) {
        return result;
    }
    
    int totalRtt = 0;
    int minRtt = INT_MAX;
    int maxRtt = 0;
    
    for (int i = 0; i < count; ++i) {
        IcmpEchoReply reply = pingOnce(ip, i, timeoutMs);
        result.replies.push_back(reply);
        
        if (reply.success) {
            result.received++;
            totalRtt += reply.rtt;
            if (reply.rtt < minRtt) minRtt = reply.rtt;
            if (reply.rtt > maxRtt) maxRtt = reply.rtt;
        }
        
        // 间隔一段时间再发送下一个
        if (i < count - 1) {
            usleep(100000); // 100ms
        }
    }
    
    closeSocket();
    
    if (result.received > 0) {
        result.avgRtt = totalRtt / result.received;
        result.minRtt = minRtt;
        result.maxRtt = maxRtt;
        result.lossRate = ((count - result.received) * 100) / count;
    }
    
    return result;
}

IcmpEchoReply MMPing::pingOnce(const std::string& ip, int sequence, int timeoutMs) {
    IcmpEchoReply reply;
    reply.ip = ip;
    reply.sequence = sequence;
    
    // 构造ICMP包
    char sendBuffer[packetSize_];
    memset(sendBuffer, 0, sizeof(sendBuffer));
    buildIcmpPacket(sendBuffer, sequence);
    
    // 发送ICMP请求
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    inet_pton(AF_INET, ip.c_str(), &addr.sin_addr);
    
    auto sendTime = std::chrono::steady_clock::now();
    
    if (sendto(icmpSocket_, sendBuffer, packetSize_, 0,
               (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        Logger::log(LogLevel::ERROR, "MMPing", "Failed to send ICMP packet");
        return reply;
    }
    
    // 等待回复
    if (!waitForReply(timeoutMs)) {
        return reply;
    }
    
    // 接收回复
    char recvBuffer[1024];
    struct sockaddr_in fromAddr;
    socklen_t fromLen = sizeof(fromAddr);
    
    ssize_t recvLen = recvfrom(icmpSocket_, recvBuffer, sizeof(recvBuffer), 0,
                               (struct sockaddr*)&fromAddr, &fromLen);
    
    if (recvLen > 0) {
        auto recvTime = std::chrono::steady_clock::now();
        auto rtt = std::chrono::duration_cast<std::chrono::milliseconds>(
            recvTime - sendTime).count();
        
        if (parseIcmpReply(recvBuffer, recvLen, reply)) {
            reply.rtt = rtt;
            reply.success = true;
        }
    }
    
    return reply;
}

void MMPing::buildIcmpPacket(char* buffer, int sequence) {
    struct icmp* icmpHdr = (struct icmp*)buffer;
    icmpHdr->icmp_type = ICMP_ECHO;
    icmpHdr->icmp_code = 0;
    icmpHdr->icmp_id = identifier_;
    icmpHdr->icmp_seq = sequence;
    icmpHdr->icmp_cksum = 0;
    
    // 填充数据
    for (int i = sizeof(struct icmp); i < packetSize_; ++i) {
        buffer[i] = i;
    }
    
    // 计算校验和
    icmpHdr->icmp_cksum = calculateChecksum(buffer, packetSize_);
}

bool MMPing::parseIcmpReply(const char* buffer, int len, IcmpEchoReply& reply) {
    if (len < (int)(sizeof(struct ip) + sizeof(struct icmp))) {
        return false;
    }
    
    // 跳过IP头
    struct ip* ipHdr = (struct ip*)buffer;
    int ipHdrLen = ipHdr->ip_hl << 2;
    
    struct icmp* icmpHdr = (struct icmp*)(buffer + ipHdrLen);
    
    if (icmpHdr->icmp_type == ICMP_ECHOREPLY &&
        icmpHdr->icmp_id == identifier_) {
        reply.ttl = ipHdr->ip_ttl;
        return true;
    }
    
    return false;
}

uint16_t MMPing::calculateChecksum(const char* buffer, int len) {
    uint32_t sum = 0;
    const uint16_t* ptr = (const uint16_t*)buffer;
    
    while (len > 1) {
        sum += *ptr++;
        len -= 2;
    }
    
    if (len == 1) {
        sum += *(const uint8_t*)ptr;
    }
    
    sum = (sum >> 16) + (sum & 0xFFFF);
    sum += (sum >> 16);
    
    return ~sum;
}

bool MMPing::waitForReply(int timeoutMs) {
    fd_set readSet;
    FD_ZERO(&readSet);
    FD_SET(icmpSocket_, &readSet);
    
    struct timeval timeout;
    timeout.tv_sec = timeoutMs / 1000;
    timeout.tv_usec = (timeoutMs % 1000) * 1000;
    
    int result = select(icmpSocket_ + 1, &readSet, nullptr, nullptr, &timeout);
    return result > 0;
}

int MMPing::getAverageRtt(const std::vector<IcmpEchoReply>& replies) {
    int total = 0;
    int count = 0;
    
    for (const auto& reply : replies) {
        if (reply.success) {
            total += reply.rtt;
            count++;
        }
    }
    
    return count > 0 ? (total / count) : -1;
}

} // namespace mmdns