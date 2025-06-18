//
// Created by 64860 on 2025/6/16.
//

#ifndef ANDROIDX_JETPACK_FILELOCKMANAGER_H
#define ANDROIDX_JETPACK_FILELOCKMANAGER_H

#include <string>
#include <memory>
#include <mutex>
#include <shared_mutex>
#include <unordered_map>

using namespace std;

/**
 * 文件锁管理系统
 */
class FileLockManager {

public:
    class FileLock{
    public:
        void lock_shared();
        void unlock_shared();
        void lock();
        void unlock();

    private:
        shared_mutex _mutex;
    };

    using LockPtr = shared_ptr<FileLock>;

    LockPtr get_lock(const string& file_path);

    void cleanup_unused();


private:
    unordered_map<string, LockPtr> _locks;
    mutex m_mutex;
};


#endif //ANDROIDX_JETPACK_FILELOCKMANAGER_H
