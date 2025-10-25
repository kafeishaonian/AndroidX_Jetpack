#pragma once

#include "MMDNSCommon.h"
#include <mutex>

namespace mmdns {

// LRU缓存实现
template<typename K, typename V>
class LRUCache {
public:
    explicit LRUCache(size_t capacity) : capacity_(capacity) {}
    
    std::optional<V> get(const K& key) {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = cache_.find(key);
        if (it == cache_.end()) {
            return std::nullopt;
        }
        // 移到链表头部
        items_.splice(items_.begin(), items_, it->second);
        return it->second->second;
    }
    
    void put(const K& key, const V& value) {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = cache_.find(key);
        if (it != cache_.end()) {
            // 更新值并移到头部
            it->second->second = value;
            items_.splice(items_.begin(), items_, it->second);
            return;
        }
        
        // 添加新项
        if (cache_.size() >= capacity_) {
            // 删除最老的项
            auto last = items_.back();
            cache_.erase(last.first);
            items_.pop_back();
        }
        
        items_.push_front({key, value});
        cache_[key] = items_.begin();
    }
    
    void remove(const K& key) {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = cache_.find(key);
        if (it != cache_.end()) {
            items_.erase(it->second);
            cache_.erase(it);
        }
    }
    
    void clear() {
        std::lock_guard<std::mutex> lock(mutex_);
        cache_.clear();
        items_.clear();
    }
    
    size_t size() const {
        std::lock_guard<std::mutex> lock(mutex_);
        return cache_.size();
    }
    
private:
    using ListItem = std::pair<K, V>;
    using ListIterator = typename std::list<ListItem>::iterator;
    
    size_t capacity_;
    std::list<ListItem> items_;
    std::unordered_map<K, ListIterator> cache_;
    mutable std::mutex mutex_;
};

// 数据缓存管理器
class MMDNSDataCache {
public:
    explicit MMDNSDataCache(size_t cacheSize = Constants::DEFAULT_CACHE_SIZE);
    ~MMDNSDataCache();
    
    // 保存和加载
    void save(const std::string& key, const std::string& data);
    std::string load(const std::string& key);
    void remove(const std::string& key);
    void clear();
    
    // 持久化
    void saveToDisk(const std::string& filename);
    void loadFromDisk(const std::string& filename);
    
    // 缓存统计
    size_t getCacheSize() const { return memoryCache_->size(); }
    
    // 设置缓存目录
    void setCacheDir(const std::string& dir) { cacheDir_ = dir; }
    std::string getCacheDir() const { return cacheDir_; }
    
private:
    std::unique_ptr<LRUCache<std::string, std::string>> memoryCache_;
    std::string cacheDir_;
    mutable std::mutex fileMutex_;
    
    // 文件操作
    std::string getCacheFilePath(const std::string& key) const;
    bool writeToFile(const std::string& filepath, const std::string& data);
    std::string readFromFile(const std::string& filepath);
};

} // namespace mmdns