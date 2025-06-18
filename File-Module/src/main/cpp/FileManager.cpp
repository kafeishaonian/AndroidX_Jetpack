//
// Created by 64860 on 2025/6/16.
//

#include "FileManager.h"

#define LOG_TAG "FileManager"

FileManager::FileManager(const string &base_path,
                         size_t cache_capacity,
                         bool use_async_writer)
        : _directory_manager(base_path),
          _lock_manager(),
          _file_operator(_lock_manager),
          _cache(cache_capacity),
          _metadata_manager(),
          _use_async_writer(use_async_writer),
          _stop_cleanup(false) {

    _maintenance_thread = thread([this] {
        maintenance_loop();
    });
}

FileManager::~FileManager() {
    _stop_cleanup.store(true, memory_order_relaxed);
    _maintenance_cv.notify_all();
    if (_maintenance_thread.joinable()) {
        _maintenance_thread.join();
    }
}

bool FileManager::create_file(const string &business_id,
                              const string &filename,
                              const string &content) {
    const string path = resolve_path(business_id, filename);
    _metadata_manager.update_metadata(path);

    auto start = chrono::high_resolution_clock::now();
    LOGE(LOG_TAG, "start time:= %lld", start);
    if (_use_async_writer) {
        auto cache_ptr = &_cache;
        enqueue_write([=] {
            auto middle = chrono::high_resolution_clock::now();
//            auto duration = chrono::duration_cast<chrono::milliseconds>();
            LOGE(LOG_TAG, "middle time:= %lld", (middle - start));
            if (_file_operator.create_file(path, content)) {
                cache_ptr->put(path, filename);
            }
            auto end = chrono::high_resolution_clock::now();
//            auto dura = chrono::duration_cast<chrono::milliseconds>(end - middle);
            LOGE(LOG_TAG, "end time:= %lld", (end - middle));
        });
        return true;
    }

    bool success = _file_operator.create_file(path, content);
    if (success) {
        _cache.put(path, filename);
    }
    return success;
}

bool FileManager::read_file(const std::string &business_id, const std::string &filename,
                            std::string &output) {
    const string path = resolve_path(business_id, filename);
    _metadata_manager.update_metadata(path);

    if (auto cached_name = _cache.get(path)) {
        if (*cached_name == filename) {
            return _file_operator.read_file(path, output);
        }
    }
    bool success = _file_operator.read_file(path, output);
    if (success) {
        _cache.put(path, filename);
    }
    return success;
}

bool FileManager::update_file(const std::string &business_id, const std::string &filename,
                              const std::string &content) {
    const string path = resolve_path(business_id, filename);
    _metadata_manager.update_metadata(path);

    if (_use_async_writer) {
        enqueue_write([=] {
            _file_operator.update_file(path, content);
        });
        return true;
    }

    return _file_operator.update_file(path, content);
}

bool FileManager::append_file(const std::string &business_id, const std::string &filename,
                              const std::string &content) {
    const string path = resolve_path(business_id, filename);
    if (_use_async_writer) {
        enqueue_write([=] {

            _file_operator.append_file_safely(path, content);
        });
        return true;
    }
    return _file_operator.append_file_safely(path, content);
}


bool FileManager::delete_file(const std::string &business_id, const std::string &filename) {
    const string path = resolve_path(business_id, filename);
    _metadata_manager.remove_metadata(path);
    _cache.remove(path);

    if (_use_async_writer) {
        enqueue_write([=] {
            _file_operator.delete_file(path);
        });
        return true;
    }

    return _file_operator.delete_file(path);
}


bool FileManager::file_exists(const std::string &business_id, const std::string &filename) {
    const string path = resolve_path(business_id, filename);
    _metadata_manager.update_metadata(path);
    return _file_operator.file_exists(path);
}


void FileManager::prefetch_directory(const std::string &business_id,
                                     const std::string &sub_str,
                                     const int32_t day,
                                     const bool flag,
                                     vector<string> &files) {
    const string dir_path = _directory_manager.resolve_path(business_id, "");

    list_directory(dir_path, sub_str, day, flag, files);

    for(const auto& filename: files) {
        const string full_path = resolve_path(business_id, filename);
        _cache.put(full_path, filename);
        _metadata_manager.update_metadata(full_path);
    }
}


string FileManager::resolve_path(const std::string &business_id, const std::string &filename) {
    return _directory_manager.resolve_path(business_id, filename);
}


void FileManager::list_directory(const std::string &path,
                                 const std::string &sub_str,
                                 const int32_t day,
                                 const bool flag,
                                 vector<std::string> &files) {
    if (path.empty()) {
        return;
    }

    error_code ec;
    auto now = filesystem::file_time_type::clock::now();
    auto iter = filesystem::directory_iterator(path, ec);
    if (ec) {
        return;
    }

    bool use_time_filer = (day > 0);
    filesystem::file_time_type cutoff_time;
    if (use_time_filer) {
        auto duration = chrono::hours(24 * day);
        cutoff_time = now - duration;
    }
    bool should_filter = !sub_str.empty();

    for(const auto& entry: iter) {
        if (entry.is_directory(ec) || ec) continue;
        if (!entry.is_regular_file()) continue;

        string filename = entry.path().filename().string();
        auto file_time = entry.last_write_time(ec);
        if (ec) continue;

        if (should_filter && filename.find(sub_str) == string::npos) {
            continue;
        }

        if (use_time_filer) {
            if (flag) {
                if (file_time < cutoff_time) continue;
            } else {
                if (file_time >= cutoff_time) continue;
            }
        }
        files.push_back(filename);
    }
}

void FileManager::enqueue_write(function<void()> task) {
    if (!_async_writer) {
        init_async_write();
    }
    _async_writer->enqueue(std::move(task));
}

void FileManager::init_async_write() {
    call_once(_async_init_flag, [this] {
        _async_writer = make_unique<AsyncBatchWriter>();
        _async_writer->set_batch_params(10, 100,
                                        chrono::milliseconds(50),
                                        chrono::milliseconds(200));
    });
}


void FileManager::maintenance_loop() {
    while(!_stop_cleanup.load()) {
        unique_lock lock(_maintenance_mutex);
        _maintenance_cv.wait_for(lock, chrono::minutes(5));

        if (_stop_cleanup.load()) {
            break;
        }
        perform_maintenance();
    }
}


void FileManager::perform_maintenance() {
    _lock_manager.cleanup_unused();
    _metadata_manager.cleanup_old_entries();

    const size_t cache_capacity = _cache.capacity();
    const size_t cache_size = _cache.size();

    if (cache_size > cache_capacity * 0.8) {
        std::call_once(_cache_cleanup_flag, [this] {
            _maintenance_cv.notify_one();
        });
    }
}