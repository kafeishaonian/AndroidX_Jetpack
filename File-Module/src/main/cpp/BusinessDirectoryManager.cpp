//
// Created by 64860 on 2025/6/16.
//

#include "BusinessDirectoryManager.h"


BusinessDirectoryManager::BusinessDirectoryManager(const string &base_path): _base_path(base_path) {
    create_directory(_base_path);
}

string BusinessDirectoryManager::resolve_path(const std::string &business_id,
                                              const std::string &filename) {
    string business_path = get_business_path(business_id);
    return (filesystem::path(business_path) / filename).string();
}


void BusinessDirectoryManager::create_directory(const std::string &path) {
    error_code ec;
    if (!filesystem::exists(path, ec) && !ec) {
        filesystem::create_directories(path, ec);
    }
}

string BusinessDirectoryManager::get_business_path(const std::string &business_id) {
    // 快速路径：尝试共享锁读取
    {
        shared_lock lock(m_mutex);
        auto it = _business_paths.find(business_id);
        if (it != _business_paths.end()) {
            return it->second;
        }
    }

    unique_lock lock(m_mutex);
    auto [it, inserted] = _business_paths.try_emplace(business_id);
    if (inserted) {
        string path = (filesystem::path(_base_path) / business_id).string();
        create_directory(path);
        it->second = std::move(path);
    }
    return it->second;
}