#include "../include/MMDNSHostManager.h"
#include "../include/MMDNSCommon.h"
#include <chrono>
#include <algorithm>
#include <thread>

namespace mmdns {

MMDNSHostManager::MMDNSHostManager() {
    Logger::log(LogLevel::INFO, "MMDNSHostManager", "主机管理器已创建");
}

MMDNSHostManager::~MMDNSHostManager() {
    clearCache();
    Logger::log(LogLevel::INFO, "MMDNSHostManager", "主机管理器已销毁");
}

std::shared_ptr<MMDNSHostModel> MMDNSHostManager::getHost(const std::string& hostname) {
    std::lock_guard<std::mutex> lock(cacheMutex_);
    
    auto it = hostCache_.find(hostname);
    if (it != hostCache_.end()) {
        Logger::log(LogLevel::DEBUG, "MMDNSHostManager", 
            "获取主机: " + hostname);
        return it->second;
    }
    
    // 尝试从持久化缓存加载
    if (dataCache_) {
        std::string jsonData = dataCache_->load(hostname);
        if (!jsonData.empty()) {
            auto host = MMDNSHostModel::fromJson(jsonData);
            if (host) {
                hostCache_[hostname] = host;
                Logger::log(LogLevel::DEBUG, "MMDNSHostManager",
                    "从持久化缓存加载: " + hostname);
                return host;
            }
        }
    }
    
    Logger::log(LogLevel::DEBUG, "MMDNSHostManager",
        "主机不存在: " + hostname);
    return nullptr;
}

void MMDNSHostManager::updateHost(std::shared_ptr<MMDNSHostModel> host) {
    if (!host) {
        return;
    }
    
    std::lock_guard<std::mutex> lock(cacheMutex_);
    
    std::string hostname = host->getHostname();
    hostCache_[hostname] = host;
    
    Logger::log(LogLevel::DEBUG, "MMDNSHostManager",
        "更新主机: " + hostname);
    
    // 异步保存到持久化缓存
    if (dataCache_) {
        std::thread([this, hostname, host]() {
            try {
                std::string jsonData = host->toJson();
                dataCache_->save(hostname, jsonData);
            } catch (const std::exception& e) {
                Logger::log(LogLevel::ERROR, "MMDNSHostManager",
                    "保存缓存失败: " + std::string(e.what()));
            }
        }).detach();
    }
}

void MMDNSHostManager::removeHost(const std::string& hostname) {
    std::lock_guard<std::mutex> lock(cacheMutex_);
    
    hostCache_.erase(hostname);
    
    if (dataCache_) {
        dataCache_->remove(hostname);
    }
    
    Logger::log(LogLevel::DEBUG, "MMDNSHostManager",
        "删除主机: " + hostname);
}

void MMDNSHostManager::clearCache() {
    std::lock_guard<std::mutex> lock(cacheMutex_);
    
    int count = hostCache_.size();
    hostCache_.clear();
    
    if (dataCache_) {
        dataCache_->clear();
    }
    
    Logger::log(LogLevel::INFO, "MMDNSHostManager",
        "清空缓存，删除 " + std::to_string(count) + " 个主机");
}

std::vector<std::shared_ptr<MMDNSHostModel>> MMDNSHostManager::getAllHosts() {
    std::lock_guard<std::mutex> lock(cacheMutex_);
    
    std::vector<std::shared_ptr<MMDNSHostModel>> hosts;
    hosts.reserve(hostCache_.size());
    
    for (const auto& pair : hostCache_) {
        hosts.push_back(pair.second);
    }
    
    return hosts;
}

int MMDNSHostManager::getHostCount() const {
    std::lock_guard<std::mutex> lock(cacheMutex_);
    return hostCache_.size();
}

void MMDNSHostManager::loadFromCache() {
    if (!dataCache_) {
        Logger::log(LogLevel::WARN, "MMDNSHostManager",
            "数据缓存未设置，无法加载");
        return;
    }
    
    Logger::log(LogLevel::INFO, "MMDNSHostManager", "开始加载缓存");
    
    // TODO: 实现从文件系统加载所有缓存的主机
    // 这需要DataCache支持列出所有缓存的key
    
    Logger::log(LogLevel::INFO, "MMDNSHostManager", "缓存加载完成");
}

void MMDNSHostManager::saveToCache() {
    if (!dataCache_) {
        Logger::log(LogLevel::WARN, "MMDNSHostManager",
            "数据缓存未设置，无法保存");
        return;
    }
    
    std::lock_guard<std::mutex> lock(cacheMutex_);
    
    Logger::log(LogLevel::INFO, "MMDNSHostManager",
        "开始保存缓存，共 " + std::to_string(hostCache_.size()) + " 个主机");
    
    int savedCount = 0;
    for (const auto& pair : hostCache_) {
        try {
            std::string jsonData = pair.second->toJson();
            dataCache_->save(pair.first, jsonData);
            savedCount++;
        } catch (const std::exception& e) {
            Logger::log(LogLevel::ERROR, "MMDNSHostManager",
                "保存失败 " + pair.first + ": " + e.what());
        }
    }
    
    Logger::log(LogLevel::INFO, "MMDNSHostManager",
        "缓存保存完成，成功 " + std::to_string(savedCount) + " 个");
}

void MMDNSHostManager::cleanExpiredHosts(int expireSeconds) {
    std::lock_guard<std::mutex> lock(cacheMutex_);
    
    auto now = std::chrono::duration_cast<std::chrono::seconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
    
    int cleanedCount = 0;
    auto it = hostCache_.begin();
    while (it != hostCache_.end()) {
        if (isHostExpired(it->second, expireSeconds)) {
            if (dataCache_) {
                dataCache_->remove(it->first);
            }
            it = hostCache_.erase(it);
            cleanedCount++;
        } else {
            ++it;
        }
    }
    
    if (cleanedCount > 0) {
        Logger::log(LogLevel::INFO, "MMDNSHostManager",
            "清理过期主机 " + std::to_string(cleanedCount) + " 个");
    }
}

MMDNSHostManager::CacheStats MMDNSHostManager::getStats() const {
    std::lock_guard<std::mutex> lock(cacheMutex_);
    
    CacheStats stats;
    stats.totalHosts = hostCache_.size();
    stats.validHosts = 0;
    stats.expiredHosts = 0;
    stats.oldestTimestamp = LONG_MAX;
    stats.newestTimestamp = 0;
    
    for (const auto& pair : hostCache_) {
        auto host = pair.second;
        long updateTime = host->getUpdateTime();
        
        if (host->hasValidIP()) {
            stats.validHosts++;
        }
        
        if (updateTime < stats.oldestTimestamp) {
            stats.oldestTimestamp = updateTime;
        }
        if (updateTime > stats.newestTimestamp) {
            stats.newestTimestamp = updateTime;
        }
    }
    
    stats.expiredHosts = stats.totalHosts - stats.validHosts;
    
    return stats;
}

bool MMDNSHostManager::isHostExpired(
    const std::shared_ptr<MMDNSHostModel>& host,
    int expireSeconds) const {
    
    if (!host) {
        return true;
    }
    
    auto now = std::chrono::duration_cast<std::chrono::seconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
    
    long age = now - host->getUpdateTime();
    return age > expireSeconds;
}

} // namespace mmdns