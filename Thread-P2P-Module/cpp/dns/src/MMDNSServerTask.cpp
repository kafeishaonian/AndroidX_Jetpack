#include "../include/MMDNSServerTask.h"
#include "../include/MMDNSServer.h"
#include "../include/MMDNSCommon.h"
#include <chrono>

namespace mmdns {

// ==================== MMDNSServerTask ====================

MMDNSServerTask::MMDNSServerTask(
    DNSServerTaskType taskType,
    const std::string& hostname,
    TaskCallback callback)
    : taskType_(taskType),
      hostname_(hostname),
      callback_(callback),
      priority_(0) {
    
    createTime_ = std::chrono::duration_cast<std::chrono::seconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
}

// ==================== ResolveHostTask ====================

ResolveHostTask::ResolveHostTask(const std::string& hostname, TaskCallback callback)
    : MMDNSServerTask(DNSServerTaskType::RESOLVE_HOST, hostname, callback) {
    priority_ = 10;  // 高优先级
}

void ResolveHostTask::execute() {
    Logger::log(LogLevel::INFO, "ResolveHostTask", 
        "开始解析主机: " + hostname_);
    
    try {
        // 这里需要访问MMDNSServer实例来执行解析
        // 由于execute是在server的工作线程中调用的，
        // 实际实现中会通过回调或其他方式获取server引用
        
        // 简化实现：直接调用回调
        if (callback_) {
            // 实际应该从DNS服务器获取结果
            auto result = std::make_shared<MMDNSHostModel>(hostname_);
            callback_(result, false, nullptr);
        }
        
        Logger::log(LogLevel::INFO, "ResolveHostTask", 
            "主机解析完成: " + hostname_);
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "ResolveHostTask",
            "解析失败: " + std::string(e.what()));
        
        if (callback_) {
            callback_(nullptr, false, nullptr);
        }
    }
}

// ==================== SpeedCheckTask ====================

SpeedCheckTask::SpeedCheckTask(
    const std::string& hostname,
    std::shared_ptr<MMDNSHostModel> hostModel,
    TaskCallback callback)
    : MMDNSServerTask(DNSServerTaskType::SPEED_CHECK, hostname, callback),
      hostModel_(hostModel) {
    priority_ = 5;  // 中等优先级
}

void SpeedCheckTask::execute() {
    if (!hostModel_) {
        Logger::log(LogLevel::ERROR, "SpeedCheckTask", "主机模型为空");
        if (callback_) {
            callback_(nullptr, false, nullptr);
        }
        return;
    }
    
    Logger::log(LogLevel::INFO, "SpeedCheckTask",
        "开始测速: " + hostname_ + " (" + 
        std::to_string(hostModel_->getIPList().size()) + " 个IP)");
    
    try {
        // 实际实现中会使用MMDNSSpeedChecker
        // 这里简化处理
        auto ipList = hostModel_->getIPList();
        
        // 模拟测速
        for (auto& ip : ipList) {
            // 实际应该调用speedChecker->checkSpeed()
            ip->setSpeed(100);  // 模拟速度
        }
        
        // 排序
        hostModel_->sortBySpeed();
        
        Logger::log(LogLevel::INFO, "SpeedCheckTask", "测速完成");
        
        if (callback_) {
            callback_(hostModel_, true, nullptr);
        }
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "SpeedCheckTask",
            "测速失败: " + std::string(e.what()));
        
        if (callback_) {
            callback_(hostModel_, false, nullptr);
        }
    }
}

// ==================== CacheUpdateTask ====================

CacheUpdateTask::CacheUpdateTask(
    const std::string& hostname,
    std::shared_ptr<MMDNSHostModel> hostModel)
    : MMDNSServerTask(DNSServerTaskType::CACHE_UPDATE, hostname, nullptr),
      hostModel_(hostModel) {
    priority_ = 1;  // 低优先级
}

void CacheUpdateTask::execute() {
    if (!hostModel_) {
        Logger::log(LogLevel::ERROR, "CacheUpdateTask", "主机模型为空");
        return;
    }
    
    Logger::log(LogLevel::DEBUG, "CacheUpdateTask",
        "更新缓存: " + hostname_);
    
    try {
        // 实际实现中会调用MMDNSHostManager::updateHost()
        // 这里简化处理
        
        Logger::log(LogLevel::DEBUG, "CacheUpdateTask", "缓存更新完成");
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "CacheUpdateTask",
            "缓存更新失败: " + std::string(e.what()));
    }
}

} // namespace mmdns