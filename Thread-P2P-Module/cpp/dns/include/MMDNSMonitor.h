#pragma once

#include "MMDNSCommon.h"
#include <string>
#include <vector>
#include <map>
#include <mutex>
#include <atomic>
#include <chrono>

namespace mmdns {

/**
 * DNS性能监控系统
 * 收集和统计DNS解析性能指标
 */
class MMDNSMonitor {
public:
    /**
     * 性能指标
     */
    struct Metrics {
        // DNS解析指标
        uint64_t totalRequests;
        uint64_t successfulRequests;
        uint64_t failedRequests;
        uint64_t cachedRequests;
        
        // 性能指标
        double avgResolutionTime;      // 平均解析时间（毫秒）
        double avgSpeedCheckTime;      // 平均测速时间（毫秒）
        double p50ResolutionTime;      // P50解析时间
        double p95ResolutionTime;      // P95解析时间
        double p99ResolutionTime;      // P99解析时间
        
        // 缓存指标
        double cacheHitRate;           // 缓存命中率
        size_t cacheSize;              // 当前缓存大小
        size_t cacheCapacity;          // 缓存容量
        
        // 资源使用
        size_t memoryUsage;            // 内存使用（字节）
        int activeThreads;             // 活跃线程数
        int queuedTasks;               // 队列中的任务数
        
        // 错误统计
        std::map<std::string, uint64_t> errorCounts;
        
        // 时间窗口统计
        std::vector<double> recentResolutionTimes;  // 最近的解析时间
        
        Metrics() : totalRequests(0), successfulRequests(0), failedRequests(0),
                   cachedRequests(0), avgResolutionTime(0), avgSpeedCheckTime(0),
                   p50ResolutionTime(0), p95ResolutionTime(0), p99ResolutionTime(0),
                   cacheHitRate(0), cacheSize(0), cacheCapacity(0),
                   memoryUsage(0), activeThreads(0), queuedTasks(0) {}
    };
    
    MMDNSMonitor();
    ~MMDNSMonitor() = default;
    
    /**
     * 记录DNS解析事件
     * @param hostname 主机名
     * @param duration 解析耗时（毫秒）
     * @param success 是否成功
     * @param fromCache 是否来自缓存
     */
    void recordResolution(const std::string& hostname, 
                         double duration, 
                         bool success,
                         bool fromCache = false);
    
    /**
     * 记录测速事件
     * @param ip IP地址
     * @param duration 测速耗时（毫秒）
     */
    void recordSpeedCheck(const std::string& ip, double duration);
    
    /**
     * 记录错误
     * @param errorType 错误类型
     */
    void recordError(const std::string& errorType);
    
    /**
     * 更新缓存统计
     * @param size 当前缓存大小
     * @param capacity 缓存容量
     */
    void updateCacheStats(size_t size, size_t capacity);
    
    /**
     * 更新资源使用
     * @param memoryUsage 内存使用（字节）
     * @param activeThreads 活跃线程数
     * @param queuedTasks 队列中的任务数
     */
    void updateResourceStats(size_t memoryUsage, int activeThreads, int queuedTasks);
    
    /**
     * 获取当前指标
     */
    Metrics getMetrics() const;
    
    /**
     * 重置所有统计
     */
    void reset();
    
    /**
     * 生成性能报告
     */
    std::string generateReport() const;
    
    /**
     * 启用/禁用监控
     */
    void setEnabled(bool enabled) { enabled_ = enabled; }
    bool isEnabled() const { return enabled_; }
    
private:
    mutable std::mutex mutex_;
    std::atomic<bool> enabled_;
    
    // 计数器
    std::atomic<uint64_t> totalRequests_;
    std::atomic<uint64_t> successfulRequests_;
    std::atomic<uint64_t> failedRequests_;
    std::atomic<uint64_t> cachedRequests_;
    
    // 时间统计
    std::vector<double> resolutionTimes_;      // 解析时间列表
    std::vector<double> speedCheckTimes_;      // 测速时间列表
    size_t maxSamples_;                        // 最大样本数
    
    // 缓存统计
    size_t currentCacheSize_;
    size_t currentCacheCapacity_;
    
    // 资源统计
    size_t currentMemoryUsage_;
    int currentActiveThreads_;
    int currentQueuedTasks_;
    
    // 错误统计
    std::map<std::string, uint64_t> errorCounts_;
    
    /**
     * 计算百分位数
     */
    double calculatePercentile(const std::vector<double>& data, double percentile) const;
    
    /**
     * 计算平均值
     */
    double calculateAverage(const std::vector<double>& data) const;
    
    /**
     * 添加样本（保持最大样本数限制）
     */
    void addSample(std::vector<double>& samples, double value);
};

} // namespace mmdns