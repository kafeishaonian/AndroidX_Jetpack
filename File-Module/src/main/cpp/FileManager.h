//
// Created by 64860 on 2025/6/16.
//

#ifndef ANDROIDX_JETPACK_FILEMANAGER_H
#define ANDROIDX_JETPACK_FILEMANAGER_H

#include "BusinessDirectoryManager.h"
#include "FileLockManager.h"
#include "AtomicFileOperator.h"
#include "ConcurrentLRUCache.h"
#include "FileMetadataManager.h"
#include "AsyncBatchWriter.h"
#include <atomic>
#include <thread>
#include <memory>
#include <functional>
#include <chrono>
#include <filesystem>
#include <string>
#include <vector>
#include <system_error>
#include "utils/log_utils.h"

using namespace std;

class FileManager {

public:
    FileManager(const string& base_path,
                size_t cache_capacity = 1000,
                bool use_async_writer = true);

    ~FileManager();

    bool create_file(const string& business_id,
                     const string& filename,
                     const string& content);

    bool read_file(const string& business_id,
                   const string& filename,
                   string& output);

    bool update_file(const string& business_id,
                     const string& filename,
                     const string& content);

    bool append_file(const string& business_id,
                     const string& filename,
                     const string& content);

    bool delete_file(const string& business_id,
                     const string& filename);

    bool file_exists(const string& business_id,
                     const string& filename);

    void prefetch_directory(const string& business_id,
                            const string& sub_str,
                            const int32_t day,
                            const bool flag,
                            vector<string> &files);

private:
    string resolve_path(const string& business_id,
                        const string& filename);


    void list_directory(const string& path,
                        const string& sub_str,
                        const int32_t day,
                        const bool flag,
                        vector<string>& files);

    void enqueue_write(function<void()> task);

    void init_async_write();

    void maintenance_loop();

    void perform_maintenance();

    BusinessDirectoryManager _directory_manager;
    FileLockManager _lock_manager;
    AtomicFileOperator _file_operator;
    ConcurrentLRUCache<string, string> _cache;
    FileMetadataManager _metadata_manager;
    unique_ptr<AsyncBatchWriter> _async_writer;
    once_flag _async_init_flag;
    once_flag _cache_cleanup_flag;
    bool _use_async_writer;

    thread _maintenance_thread;
    mutex _maintenance_mutex;
    condition_variable _maintenance_cv;
    atomic<bool> _stop_cleanup;

};


#endif //ANDROIDX_JETPACK_FILEMANAGER_H
