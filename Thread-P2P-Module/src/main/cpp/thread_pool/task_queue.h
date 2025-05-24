//
// Created by 64860 on 2025/5/5.
//

#ifndef ANDROIDX_JETPACK_TASKQUEUE_H
#define ANDROIDX_JETPACK_TASKQUEUE_H

#include <atomic>
#include <vector>
#include <queue>
#include <thread>

using namespace std::chrono_literals;

class TaskQueue {

public:

    TaskQueue(): shutdown_flag_(false) {}

    // 删除复制操作以明确语义
    TaskQueue(const TaskQueue&) = delete;
    TaskQueue& operator=(const TaskQueue&) = delete;

    // 显式定义移动构造函数（必须 noexcept）
    TaskQueue(TaskQueue&& other) noexcept
            : tasks_(std::move(other.tasks_)),
              shutdown_flag_(other.shutdown_flag_.load()) {}

    // 显式定义移动赋值操作符
    TaskQueue& operator=(TaskQueue&& other) noexcept {
        if (this != &other) {
            tasks_ = std::move(other.tasks_);
            shutdown_flag_.store(other.shutdown_flag_.load());
        }
        return *this;
    }


    template<typename... Args>
    void batch_push(Args&&... tasks) {
        {
            std::lock_guard<std::mutex> lock(mtx_);
            (tasks_.emplace_back(std::forward<Args>(tasks)), ...);
        }
        cv_.notify_all();
    }

    bool batch_pop(std::vector<std::function<void()>>& output,
                   size_t  max_batch = 32);

    void shutdown();

private:
    std::deque<std::function<void()>> tasks_;
    std::mutex mtx_;
    std::condition_variable cv_;
    std::atomic<bool> shutdown_flag_;
};

#endif //ANDROIDX_JETPACK_TASKQUEUE_H
