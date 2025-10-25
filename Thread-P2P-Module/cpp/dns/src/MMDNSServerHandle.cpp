#include "../include/MMDNSServerHandle.h"
#include "../include/MMDNSHostManager.h"
#include "../include/MMDNSCommon.h"
#include "../include/MMDNSHttpClient.h"
#include <netdb.h>
#include <arpa/inet.h>
#include <cstring>
#include <sstream>
#include <chrono>

namespace mmdns {

// ==================== MMDNSServerHandle ====================

MMDNSServerHandle::MMDNSServerHandle(DNSServerType serverType)
    : serverType_(serverType) {
}

// ==================== MMDNSSystemServerHandle ====================

MMDNSSystemServerHandle::MMDNSSystemServerHandle()
    : MMDNSServerHandle(DNSServerType::SYSTEM) {
}

std::shared_ptr<MMDNSHostModel> MMDNSSystemServerHandle::resolve(const std::string& hostname) {
    Logger::log(LogLevel::INFO, "SystemDNS", "解析主机: " + hostname);
    
    std::vector<std::string> ipList;
    if (!resolveWithGetaddrinfo(hostname, ipList)) {
        Logger::log(LogLevel::ERROR, "SystemDNS", "解析失败: " + hostname);
        return nullptr;
    }
    
    auto hostModel = std::make_shared<MMDNSHostModel>(hostname);
    hostModel->setServerType(DNSServerType::SYSTEM);
    
    for (const auto& ip : ipList) {
        auto ipModel = std::make_shared<MMDNSIPModel>(ip);
        hostModel->addIP(ipModel);
    }
    
    Logger::log(LogLevel::INFO, "SystemDNS",
        "解析成功: " + hostname + " -> " + std::to_string(ipList.size()) + " 个IP");
    
    return hostModel;
}

bool MMDNSSystemServerHandle::resolveWithGetaddrinfo(
    const std::string& hostname,
    std::vector<std::string>& ipList) {
    
    struct addrinfo hints;
    struct addrinfo* result = nullptr;
    
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;     // IPv4 或 IPv6
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = AI_ADDRCONFIG;
    
    int ret = getaddrinfo(hostname.c_str(), nullptr, &hints, &result);
    if (ret != 0) {
        Logger::log(LogLevel::ERROR, "SystemDNS",
            "getaddrinfo失败: " + std::string(gai_strerror(ret)));
        return false;
    }
    
    // 遍历结果
    for (struct addrinfo* rp = result; rp != nullptr; rp = rp->ai_next) {
        char ipstr[INET6_ADDRSTRLEN];
        void* addr;
        
        if (rp->ai_family == AF_INET) {
            // IPv4
            struct sockaddr_in* ipv4 = (struct sockaddr_in*)rp->ai_addr;
            addr = &(ipv4->sin_addr);
        } else if (rp->ai_family == AF_INET6) {
            // IPv6
            struct sockaddr_in6* ipv6 = (struct sockaddr_in6*)rp->ai_addr;
            addr = &(ipv6->sin6_addr);
        } else {
            continue;
        }
        
        inet_ntop(rp->ai_family, addr, ipstr, sizeof(ipstr));
        ipList.push_back(std::string(ipstr));
    }
    
    freeaddrinfo(result);
    return !ipList.empty();
}

// ==================== MMDNSHttpServerHandle ====================

MMDNSHttpServerHandle::MMDNSHttpServerHandle(const std::string& dohServer)
    : MMDNSServerHandle(DNSServerType::HTTP_DNS),
      dohServer_(dohServer),
      httpClient_(std::make_shared<MMDNSHttpClient>()) {
    
    // 配置HTTP客户端
    httpClient_->setTimeout(5);
    httpClient_->setConnectTimeout(3);
    httpClient_->setVerifySSL(true);
}

std::shared_ptr<MMDNSHostModel> MMDNSHttpServerHandle::resolve(const std::string& hostname) {
    Logger::log(LogLevel::INFO, "HttpDNS", "解析主机: " + hostname);
    
    // 发送DoH请求
    std::string response = sendDohRequest(hostname);
    if (response.empty()) {
        Logger::log(LogLevel::ERROR, "HttpDNS", "DoH请求失败");
        return nullptr;
    }
    
    // 解析响应
    return parseDohResponse(response, hostname);
}

std::string MMDNSHttpServerHandle::sendDohRequest(const std::string& hostname) {
    if (!httpClient_) {
        Logger::log(LogLevel::ERROR, "HttpDNS", "HTTP客户端未初始化");
        return "";
    }
    
    Logger::log(LogLevel::INFO, "HttpDNS", "发送DoH请求: " + hostname);
    
    // 首先尝试A记录（IPv4）
    std::string response = httpClient_->sendDohRequest(dohServer_, hostname, "A");
    
    // 如果启用IPv6，也尝试AAAA记录
    // TODO: 可以并行查询A和AAAA记录
    
    return response;
}

std::shared_ptr<MMDNSHostModel> MMDNSHttpServerHandle::parseDohResponse(
    const std::string& response,
    const std::string& hostname) {
    
    if (response.empty()) {
        Logger::log(LogLevel::ERROR, "HttpDNS", "DoH响应为空");
        return nullptr;
    }
    
    if (!httpClient_) {
        Logger::log(LogLevel::ERROR, "HttpDNS", "HTTP客户端未初始化");
        return nullptr;
    }
    
    // 使用HTTP客户端解析JSON响应
    auto ipList = httpClient_->parseDohResponse(response);
    
    if (ipList.empty()) {
        Logger::log(LogLevel::WARN, "HttpDNS", "未从DoH响应中提取到IP地址");
        return nullptr;
    }
    
    // 创建主机模型
    auto hostModel = std::make_shared<MMDNSHostModel>(hostname);
    hostModel->setServerType(DNSServerType::HTTP_DNS);
    
    // 添加IP地址
    for (const auto& ip : ipList) {
        auto ipModel = std::make_shared<MMDNSIPModel>(ip);
        hostModel->addIP(ipModel);
    }
    
    Logger::log(LogLevel::INFO, "HttpDNS",
        "DoH解析成功: " + hostname + " -> " + std::to_string(ipList.size()) + " 个IP");
    
    return hostModel;
}

// ==================== MMDNSLocalServerHandle ====================

MMDNSLocalServerHandle::MMDNSLocalServerHandle()
    : MMDNSServerHandle(DNSServerType::LOCAL),
      cacheExpireTime_(3600) {  // 默认1小时过期
}

std::shared_ptr<MMDNSHostModel> MMDNSLocalServerHandle::resolve(const std::string& hostname) {
    std::lock_guard<std::mutex> lock(cacheMutex_);
    
    auto it = cache_.find(hostname);
    if (it != cache_.end()) {
        auto host = it->second;
        
        // 检查是否过期
        if (!isCacheExpired(host)) {
            Logger::log(LogLevel::DEBUG, "LocalDNS", "缓存命中: " + hostname);
            return host;
        } else {
            Logger::log(LogLevel::DEBUG, "LocalDNS", "缓存已过期: " + hostname);
            cache_.erase(it);
        }
    }
    
    Logger::log(LogLevel::DEBUG, "LocalDNS", "缓存未命中: " + hostname);
    return nullptr;
}

void MMDNSLocalServerHandle::addToCache(
    const std::string& hostname,
    std::shared_ptr<MMDNSHostModel> host) {
    
    if (!host) {
        return;
    }
    
    std::lock_guard<std::mutex> lock(cacheMutex_);
    cache_[hostname] = host;
    
    Logger::log(LogLevel::DEBUG, "LocalDNS", "添加到缓存: " + hostname);
}

void MMDNSLocalServerHandle::removeFromCache(const std::string& hostname) {
    std::lock_guard<std::mutex> lock(cacheMutex_);
    cache_.erase(hostname);
    
    Logger::log(LogLevel::DEBUG, "LocalDNS", "从缓存删除: " + hostname);
}

void MMDNSLocalServerHandle::clearCache() {
    std::lock_guard<std::mutex> lock(cacheMutex_);
    cache_.clear();
    
    Logger::log(LogLevel::INFO, "LocalDNS", "清空缓存");
}

bool MMDNSLocalServerHandle::isCacheExpired(const std::shared_ptr<MMDNSHostModel>& host) const {
    if (!host) {
        return true;
    }
    
    auto now = std::chrono::duration_cast<std::chrono::seconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
    
    long age = now - host->getUpdateTime();
    return age > cacheExpireTime_;
}

} // namespace mmdns