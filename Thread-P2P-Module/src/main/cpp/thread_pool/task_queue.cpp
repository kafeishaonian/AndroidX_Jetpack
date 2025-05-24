//
// Created by 64860 on 2025/5/5.
//

#include "task_queue.h"

//批量获取任务
bool TaskQueue::batch_pop(std::vector<std::function<void()>> &output, size_t max_batch) {
    std::unique_lock<std::mutex> lock(mtx_);
    cv_.wait_for(lock, 100ms, [&](){
        return !tasks_.empty() || shutdown_flag_;
    });

    if (tasks_.empty()) {
        return false;
    }

    size_t count = std::min(max_batch, tasks_.size());
    output.reserve(output.size() + count);
    for (size_t i = 0; i < count; ++i) {
        output.emplace_back(std::move(tasks_.front()));
        tasks_.pop_front();
    }
    return true;
}


void TaskQueue::shutdown() {
    shutdown_flag_ = false;
    cv_.notify_all();
}

