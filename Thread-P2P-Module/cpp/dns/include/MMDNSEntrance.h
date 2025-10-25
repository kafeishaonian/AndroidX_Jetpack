#pragma once

#include "MMDNSCommon.h"
#include "MMDNSServer.h"
#include <mutex>

namespace mmdns {

// DNS入口接口
class MMDNSEntrance {
public:
    virtual ~MMDNSEntrance() = default;
    
    // 初始化
    virtual void init() = 0;
    
    // DNS解析
    virtual std::string resolveHost(const std::string& hostname) = 0;
    virtual void resolveHostAsync(const std::string& hostname, TaskCallback callback) = 0;
    
    // 获取所有IP
    virtual std::vector<std::string> getAllIPs(const std::string& hostname) = 0;
    
    // 网络状态
    virtual void setNetworkState(MMDNSAppNetState state) = 0;
    
    // 清理
    virtual void clear() = 0;
};

// DNS入口实现
class MMDNSEntranceImpl : public MMDNSEntrance, 
                          public std::enable_shared_from_this<MMDNSEntranceImpl> {
public:
    MMDNSEntranceImpl();
    ~MMDNSEntranceImpl() override;
    
    // 实现接口
    void init() override;
    std::string resolveHost(const std::string& hostname) override;
    void resolveHostAsync(const std::string& hostname, TaskCallback callback) override;
    std::vector<std::string> getAllIPs(const std::string& hostname) override;
    void setNetworkState(MMDNSAppNetState state) override;
    void clear() override;
    
    // DNS服务器配置
    void setDohServer(const std::string& server);
    void enableSystemDNS(bool enable);
    void enableHttpDNS(bool enable);
    void enableLocalCache(bool enable);
    
    // 缓存配置
    void setCacheDir(const std::string& dir);
    void setCacheSize(size_t size);
    void setCacheExpireTime(int seconds);
    
    // 线程配置
    void setThreadCount(int count);
    
    // 获取统计信息
    MMDNSServer::ServerStats getStats() const;
    
    // 单例管理
    static std::shared_ptr<MMDNSEntrance> getInstance(const std::string& key = "default");
    static void removeInstance(const std::string& key);
    static void clearAllInstances();
    
private:
    std::shared_ptr<MMDNSServer> dnsServer_;
    std::shared_ptr<MMDNSHostManager> hostManager_;
    std::shared_ptr<MMDNSDataCache> dataCache_;
    std::shared_ptr<MMDNSSpeedChecker> speedChecker_;
    
    MMDNSAppNetState networkState_;
    std::string cacheDir_;
    std::string dohServer_;
    
    bool systemDNSEnabled_;
    bool httpDNSEnabled_;
    bool localCacheEnabled_;
    
    mutable std::mutex mutex_;
    
    // 初始化DNS处理器
    void initDNSHandlers();
    
    // 单例存储
    static std::unordered_map<std::string, std::shared_ptr<MMDNSEntrance>> instances_;
    static std::mutex instanceMutex_;
};

} // namespace mmdns