//
// Created by 64860 on 2025/6/16.
//

#ifndef ANDROIDX_JETPACK_ASYNCBATCHWRITER_H
#define ANDROIDX_JETPACK_ASYNCBATCHWRITER_H

#include <functional>
#include <vector>
#include <queue>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <atomic>
#include <chrono>
#include <algorithm>

using namespace std;

/**
 *  异步批处理
 */
class AsyncBatchWriter {

public:
    using Task = function<void()>;
    using Clock = chrono::steady_clock;
    using TimePoint = Clock::time_point;
    using Duration = chrono::milliseconds;

    AsyncBatchWriter();

    ~AsyncBatchWriter();

    void enqueue(Task task);

    void set_batch_params(size_t min_batch, size_t max_batch, Duration interval, Duration max_wait);

    void enable_adaptive_mode(bool enable);


private:
    void worker_loop();

    void collect_batch(vector<Task>& batch);

    void execute_batch(const std::vector<Task>& batch);

    void adjust_parameters(size_t batch_size);


    thread _worker_thread;
    queue<Task> _queue;
    mutex _mutex;
    condition_variable _cv;
    atomic<bool> _stop_flag;

    size_t _max_batch_size;
    size_t _min_batch_size;
    int _batch_interval;
    int _max_wait_time;
    bool _adaptive_mode;
};


#endif //ANDROIDX_JETPACK_ASYNCBATCHWRITER_H
