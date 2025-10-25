#include "../include/MMDNSMonitor.h"
#include <algorithm>
#include <sstream>
#include <iomanip>

namespace mmdns {

MMDNSMonitor::MMDNSMonitor()
    : enabled_(true)
    , totalRequests_(0)
    , successfulRequests_(0)
    , failedRequests_(0)
    , cachedRequests_(0)
    , maxSamples_(1000)  // 保留最近1000个样本
    , currentCacheSize_(0)
    , currentCacheCapacity_(0)
    , currentMemoryUsage_(0)
    , currentActiveThreads_(0)
    , currentQueuedTasks_(0) {
}

void MMDNSMonitor::recordResolution(const std::string& hostname,
                                    double duration,
                                    bool success,
                                    bool fromCache) {
    if (!enabled_) return;
    
    totalRequests_++;
    
    if (success) {
        successfulRequests_++;
    } else {
        failedRequests_++;
    }
    
    if (fromCache) {
        cachedRequests_++;
    }
    
    std::lock_guard<std::mutex> lock(mutex_);
    addSample(resolutionTimes_, duration);
    
    Logger::log(LogLevel::DEBUG, "Monitor",
        "Recorded resolution: " + hostname + " " + std::to_string(duration) + "ms " +
        (success ? "success" : "failed") + (fromCache ? " (cached)" : ""));
}

void MMDNSMonitor::recordSpeedCheck(const std::string& ip, double duration) {
    if (!enabled_) return;
    
    std::lock_guard<std::mutex> lock(mutex_);
    addSample(speedCheckTimes_, duration);
    
    Logger::log(LogLevel::DEBUG, "Monitor",
        "Recorded speed check: " + ip + " " + std::to_string(duration) + "ms");
}

void MMDNSMonitor::recordError(const std::string& errorType) {
    if (!enabled_) return;
    
    std::lock_guard<std::mutex> lock(mutex_);
    errorCounts_[errorType]++;
    
    Logger::log(LogLevel::DEBUG, "Monitor", "Recorded error: " + errorType);
}

void MMDNSMonitor::updateCacheStats(size_t size, size_t capacity) {
    if (!enabled_) return;
    
    std::lock_guard<std::mutex> lock(mutex_);
    currentCacheSize_ = size;
    currentCacheCapacity_ = capacity;
}

void MMDNSMonitor::updateResourceStats(size_t memoryUsage, int activeThreads, int queuedTasks) {
    if (!enabled_) return;
    
    std::lock_guard<std::mutex> lock(mutex_);
    currentMemoryUsage_ = memoryUsage;
    currentActiveThreads_ = activeThreads;
    currentQueuedTasks_ = queuedTasks;
}

double MMDNSMonitor::calculatePercentile(const std::vector<double>& data, double percentile) const {
    if (data.empty()) return 0.0;
    
    std::vector<double> sorted = data;
    std::sort(sorted.begin(), sorted.end());
    
    size_t index = static_cast<size_t>((percentile / 100.0) * sorted.size());
    if (index >= sorted.size()) {
        index = sorted.size() - 1;
    }
    
    return sorted[index];
}

double MMDNSMonitor::calculateAverage(const std::vector<double>& data) const {
    if (data.empty()) return 0.0;
    
    double sum = 0.0;
    for (double value : data) {
        sum += value;
    }
    
    return sum / data.size();
}

void MMDNSMonitor::addSample(std::vector<double>& samples, double value) {
    samples.push_back(value);
    
    // 保持样本数量在限制内
    if (samples.size() > maxSamples_) {
        samples.erase(samples.begin());
    }
}

MMDNSMonitor::Metrics MMDNSMonitor::getMetrics() const {
    std::lock_guard<std::mutex> lock(mutex_);
    
    Metrics metrics;
    
    // 基本计数
    metrics.totalRequests = totalRequests_.load();
    metrics.successfulRequests = successfulRequests_.load();
    metrics.failedRequests = failedRequests_.load();
    metrics.cachedRequests = cachedRequests_.load();
    
    // 性能指标
    metrics.avgResolutionTime = calculateAverage(resolutionTimes_);
    metrics.avgSpeedCheckTime = calculateAverage(speedCheckTimes_);
    metrics.p50ResolutionTime = calculatePercentile(resolutionTimes_, 50.0);
    metrics.p95ResolutionTime = calculatePercentile(resolutionTimes_, 95.0);
    metrics.p99ResolutionTime = calculatePercentile(resolutionTimes_, 99.0);
    
    // 缓存指标
    if (metrics.totalRequests > 0) {
        metrics.cacheHitRate = (metrics.cachedRequests * 100.0) / metrics.totalRequests;
    }
    metrics.cacheSize = currentCacheSize_;
    metrics.cacheCapacity = currentCacheCapacity_;
    
    // 资源使用
    metrics.memoryUsage = currentMemoryUsage_;
    metrics.activeThreads = currentActiveThreads_;
    metrics.queuedTasks = currentQueuedTasks_;
    
    // 错误统计
    metrics.errorCounts = errorCounts_;
    
    // 最近的解析时间（最多100个）
    size_t recentCount = std::min(resolutionTimes_.size(), size_t(100));
    if (recentCount > 0) {
        metrics.recentResolutionTimes.assign(
            resolutionTimes_.end() - recentCount,
            resolutionTimes_.end());
    }
    
    return metrics;
}

void MMDNSMonitor::reset() {
    std::lock_guard<std::mutex> lock(mutex_);
    
    totalRequests_ = 0;
    successfulRequests_ = 0;
    failedRequests_ = 0;
    cachedRequests_ = 0;
    
    resolutionTimes_.clear();
    speedCheckTimes_.clear();
    errorCounts_.clear();
    
    currentCacheSize_ = 0;
    currentCacheCapacity_ = 0;
    currentMemoryUsage_ = 0;
    currentActiveThreads_ = 0;
    currentQueuedTasks_ = 0;
    
    Logger::log(LogLevel::INFO, "Monitor", "Statistics reset");
}

std::string MMDNSMonitor::generateReport() const {
    auto metrics = getMetrics();
    
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(2);
    
    oss << "=== MMDNS 性能监控报告 ===\n\n";
    
    // DNS解析统计
    oss << "DNS解析统计:\n";
    oss << "  总请求数: " << metrics.totalRequests << "\n";
    oss << "  成功数: " << metrics.successfulRequests << "\n";
    oss << "  失败数: " << metrics.failedRequests << "\n";
    oss << "  缓存命中: " << metrics.cachedRequests << "\n";
    if (metrics.totalRequests > 0) {
        oss << "  成功率: " << (metrics.successfulRequests * 100.0 / metrics.totalRequests) << "%\n";
    }
    oss << "\n";
    
    // 性能指标
    oss << "性能指标:\n";
    oss << "  平均解析时间: " << metrics.avgResolutionTime << "ms\n";
    oss << "  平均测速时间: " << metrics.avgSpeedCheckTime << "ms\n";
    oss << "  P50解析时间: " << metrics.p50ResolutionTime << "ms\n";
    oss << "  P95解析时间: " << metrics.p95ResolutionTime << "ms\n";
    oss << "  P99解析时间: " << metrics.p99ResolutionTime << "ms\n";
    oss << "\n";
    
    // 缓存统计
    oss << "缓存统计:\n";
    oss << "  缓存命中率: " << metrics.cacheHitRate << "%\n";
    oss << "  当前缓存大小: " << metrics.cacheSize << "/" << metrics.cacheCapacity << "\n";
    oss << "\n";
    
    // 资源使用
    oss << "资源使用:\n";
    oss << "  内存使用: " << (metrics.memoryUsage / 1024.0 / 1024.0) << "MB\n";
    oss << "  活跃线程: " << metrics.activeThreads << "\n";
    oss << "  队列任务: " << metrics.queuedTasks << "\n";
    oss << "\n";
    
    // 错误统计
    if (!metrics.errorCounts.empty()) {
        oss << "错误统计:\n";
        for (const auto& pair : metrics.errorCounts) {
            oss << "  " << pair.first << ": " << pair.second << "\n";
        }
        oss << "\n";
    }
    
    oss << "===========================\n";
    
    return oss.str();
}

} // namespace mmdns