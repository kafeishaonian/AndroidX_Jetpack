//
// Created by 64860 on 2025/5/5.
//

#ifndef ANDROIDX_JETPACK_THREAD_POOL_H
#define ANDROIDX_JETPACK_THREAD_POOL_H

#include "task_queue.h"
#include <mutex>
#include <condition_variable>
#include <functional>
#include <chrono>
#include <unordered_map>
#include <stdexcept>
#include <sys/sysinfo.h>


class ThreadPool {
public:
    explicit ThreadPool(
            size_t worker_threads = std::thread::hardware_concurrency()
    ) : monitor_(&ThreadPool::monitor_loop, this) {

        queues_.clear();
        for (size_t i = 0; i < worker_threads; i++) {
            queues_.emplace_back();
        }
        queues_.reserve(worker_threads);
        for (size_t  i = 0; i < worker_threads; i++) {
            threads_.emplace_back(&ThreadPool::worker_loop, this, i);
        }
    }
    ~ThreadPool();

    template<typename F>
    void submit(F&& task) {
        //轮询选择队列：必满单个队列过载
        static size_t index = 0;
        queues_[index++ % queues_.size()].batch_push(std::forward<F>(task));
        pending_tasks_.fetch_add(1, std::memory_order_relaxed);
    }

    void shutdown_gracefully();
    void shutdown_immediately();

private:
    std::vector<TaskQueue> queues_;
    std::vector<std::thread> threads_;
    std::atomic<bool> graceful_shutdown_{false};
    std::atomic<bool> immediate_shutdown_{false};
    std::atomic<bool> monitor_running_{true};
    std::thread monitor_;


    //原子计数器
    struct alignas(64) AlignedCounter{
        std::atomic<size_t> value{0};

        size_t fetch_add(size_t arg, std::memory_order order = std::memory_order_seq_cst) {
            return value.fetch_add(arg, order);
        }

        size_t fetch_sub(size_t arg, std::memory_order order = std::memory_order_seq_cst) {
            return value.fetch_sub(arg, order);
        }

        size_t load(std::memory_order order = std::memory_order_seq_cst) const {
            return value.load(order);
        }

        void store(size_t arg, std::memory_order order = std::memory_order_seq_cst) {
            value.store(arg, order);
        }
    };
    AlignedCounter pending_tasks_;
    AlignedCounter completed_tasks_;
    AlignedCounter failed_tasks_;

    void worker_loop(size_t queue_idx);
    void process_batch(std::vector<std::function<void()>>& batch);
    void monitor_loop();
    void check_system_health();
    void adjust_thread_pool();
};


#endif //ANDROIDX_JETPACK_THREAD_POOL_H
