#include "../include/MMDNSConnectionPool.h"
#include <algorithm>

namespace mmdns {

MMDNSConnectionPool::MMDNSConnectionPool(
    int maxConnectionsPerHost,
    int connectionTimeout,
    int idleTimeout)
    : maxConnectionsPerHost_(maxConnectionsPerHost)
    , connectionTimeout_(connectionTimeout)
    , idleTimeout_(idleTimeout) {
    
    Logger::log(LogLevel::INFO, "ConnectionPool",
        "Initialized with max " + std::to_string(maxConnectionsPerHost) + " connections per host");
}

MMDNSConnectionPool::~MMDNSConnectionPool() {
    clear();
}

CURL* MMDNSConnectionPool::createConnection() {
    CURL* curl = curl_easy_init();
    if (!curl) {
        Logger::log(LogLevel::ERROR, "ConnectionPool", "Failed to create CURL handle");
        return nullptr;
    }
    
    // 设置基本选项
    curl_easy_setopt(curl, CURLOPT_NOSIGNAL, 1L);
    curl_easy_setopt(curl, CURLOPT_TCP_KEEPALIVE, 1L);
    
    return curl;
}

std::string MMDNSConnectionPool::extractHost(const std::string& url) {
    // 简单提取主机名：https://example.com/path -> example.com
    size_t start = url.find("://");
    if (start == std::string::npos) {
        return url;
    }
    start += 3;
    
    size_t end = url.find('/', start);
    if (end == std::string::npos) {
        end = url.length();
    }
    
    return url.substr(start, end - start);
}

bool MMDNSConnectionPool::isConnectionExpired(const Connection& conn) const {
    auto now = std::chrono::steady_clock::now();
    auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(
        now - conn.lastUsed).count();
    return elapsed > idleTimeout_;
}

CURL* MMDNSConnectionPool::acquire(const std::string& host) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    std::string hostKey = extractHost(host);
    auto& pool = pools_[hostKey];
    
    // 查找可用的空闲连接
    for (auto& conn : pool) {
        if (!conn.inUse && !isConnectionExpired(conn)) {
            conn.inUse = true;
            conn.lastUsed = std::chrono::steady_clock::now();
            Logger::log(LogLevel::DEBUG, "ConnectionPool",
                "Reusing connection for " + hostKey);
            return conn.handle;
        }
    }
    
    // 没有可用连接，检查是否可以创建新连接
    if (pool.size() < static_cast<size_t>(maxConnectionsPerHost_)) {
        CURL* newHandle = createConnection();
        if (newHandle) {
            Connection conn;
            conn.handle = newHandle;
            conn.inUse = true;
            conn.lastUsed = std::chrono::steady_clock::now();
            conn.host = hostKey;
            pool.push_back(conn);
            
            Logger::log(LogLevel::DEBUG, "ConnectionPool",
                "Created new connection for " + hostKey +
                " (total: " + std::to_string(pool.size()) + ")");
            
            return newHandle;
        }
    }
    
    Logger::log(LogLevel::WARN, "ConnectionPool",
        "No available connections for " + hostKey);
    return nullptr;
}

void MMDNSConnectionPool::release(CURL* handle) {
    if (!handle) {
        return;
    }
    
    std::lock_guard<std::mutex> lock(mutex_);
    
    // 查找并标记为空闲
    for (auto& pair : pools_) {
        for (auto& conn : pair.second) {
            if (conn.handle == handle) {
                conn.inUse = false;
                conn.lastUsed = std::chrono::steady_clock::now();
                Logger::log(LogLevel::DEBUG, "ConnectionPool",
                    "Released connection for " + pair.first);
                return;
            }
        }
    }
}

void MMDNSConnectionPool::cleanup() {
    std::lock_guard<std::mutex> lock(mutex_);
    
    int cleaned = 0;
    
    for (auto& pair : pools_) {
        auto& pool = pair.second;
        
        // 删除过期的空闲连接
        pool.erase(
            std::remove_if(pool.begin(), pool.end(),
                [this, &cleaned](const Connection& conn) {
                    if (!conn.inUse && isConnectionExpired(conn)) {
                        if (conn.handle) {
                            curl_easy_cleanup(conn.handle);
                        }
                        cleaned++;
                        return true;
                    }
                    return false;
                }),
            pool.end());
    }
    
    // 删除空的主机池
    for (auto it = pools_.begin(); it != pools_.end();) {
        if (it->second.empty()) {
            it = pools_.erase(it);
        } else {
            ++it;
        }
    }
    
    if (cleaned > 0) {
        Logger::log(LogLevel::DEBUG, "ConnectionPool",
            "Cleaned up " + std::to_string(cleaned) + " expired connections");
    }
}

void MMDNSConnectionPool::clear() {
    std::lock_guard<std::mutex> lock(mutex_);
    
    int count = 0;
    for (auto& pair : pools_) {
        for (auto& conn : pair.second) {
            if (conn.handle) {
                curl_easy_cleanup(conn.handle);
                count++;
            }
        }
    }
    
    pools_.clear();
    
    Logger::log(LogLevel::INFO, "ConnectionPool",
        "Cleared all connections (" + std::to_string(count) + ")");
}

MMDNSConnectionPool::Stats MMDNSConnectionPool::getStats() const {
    std::lock_guard<std::mutex> lock(mutex_);
    
    Stats stats;
    stats.totalConnections = 0;
    stats.activeConnections = 0;
    stats.idleConnections = 0;
    stats.hostsCount = pools_.size();
    
    for (const auto& pair : pools_) {
        for (const auto& conn : pair.second) {
            stats.totalConnections++;
            if (conn.inUse) {
                stats.activeConnections++;
            } else {
                stats.idleConnections++;
            }
        }
    }
    
    return stats;
}

} // namespace mmdns