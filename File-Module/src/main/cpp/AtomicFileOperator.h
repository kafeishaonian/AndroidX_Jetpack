//
// Created by 64860 on 2025/6/16.
//

#ifndef ANDROIDX_JETPACK_ATOMICFILEOPERATOR_H
#define ANDROIDX_JETPACK_ATOMICFILEOPERATOR_H

#include "FileLockManager.h"
#include <fstream>
#include <system_error>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <filesystem>

using namespace std;

/**
 * 原子文件操作
 */
class AtomicFileOperator {

public:
    explicit AtomicFileOperator(FileLockManager& lock_manager);

    bool create_file(const string& path, const string& content);

    bool read_file(const string& path, string& output);

    bool update_file(const string& path, const string& content);

    bool append_file_safely(const string& path, const string& content);

    bool delete_file(const string& path);

    bool file_exists(const string& path);


private:
    static constexpr size_t MMAP_THRESHOLD = 1024 * 1024;

    void ensure_parent_directory(const string& path);

    bool mmap_read(const string& path, string& output);

    bool stream_read(const string& path, string& output);

    bool append_file(const string& path, const string& content);

    bool copy_file(const string& src, const string& dest);

    FileLockManager& _lock_manager;
};


#endif //ANDROIDX_JETPACK_ATOMICFILEOPERATOR_H
