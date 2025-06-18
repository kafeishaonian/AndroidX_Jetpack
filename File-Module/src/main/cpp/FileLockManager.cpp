//
// Created by 64860 on 2025/6/16.
//

#include "FileLockManager.h"


void FileLockManager::FileLock::lock_shared() {
    _mutex.lock_shared();
}

void FileLockManager::FileLock::unlock_shared() {
    _mutex.unlock_shared();
}

void FileLockManager::FileLock::lock() {
    _mutex.lock();
}

void FileLockManager::FileLock::unlock() {
    _mutex.unlock();
}

FileLockManager::LockPtr FileLockManager::get_lock(const std::string &file_path) {
    lock_guard map_lock(m_mutex);
    auto it = _locks.find(file_path);
    if (it != _locks.end()) {
        return it->second;
    }

    auto lock_ptr = make_shared<FileLock>();
    _locks.emplace(file_path, lock_ptr);
    return lock_ptr;
}

void FileLockManager::cleanup_unused() {
    lock_guard map_lock(m_mutex);
    for (auto it = _locks.begin(); it != _locks.end();) {
        if (it->second.use_count() == 1) {
            it = _locks.erase(it);
        } else {
            ++it;
        }
    }
}