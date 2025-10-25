#pragma once

#include "MMDNSCommon.h"
#include <netinet/ip_icmp.h>

namespace mmdns {

// ICMP回复结构
struct IcmpEchoReply {
    std::string ip;
    int sequence;
    int ttl;
    int rtt;  // 往返时间（毫秒）
    bool success;
    
    IcmpEchoReply() : sequence(0), ttl(0), rtt(-1), success(false) {}
};

// Ping结果
struct PingResult {
    std::string ip;
    int sent;
    int received;
    int lossRate;  // 丢包率（百分比）
    int minRtt;
    int maxRtt;
    int avgRtt;
    std::vector<IcmpEchoReply> replies;
    
    PingResult() : sent(0), received(0), lossRate(100),
                   minRtt(-1), maxRtt(-1), avgRtt(-1) {}
};

class MMPing {
public:
    MMPing();
    ~MMPing();
    
    // Ping操作
    PingResult ping(const std::string& ip, int count = 4, int timeoutMs = 1000);
    IcmpEchoReply pingOnce(const std::string& ip, int sequence, int timeoutMs = 1000);
    
    // 获取平均延迟
    static int getAverageRtt(const std::vector<IcmpEchoReply>& replies);
    
    // 设置参数
    void setPacketSize(int size) { packetSize_ = size; }
    void setTTL(int ttl) { ttl_ = ttl; }
    
private:
    int icmpSocket_;
    int packetSize_;
    int ttl_;
    uint16_t identifier_;
    
    bool createSocket();
    void closeSocket();
    
    // ICMP包构造和解析
    void buildIcmpPacket(char* buffer, int sequence);
    bool parseIcmpReply(const char* buffer, int len, IcmpEchoReply& reply);
    
    // 校验和计算
    uint16_t calculateChecksum(const char* buffer, int len);
    
    // 等待接收
    bool waitForReply(int timeoutMs);
};

} // namespace mmdns