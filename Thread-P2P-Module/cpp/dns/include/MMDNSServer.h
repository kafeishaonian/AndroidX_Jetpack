#pragma once

#include "MMDNSCommon.h"
#include "MMDNSBlockingQueue.h"
#include "MMDNSServerTask.h"
#include "MMDNSServerHandle.h"
#include "MMDNSHostManager.h"
#include "MMDNSSpeedChecker.h"
#include <thread>
#include <atomic>

namespace mmdns {

class MMDNSServer {
public:
    MMDNSServer();
    ~MMDNSServer();
    
    // 初始化和生命周期
    void start();
    void stop();
    bool isRunning() const { return running_.load(); }
    
    // DNS解析接口
    std::shared_ptr<MMDNSHostModel> resolveSync(const std::string& hostname);
    void resolveAsync(const std::string& hostname, TaskCallback callback);
    
    // 配置管理
    void addServerHandle(DNSServerType type, std::shared_ptr<MMDNSServerHandle> handle);
    void removeServerHandle(DNSServerType type);
    void setThreadCount(int count);
    void setQueueSize(size_t size);
    
    // 测速配置
    void setSpeedChecker(std::shared_ptr<MMDNSSpeedChecker> checker) {
        speedChecker_ = checker;
    }
    
    // 缓存管理
    void setHostManager(std::shared_ptr<MMDNSHostManager> manager) {
        hostManager_ = manager;
    }
    
    std::shared_ptr<MMDNSHostManager> getHostManager() const {
        return hostManager_;
    }
    
    // 统计信息
    struct ServerStats {
        int totalRequests;
        int successRequests;
        int failedRequests;
        int cachedRequests;
        int queueSize;
        int threadCount;
    };
    ServerStats getStats() const;
    
private:
    // 工作线程
    void workerLoop();
    void processTask(std::shared_ptr<MMDNSServerTask> task);
    
    // DNS解析实现
    std::shared_ptr<MMDNSHostModel> performResolve(const std::string& hostname);
    
    // 成员变量
    std::map<DNSServerType, std::shared_ptr<MMDNSServerHandle>> serverHandles_;
    std::shared_ptr<MMDNSBlockingQueue<std::shared_ptr<MMDNSServerTask>>> taskQueue_;
    std::vector<std::shared_ptr<std::thread>> workerThreads_;
    std::shared_ptr<MMDNSHostManager> hostManager_;
    std::shared_ptr<MMDNSSpeedChecker> speedChecker_;
    
    std::atomic<bool> running_;
    int threadCount_;
    
    // 统计数据
    mutable std::mutex statsMutex_;
    std::atomic<int> totalRequests_;
    std::atomic<int> successRequests_;
    std::atomic<int> failedRequests_;
    std::atomic<int> cachedRequests_;
    
    // 禁止拷贝
    MMDNSServer(const MMDNSServer&) = delete;
    MMDNSServer& operator=(const MMDNSServer&) = delete;
};

} // namespace mmdns