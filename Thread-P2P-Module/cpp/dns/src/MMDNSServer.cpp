#include "../include/MMDNSServer.h"
#include "../include/MMDNSCommon.h"
#include <algorithm>
#include <future>

namespace mmdns {

MMDNSServer::MMDNSServer()
    : running_(false),
      threadCount_(Constants::DEFAULT_THREAD_COUNT),
      totalRequests_(0),
      successRequests_(0),
      failedRequests_(0),
      cachedRequests_(0) {
    
    taskQueue_ = std::make_shared<MMDNSBlockingQueue<std::shared_ptr<MMDNSServerTask>>>();
    hostManager_ = std::make_shared<MMDNSHostManager>();
    speedChecker_ = std::make_shared<MMDNSSpeedChecker>();
}

MMDNSServer::~MMDNSServer() {
    stop();
}

void MMDNSServer::start() {
    if (running_.load()) {
        Logger::log(LogLevel::WARN, "MMDNSServer", "服务器已在运行中");
        return;
    }
    
    Logger::log(LogLevel::INFO, "MMDNSServer", "启动DNS服务器...");
    running_.store(true);
    
    // 创建工作线程
    for (int i = 0; i < threadCount_; ++i) {
        workerThreads_.push_back(
            std::make_shared<std::thread>(&MMDNSServer::workerLoop, this)
        );
    }
    
    Logger::log(LogLevel::INFO, "MMDNSServer", 
        "DNS服务器已启动，工作线程数: " + std::to_string(threadCount_));
}

void MMDNSServer::stop() {
    if (!running_.load()) {
        return;
    }
    
    Logger::log(LogLevel::INFO, "MMDNSServer", "停止DNS服务器...");
    running_.store(false);
    
    // 放入终止标记
    for (size_t i = 0; i < workerThreads_.size(); ++i) {
        taskQueue_->tryPut(nullptr);
    }
    
    // 等待所有线程结束
    for (auto& thread : workerThreads_) {
        if (thread && thread->joinable()) {
            thread->join();
        }
    }
    
    workerThreads_.clear();
    taskQueue_->clear();
    
    Logger::log(LogLevel::INFO, "MMDNSServer", "DNS服务器已停止");
}

void MMDNSServer::workerLoop() {
    Logger::log(LogLevel::DEBUG, "MMDNSServer", "工作线程启动");
    
    while (running_.load()) {
        try {
            auto task = taskQueue_->take(std::chrono::milliseconds(1000));
            if (task.has_value() && task.value()) {
                processTask(task.value());
            }
        } catch (const std::exception& e) {
            Logger::log(LogLevel::ERROR, "MMDNSServer", 
                std::string("工作线程异常: ") + e.what());
        }
    }
    
    Logger::log(LogLevel::DEBUG, "MMDNSServer", "工作线程退出");
}

void MMDNSServer::processTask(std::shared_ptr<MMDNSServerTask> task) {
    if (!task) {
        return;
    }
    
    Logger::log(LogLevel::DEBUG, "MMDNSServer", 
        "处理任务: " + task->getHostname());
    
    totalRequests_++;
    
    try {
        task->execute();
        successRequests_++;
    } catch (const std::exception& e) {
        failedRequests_++;
        Logger::log(LogLevel::ERROR, "MMDNSServer",
            "任务执行失败: " + std::string(e.what()));
    }
}

std::shared_ptr<MMDNSHostModel> MMDNSServer::resolveSync(const std::string& hostname) {
    Logger::log(LogLevel::INFO, "MMDNSServer", "同步解析: " + hostname);
    
    // 先从缓存获取
    auto cachedHost = hostManager_->getHost(hostname);
    if (cachedHost && cachedHost->hasValidIP()) {
        Logger::log(LogLevel::INFO, "MMDNSServer", "从缓存获取: " + hostname);
        cachedRequests_++;
        return cachedHost;
    }
    
    // 执行实际解析
    return performResolve(hostname);
}

void MMDNSServer::resolveAsync(const std::string& hostname, TaskCallback callback) {
    Logger::log(LogLevel::INFO, "MMDNSServer", "异步解析: " + hostname);
    
    auto task = std::make_shared<ResolveHostTask>(hostname, callback);
    taskQueue_->put(task);
}

std::shared_ptr<MMDNSHostModel> MMDNSServer::performResolve(const std::string& hostname) {
    std::shared_ptr<MMDNSHostModel> bestResult = nullptr;
    std::vector<std::future<std::shared_ptr<MMDNSHostModel>>> futures;
    
    // 并发查询所有配置的DNS服务器
    for (auto& pair : serverHandles_) {
        auto handler = pair.second;
        futures.push_back(std::async(std::launch::async, [handler, hostname]() {
            return handler->resolve(hostname);
        }));
    }
    
    // 获取第一个成功的结果
    for (auto& future : futures) {
        try {
            auto result = future.get();
            if (result && result->hasValidIP()) {
                bestResult = result;
                break;
            }
        } catch (...) {
            // 忽略单个DNS服务器的错误
        }
    }
    
    if (bestResult && speedChecker_) {
        // 执行测速
        auto ipList = bestResult->getIPList();
        speedChecker_->checkMultiple(ipList);
        bestResult->sortBySpeed();
        
        // 更新到缓存
        hostManager_->updateHost(bestResult);
    }
    
    return bestResult;
}

void MMDNSServer::addServerHandle(DNSServerType type, std::shared_ptr<MMDNSServerHandle> handle) {
    if (handle) {
        handle->setHostManager(hostManager_);
        serverHandles_[type] = handle;
        Logger::log(LogLevel::INFO, "MMDNSServer", 
            "添加DNS处理器: " + std::to_string(static_cast<int>(type)));
    }
}

void MMDNSServer::removeServerHandle(DNSServerType type) {
    serverHandles_.erase(type);
}

void MMDNSServer::setThreadCount(int count) {
    if (count > 0 && count <= 16) {
        threadCount_ = count;
    }
}

void MMDNSServer::setQueueSize(size_t size) {
    // 需要重新创建队列
    if (!running_.load()) {
        taskQueue_ = std::make_shared<MMDNSBlockingQueue<std::shared_ptr<MMDNSServerTask>>>(size);
    }
}

MMDNSServer::ServerStats MMDNSServer::getStats() const {
    ServerStats stats;
    stats.totalRequests = totalRequests_.load();
    stats.successRequests = successRequests_.load();
    stats.failedRequests = failedRequests_.load();
    stats.cachedRequests = cachedRequests_.load();
    stats.queueSize = taskQueue_->size();
    stats.threadCount = workerThreads_.size();
    return stats;
}

} // namespace mmdns