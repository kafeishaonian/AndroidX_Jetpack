#pragma once

#include "MMDNSCommon.h"
#include <vector>
#include <memory>
#include <mutex>
#include <functional>

namespace mmdns {

/**
 * 通用对象池模板
 * 用于复用对象，减少频繁的内存分配和释放
 */
template<typename T>
class ObjectPool {
public:
    /**
     * 构造函数
     * @param initialSize 初始对象数量
     * @param maxSize 最大对象数量
     * @param factory 对象工厂函数（可选）
     * @param resetter 对象重置函数（可选）
     */
    explicit ObjectPool(
        size_t initialSize = 10,
        size_t maxSize = 100,
        std::function<std::shared_ptr<T>()> factory = nullptr,
        std::function<void(std::shared_ptr<T>&)> resetter = nullptr)
        : maxSize_(maxSize)
        , factory_(factory)
        , resetter_(resetter)
        , totalCreated_(0) {
        
        // 预创建初始对象
        for (size_t i = 0; i < initialSize && i < maxSize; ++i) {
            pool_.push_back(createObject());
            totalCreated_++;
        }
        
        Logger::log(LogLevel::DEBUG, "ObjectPool",
            "Initialized with " + std::to_string(initialSize) + " objects");
    }
    
    ~ObjectPool() {
        std::lock_guard<std::mutex> lock(mutex_);
        pool_.clear();
        Logger::log(LogLevel::DEBUG, "ObjectPool", "Destroyed");
    }
    
    /**
     * 从池中获取对象
     * @return 对象智能指针，如果池已空且未达到上限则创建新对象
     */
    std::shared_ptr<T> acquire() {
        std::lock_guard<std::mutex> lock(mutex_);
        
        if (!pool_.empty()) {
            auto obj = pool_.back();
            pool_.pop_back();
            return obj;
        }
        
        // 池已空，检查是否可以创建新对象
        if (totalCreated_ < maxSize_) {
            totalCreated_++;
            return createObject();
        }
        
        // 达到最大限制，返回nullptr
        Logger::log(LogLevel::WARN, "ObjectPool",
            "Pool exhausted, max size reached: " + std::to_string(maxSize_));
        return nullptr;
    }
    
    /**
     * 将对象归还到池中
     * @param obj 要归还的对象
     */
    void release(std::shared_ptr<T> obj) {
        if (!obj) {
            return;
        }
        
        std::lock_guard<std::mutex> lock(mutex_);
        
        // 重置对象状态
        if (resetter_) {
            resetter_(obj);
        }
        
        // 检查池大小
        if (pool_.size() < maxSize_) {
            pool_.push_back(obj);
        } else {
            // 池已满，丢弃对象（会自动释放）
            totalCreated_--;
        }
    }
    
    /**
     * 获取池中可用对象数量
     */
    size_t availableCount() const {
        std::lock_guard<std::mutex> lock(mutex_);
        return pool_.size();
    }
    
    /**
     * 获取总共创建的对象数量
     */
    size_t totalCount() const {
        std::lock_guard<std::mutex> lock(mutex_);
        return totalCreated_;
    }
    
    /**
     * 清空对象池
     */
    void clear() {
        std::lock_guard<std::mutex> lock(mutex_);
        pool_.clear();
        totalCreated_ = 0;
        Logger::log(LogLevel::DEBUG, "ObjectPool", "Cleared");
    }
    
private:
    std::vector<std::shared_ptr<T>> pool_;  // 对象池
    mutable std::mutex mutex_;               // 互斥锁
    size_t maxSize_;                        // 最大对象数量
    size_t totalCreated_;                   // 总共创建的对象数
    std::function<std::shared_ptr<T>()> factory_;      // 对象工厂
    std::function<void(std::shared_ptr<T>&)> resetter_; // 对象重置器
    
    /**
     * 创建新对象
     */
    std::shared_ptr<T> createObject() {
        if (factory_) {
            return factory_();
        }
        return std::make_shared<T>();
    }
};

// 特化的对象池类型
class MMDNSHostModel;
class MMDNSIPModel;
class MMDNSSocket;

using HostModelPool = ObjectPool<MMDNSHostModel>;
using IPModelPool = ObjectPool<MMDNSIPModel>;
using SocketPool = ObjectPool<MMDNSSocket>;

} // namespace mmdns