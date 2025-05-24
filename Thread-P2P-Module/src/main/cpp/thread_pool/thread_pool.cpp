//
// Created by 64860 on 2025/5/5.
//

#include "thread_pool.h"

ThreadPool::~ThreadPool() {
    shutdown_gracefully();
    //原子操作停止监控线程
    monitor_running_.store(false, std::memory_order_release);
    //等待监控线程结束
    if (monitor_.joinable()) {
        monitor_.join();
    }
}

void ThreadPool::shutdown_gracefully() {
    graceful_shutdown_.store(true, std::memory_order_release);
    for (auto &q: queues_) {
        q.shutdown();
    }
    for (auto &t: threads_) {
        if (t.joinable()) {
            t.join();
        }
    }
}


void ThreadPool::shutdown_immediately() {
    immediate_shutdown_.store(true, std::memory_order_release);
    for (auto &t: threads_) {
        if (t.joinable()) {
            t.detach();
        }
    }
}


void ThreadPool::worker_loop(size_t queue_idx) {
    std::vector<std::function<void()>> local_batch;

    while (true) {
        //双重关闭检查
        if (immediate_shutdown_.load(std::memory_order_acquire)) {
            break;
        }

        if (graceful_shutdown_.load(std::memory_order_acquire) &&
            pending_tasks_.load(std::memory_order_relaxed) == 0) {
            break;
        }

        //优先处理本地队列
        if (queues_[queue_idx].batch_pop(local_batch)) {
            process_batch(local_batch);
        } else {
            for (size_t i = 0; i < queues_.size(); i++) {
                if (i == queue_idx) {
                    continue;
                }
                if (queues_[i].batch_pop(local_batch)) {
                    process_batch(local_batch);
                    break;
                }
            }
        }
    }
}


void ThreadPool::process_batch(std::vector<std::function<void()>> &batch) {
    for (auto& task: batch) {
        try {
            task();
            completed_tasks_.fetch_add(1, std::memory_order_relaxed);
        } catch (...) {
            failed_tasks_.fetch_add(1, std::memory_order_relaxed);
        }
        pending_tasks_.fetch_sub(1, std::memory_order_relaxed);
    }
    batch.clear();
}

void ThreadPool::monitor_loop() {
    while (monitor_running_.load(std::memory_order_acquire)){
        check_system_health();
        adjust_thread_pool();
        std::this_thread::sleep_for(1s);
    }
}

void ThreadPool::check_system_health() {
    static auto last_time = std::chrono::steady_clock::now();
    static size_t last_total = pending_tasks_.value + completed_tasks_.value;
    auto now = std::chrono::steady_clock::now();
    size_t current_total = pending_tasks_.value + completed_tasks_.value;
    double task_rate = (current_total - last_total)
            / std::chrono::duration<double>(now - last_time).count();
    last_time = now;
    last_total = current_total;

    //动态调整工作线程数量
    if (task_rate > 1000) {
        //考虑增加工作线程
    }
}

void ThreadPool::adjust_thread_pool() {
    //基于系统负责的动态线程调整
    struct sysinfo info;
    sysinfo(&info);

    const double mem_usage = 1.0 - 1.0 * info.freeram / info.totalram;

    const size_t ideal_threads = std::min<size_t>(
            std::thread::hardware_concurrency(),
            static_cast<size_t>(std::thread::hardware_concurrency() * (1.0 - mem_usage))
    );
    if (ideal_threads != threads_.size()) {
        //动态调整线程数量
    }
}