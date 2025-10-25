#include "../include/MMDNSDataCache.h"
#include "../include/MMDNSCommon.h"
#include <fstream>
#include <sstream>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

namespace mmdns {

// ==================== MMDNSDataCache ====================

MMDNSDataCache::MMDNSDataCache(size_t cacheSize)
    : cacheDir_("/data/local/tmp/mmdns_cache") {
    
    memoryCache_ = std::make_unique<LRUCache<std::string, std::string>>(cacheSize);
    
    // 创建缓存目录
    mkdir(cacheDir_.c_str(), 0755);
    
    Logger::log(LogLevel::INFO, "MMDNSDataCache",
        "数据缓存已创建，大小: " + std::to_string(cacheSize));
}

MMDNSDataCache::~MMDNSDataCache() {
    Logger::log(LogLevel::INFO, "MMDNSDataCache", "数据缓存已销毁");
}

void MMDNSDataCache::save(const std::string& key, const std::string& data) {
    // 保存到内存缓存
    memoryCache_->put(key, data);
    
    // 异步保存到文件
    std::string filepath = getCacheFilePath(key);
    if (!writeToFile(filepath, data)) {
        Logger::log(LogLevel::ERROR, "MMDNSDataCache",
            "写入文件失败: " + filepath);
    }
}

std::string MMDNSDataCache::load(const std::string& key) {
    // 先从内存缓存读取
    auto cached = memoryCache_->get(key);
    if (cached.has_value()) {
        Logger::log(LogLevel::DEBUG, "MMDNSDataCache",
            "内存缓存命中: " + key);
        return cached.value();
    }
    
    // 从文件读取
    std::string filepath = getCacheFilePath(key);
    std::string data = readFromFile(filepath);
    
    if (!data.empty()) {
        // 放入内存缓存
        memoryCache_->put(key, data);
        Logger::log(LogLevel::DEBUG, "MMDNSDataCache",
            "文件缓存命中: " + key);
    }
    
    return data;
}

void MMDNSDataCache::remove(const std::string& key) {
    // 从内存缓存删除
    memoryCache_->remove(key);
    
    // 删除文件
    std::string filepath = getCacheFilePath(key);
    if (unlink(filepath.c_str()) == 0) {
        Logger::log(LogLevel::DEBUG, "MMDNSDataCache",
            "删除缓存文件: " + filepath);
    }
}

void MMDNSDataCache::clear() {
    // 清空内存缓存
    memoryCache_->clear();
    
    // TODO: 清空缓存目录中的所有文件
    Logger::log(LogLevel::INFO, "MMDNSDataCache", "清空缓存");
}

void MMDNSDataCache::saveToDisk(const std::string& filename) {
    std::lock_guard<std::mutex> lock(fileMutex_);
    
    // TODO: 将内存缓存序列化保存到单个文件
    Logger::log(LogLevel::INFO, "MMDNSDataCache",
        "保存缓存到文件: " + filename);
}

void MMDNSDataCache::loadFromDisk(const std::string& filename) {
    std::lock_guard<std::mutex> lock(fileMutex_);
    
    // TODO: 从文件加载并反序列化到内存缓存
    Logger::log(LogLevel::INFO, "MMDNSDataCache",
        "从文件加载缓存: " + filename);
}

std::string MMDNSDataCache::getCacheFilePath(const std::string& key) const {
    // 使用key的hash作为文件名，避免特殊字符
    std::hash<std::string> hasher;
    size_t hash = hasher(key);
    
    std::ostringstream oss;
    oss << cacheDir_ << "/" << hash << ".cache";
    return oss.str();
}

bool MMDNSDataCache::writeToFile(const std::string& filepath, const std::string& data) {
    std::lock_guard<std::mutex> lock(fileMutex_);
    
    try {
        std::ofstream file(filepath, std::ios::binary);
        if (!file.is_open()) {
            return false;
        }
        
        file.write(data.c_str(), data.size());
        file.close();
        
        return true;
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "MMDNSDataCache",
            "写入文件异常: " + std::string(e.what()));
        return false;
    }
}

std::string MMDNSDataCache::readFromFile(const std::string& filepath) {
    std::lock_guard<std::mutex> lock(fileMutex_);
    
    try {
        std::ifstream file(filepath, std::ios::binary);
        if (!file.is_open()) {
            return "";
        }
        
        std::ostringstream oss;
        oss << file.rdbuf();
        file.close();
        
        return oss.str();
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "MMDNSDataCache",
            "读取文件异常: " + std::string(e.what()));
        return "";
    }
}

} // namespace mmdns