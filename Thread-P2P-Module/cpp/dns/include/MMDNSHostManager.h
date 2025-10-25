#pragma once

#include "MMDNSCommon.h"
#include "MMDNSHostModel.h"
#include "MMDNSDataCache.h"
#include <mutex>

namespace mmdns {

class MMDNSDataCache;

class MMDNSHostManager {
public:
    MMDNSHostManager();
    ~MMDNSHostManager();
    
    // 主机管理
    std::shared_ptr<MMDNSHostModel> getHost(const std::string& hostname);
    void updateHost(std::shared_ptr<MMDNSHostModel> host);
    void removeHost(const std::string& hostname);
    void clearCache();
    
    // 批量操作
    std::vector<std::shared_ptr<MMDNSHostModel>> getAllHosts();
    int getHostCount() const;
    
    // 缓存管理
    void setDataCache(std::shared_ptr<MMDNSDataCache> cache) { dataCache_ = cache; }
    void loadFromCache();
    void saveToCache();
    
    // 清理过期缓存
    void cleanExpiredHosts(int expireSeconds = 3600);
    
    // 统计信息
    struct CacheStats {
        int totalHosts;
        int validHosts;
        int expiredHosts;
        long oldestTimestamp;
        long newestTimestamp;
    };
    CacheStats getStats() const;
    
private:
    std::map<std::string, std::shared_ptr<MMDNSHostModel>> hostCache_;
    mutable std::mutex cacheMutex_;
    std::shared_ptr<MMDNSDataCache> dataCache_;
    
    // 检查主机是否过期
    bool isHostExpired(const std::shared_ptr<MMDNSHostModel>& host, int expireSeconds) const;
};

} // namespace mmdns