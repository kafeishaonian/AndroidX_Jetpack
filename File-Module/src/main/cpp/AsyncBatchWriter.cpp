//
// Created by 64860 on 2025/6/17.
//

#include "AsyncBatchWriter.h"


AsyncBatchWriter::AsyncBatchWriter()
        : _max_batch_size(50), _min_batch_size(5),
          _batch_interval(100), _max_wait_time(50),
          _stop_flag(false), _adaptive_mode(true) {
    _worker_thread = thread([this] { worker_loop(); });
}


AsyncBatchWriter::~AsyncBatchWriter() {
    {
        lock_guard lock(_mutex);
        _stop_flag = true;
    }

    _cv.notify_all();
    if (_worker_thread.joinable()) {
        _worker_thread.join();
    }
}


void AsyncBatchWriter::enqueue(AsyncBatchWriter::Task task) {
    lock_guard lock(_mutex);
    _queue.push(std::move(task));
    //唤醒工作线程
    _cv.notify_one();
}

void AsyncBatchWriter::set_batch_params(size_t min_batch, size_t max_batch,
                                        AsyncBatchWriter::Duration interval,
                                        AsyncBatchWriter::Duration max_wait) {
    lock_guard lock(_mutex);
    _min_batch_size = min_batch;
    _max_batch_size = max_batch;
    _batch_interval = interval.count();
    _max_wait_time = max_wait.count();
}

void AsyncBatchWriter::enable_adaptive_mode(bool enable) {
    _adaptive_mode = enable;
}

void AsyncBatchWriter::worker_loop() {
    vector<Task> batch;
    TimePoint last_flush_time = Clock::now();

    while (!_stop_flag.load(memory_order_relaxed)) {
        batch.clear();

        collect_batch(batch);

        if (!batch.empty()) {
            execute_batch(batch);

            if (_adaptive_mode) {
                adjust_parameters(batch.size());
            }
        }
    }
}


void AsyncBatchWriter::collect_batch(vector<AsyncBatchWriter::Task> &batch) {
    unique_lock lock(_mutex);

    if (_queue.empty()) {
        _cv.wait_for(lock, chrono::milliseconds(_batch_interval));
    }

    if (batch.size() < _min_batch_size) {
        auto wait_time = min<int>(_batch_interval, _max_wait_time - static_cast<int>(batch.size()));

        if (wait_time > 0) {
            _cv.wait_for(lock, chrono::milliseconds(wait_time));
        }

        while (batch.size() < _max_batch_size && !_queue.empty()) {
            batch.push_back(std::move(_queue.front()));
            _queue.pop();
        }
    }

}


void AsyncBatchWriter::execute_batch(const std::vector<Task> &batch) {
    for (const auto& task : batch) {
        try {
            task();
        } catch (...) {

        }
    }
}

void AsyncBatchWriter::adjust_parameters(size_t batch_size) {
    if (batch_size >= _max_batch_size) {
        _batch_interval = std::min(250, _batch_interval + 5);
    } else if (batch_size < _min_batch_size) {
        _batch_interval = std::max(10, _batch_interval - 2);
    }
}