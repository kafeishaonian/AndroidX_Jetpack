#pragma once

#include "MMDNSCommon.h"
#include "MMDNSHostModel.h"
#include "MMDNSSocket.h"
#include <mutex>

namespace mmdns {

class MMDNSHostManager;
class MMDNSHttpClient;

// DNS服务器句柄基类
class MMDNSServerHandle {
public:
    explicit MMDNSServerHandle(DNSServerType serverType);
    virtual ~MMDNSServerHandle() = default;
    
    // 解析主机名
    virtual std::shared_ptr<MMDNSHostModel> resolve(const std::string& hostname) = 0;
    
    // 获取服务器类型
    DNSServerType getServerType() const { return serverType_; }
    
    // 设置主机管理器
    void setHostManager(std::shared_ptr<MMDNSHostManager> manager) {
        hostManager_ = manager;
    }
    
protected:
    DNSServerType serverType_;
    std::shared_ptr<MMDNSHostManager> hostManager_;
};

// 系统DNS服务器句柄
class MMDNSSystemServerHandle : public MMDNSServerHandle {
public:
    MMDNSSystemServerHandle();
    ~MMDNSSystemServerHandle() override = default;
    
    std::shared_ptr<MMDNSHostModel> resolve(const std::string& hostname) override;
    
private:
    // 使用getaddrinfo进行系统DNS解析
    bool resolveWithGetaddrinfo(const std::string& hostname, 
                               std::vector<std::string>& ipList);
};

// HTTP DNS (DoH) 服务器句柄
class MMDNSHttpServerHandle : public MMDNSServerHandle {
public:
    explicit MMDNSHttpServerHandle(const std::string& dohServer);
    ~MMDNSHttpServerHandle() override = default;
    
    std::shared_ptr<MMDNSHostModel> resolve(const std::string& hostname) override;
    
    // 设置DoH服务器
    void setDohServer(const std::string& server) { dohServer_ = server; }
    std::string getDohServer() const { return dohServer_; }
    
private:
    std::string dohServer_;  // DoH服务器URL，如 "https://dns.google/dns-query"
    std::shared_ptr<MMDNSHttpClient> httpClient_;  // HTTP客户端
    
    // 发送HTTP DNS查询
    std::string sendDohRequest(const std::string& hostname);
    
    // 解析DoH响应
    std::shared_ptr<MMDNSHostModel> parseDohResponse(const std::string& response,
                                                     const std::string& hostname);
};

// 本地缓存DNS服务器句柄
class MMDNSLocalServerHandle : public MMDNSServerHandle {
public:
    MMDNSLocalServerHandle();
    ~MMDNSLocalServerHandle() override = default;
    
    std::shared_ptr<MMDNSHostModel> resolve(const std::string& hostname) override;
    
    // 缓存管理
    void addToCache(const std::string& hostname, std::shared_ptr<MMDNSHostModel> host);
    void removeFromCache(const std::string& hostname);
    void clearCache();
    
    // 设置缓存过期时间（秒）
    void setCacheExpireTime(int seconds) { cacheExpireTime_ = seconds; }
    
private:
    std::map<std::string, std::shared_ptr<MMDNSHostModel>> cache_;
    std::mutex cacheMutex_;
    int cacheExpireTime_;  // 缓存过期时间（秒）
    
    // 检查缓存是否过期
    bool isCacheExpired(const std::shared_ptr<MMDNSHostModel>& host) const;
};

} // namespace mmdns