//
// Created by 魏红明 on 2025/7/1.
//

#include "FileOperationLogger.h"
#include "utils/log_utils.h"

#define LOG_TAG "FileOperationLogger"


FileOperationLogger &FileOperationLogger::instance() {
    static FileOperationLogger singleton;
    return singleton;
}

bool FileOperationLogger::start(const string &log_path,
                                const string &log_file) {
    if (_active) {
        return false;
    }

    if (log_path.empty() || log_file.empty()) {
        return false;
    }
    _log_path = log_path;
    _log_file = log_file;
    std::error_code ec;
    if (!filesystem::exists(_log_path, ec)) {
        filesystem::create_directories(_log_path, ec);
    }

    _active = true;
    _writer_thread = thread(&FileOperationLogger::run_logger, this);
    return true;
}


void FileOperationLogger::stop() {
    if (!_active) {
        return;
    }

    _active = false;
    _queue_cv.notify_all();
    if (_writer_thread.joinable()) {
        _writer_thread.join();
    }
}

void FileOperationLogger::log_failure(FileOperation op, const std::string &file_path,
                                      const std::string &error_msg, const std::string &module,
                                      int error_code, const std::string &dest_path) {

    FileOperationLog log_entry{
            .timestamp = time(nullptr),
            .operation = op,
            .file_path = file_path,
            .dest_path = dest_path,
            .error_message = error_msg,
            .calling_module = module,
            .file_id = 0,
            .error_code = error_code
    };

    {
        lock_guard<mutex> lock(_queue_mutex);
        _log_queue.push(std::move(log_entry));
        ++_pending_count;
    }
    _queue_cv.notify_one();
}


void FileOperationLogger::run_logger() {
    ofstream current_log;

    while (_active) {
        FileOperationLog entry;
        {
            unique_lock lock(_queue_mutex);
            _queue_cv.wait(lock, [this] {
                return !_log_queue.empty() || !_active;
            });

            if (!_active && _log_queue.empty()) {
                break;
            }

            if (_log_queue.empty()) {
                continue;
            }

            entry = _log_queue.front();
            _log_queue.pop();
            --_pending_count;
        }

        if (!current_log.is_open()) {
            if (current_log.is_open()) {
                current_log.close();
            }
            current_log = open_log_file();

            if (current_log) {
                current_log << "[" << get_date_time(entry.timestamp) << "] \n";
            }
        }
        if (current_log && current_log.is_open()) {
            current_log << entry.to_json() << ",\n";
            current_log.flush();
        }

    }

    if (current_log.is_open()) {
        current_log.close();
    }
}


FileOperationLogger::FileOperationLogger()
        : _active(false),
          _pending_count(0) {
}

string FileOperationLogger::get_date_time(time_t time) {
    tm tm = *localtime(&time);
    char buffer[20];
    strftime(buffer, sizeof(buffer), "%Y-%m-%d %H:%M:%S", &tm);
    return buffer;
}

ofstream FileOperationLogger::open_log_file() {
    const string path = resolve_path(_log_path, _log_file);
    ofstream file(path, ios::app);
    return file;
}

string
FileOperationLogger::resolve_path(const std::string &error_path, const std::string &error_file) {
    return (filesystem::path(error_path) / error_file).string();
}