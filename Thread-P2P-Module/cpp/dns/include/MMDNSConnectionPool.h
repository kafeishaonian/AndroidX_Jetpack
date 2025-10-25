#pragma once

#include "MMDNSCommon.h"
#include <string>
#include <map>
#include <vector>
#include <memory>
#include <mutex>
#include <chrono>
#include <curl/curl.h>

namespace mmdns {

/**
 * HTTP连接池
 * 管理和复用CURL连接，提高HTTP请求性能
 */
class MMDNSConnectionPool {
public:
    /**
     * 连接信息
     */
    struct Connection {
        CURL* handle;                                          // CURL句柄
        bool inUse;                                           // 是否正在使用
        std::chrono::steady_clock::time_point lastUsed;      // 最后使用时间
        std::string host;                                     // 主机名
        
        Connection() : handle(nullptr), inUse(false) {}
    };
    
    /**
     * 构造函数
     * @param maxConnectionsPerHost 每个主机的最大连接数
     * @param connectionTimeout 连接超时时间（毫秒）
     * @param idleTimeout 空闲超时时间（毫秒）
     */
    explicit MMDNSConnectionPool(
        int maxConnectionsPerHost = 6,
        int connectionTimeout = 30000,
        int idleTimeout = 60000);
    
    ~MMDNSConnectionPool();
    
    /**
     * 获取连接
     * @param host 主机名
     * @return CURL句柄，如果获取失败返回nullptr
     */
    CURL* acquire(const std::string& host);
    
    /**
     * 释放连接
     * @param handle CURL句柄
     */
    void release(CURL* handle);
    
    /**
     * 清理空闲连接
     */
    void cleanup();
    
    /**
     * 清空所有连接
     */
    void clear();
    
    /**
     * 获取统计信息
     */
    struct Stats {
        size_t totalConnections;     // 总连接数
        size_t activeConnections;    // 活跃连接数
        size_t idleConnections;      // 空闲连接数
        size_t hostsCount;           // 主机数量
    };
    
    Stats getStats() const;
    
private:
    std::map<std::string, std::vector<Connection>> pools_;  // 每个主机的连接池
    mutable std::mutex mutex_;                              // 互斥锁
    int maxConnectionsPerHost_;                             // 每个主机的最大连接数
    int connectionTimeout_;                                 // 连接超时（毫秒）
    int idleTimeout_;                                       // 空闲超时（毫秒）
    
    /**
     * 创建新连接
     */
    CURL* createConnection();
    
    /**
     * 从主机名提取域名
     */
    std::string extractHost(const std::string& url);
    
    /**
     * 检查连接是否过期
     */
    bool isConnectionExpired(const Connection& conn) const;
};

} // namespace mmdns