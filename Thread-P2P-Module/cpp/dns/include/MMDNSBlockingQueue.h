#pragma once

#include "MMDNSCommon.h"
#include <deque>
#include <mutex>
#include <condition_variable>
#include <optional>

namespace mmdns {

template<typename T>
class MMDNSBlockingQueue {
public:
    explicit MMDNSBlockingQueue(size_t maxSize = Constants::DEFAULT_QUEUE_SIZE)
        : maxSize_(maxSize) {}
    
    ~MMDNSBlockingQueue() {
        clear();
    }
    
    // 阻塞放入
    void put(const T& item) {
        std::unique_lock<std::mutex> lock(mutex_);
        notFull_.wait(lock, [this] { return queue_.size() < maxSize_; });
        queue_.push_back(item);
        notEmpty_.notify_one();
    }
    
    // 阻塞取出
    T take() {
        std::unique_lock<std::mutex> lock(mutex_);
        notEmpty_.wait(lock, [this] { return !queue_.empty(); });
        T item = queue_.front();
        queue_.pop_front();
        notFull_.notify_one();
        return item;
    }
    
    // 带超时的取出
    std::optional<T> take(std::chrono::milliseconds timeout) {
        std::unique_lock<std::mutex> lock(mutex_);
        if (!notEmpty_.wait_for(lock, timeout, [this] { return !queue_.empty(); })) {
            return std::nullopt;
        }
        T item = queue_.front();
        queue_.pop_front();
        notFull_.notify_one();
        return item;
    }
    
    // 非阻塞放入
    bool tryPut(const T& item) {
        std::lock_guard<std::mutex> lock(mutex_);
        if (queue_.size() >= maxSize_) {
            return false;
        }
        queue_.push_back(item);
        notEmpty_.notify_one();
        return true;
    }
    
    // 查询操作
    size_t size() const {
        std::lock_guard<std::mutex> lock(mutex_);
        return queue_.size();
    }
    
    bool isEmpty() const {
        std::lock_guard<std::mutex> lock(mutex_);
        return queue_.empty();
    }
    
    // 清空队列
    void clear() {
        std::lock_guard<std::mutex> lock(mutex_);
        queue_.clear();
        notFull_.notify_all();
    }
    
private:
    std::deque<T> queue_;
    mutable std::mutex mutex_;
    std::condition_variable notEmpty_;
    std::condition_variable notFull_;
    size_t maxSize_;
};

} // namespace mmdns