//
// Created by 64860 on 2025/6/16.
//

#include "FileMetadataManager.h"


FileMetadataManager::FileMetadata::FileMetadata(const std::string &path) {
    refresh(path);
}


void FileMetadataManager::FileMetadata::refresh(const std::string &path) {
    error_code ec;

    exists = filesystem::exists(path, ec);
    if (!exists || ec) {
        return;
    }

    auto status = filesystem::status(path, ec);
    if (ec) {
        exists = false;
        return;
    }

    if (!filesystem::is_regular_file(status)) {
        exists = false;
        return;
    }

    file_size = filesystem::file_size(path, ec);
    if (ec) {
        exists = false;
        file_size = 0;
        return;
    }

    last_modified = filesystem::last_write_time(path, ec);
    if (ec) {
        exists = false;
        return;
    }

    last_access = filesystem::file_time_type::clock::now();
}


bool FileMetadataManager::FileMetadata::is_outdated(const std::string &path) const {
    if (!exists) {
        return false;
    }

    error_code ec;
    auto current_mod_time = filesystem::last_write_time(path, ec);
    return ec || current_mod_time != last_modified;
}

void FileMetadataManager::update_metadata(const std::string &path) {
    unique_lock lock(_mutex);

    auto it = _metadata_map.find(path);
    if (it != _metadata_map.end()) {
        it->second.refresh(path);
    } else {
        _metadata_map.emplace(path, FileMetadata(path));
    }

    update_access_order(path);
}


const FileMetadataManager::FileMetadata &FileMetadataManager::get_metadata(const std::string &path) {
    unique_lock lock(_mutex);
    
    auto it = _metadata_map.find(path);
    if (it != _metadata_map.end()) {
        return it->second;
    }
    
    auto [new_it, _] = _metadata_map.emplace(path, FileMetadata(path));
    update_access_order(path);
    return new_it->second;
}


optional<FileMetadataManager::FileMetadata> FileMetadataManager::try_get_metadata(const std::string &path) const {
    shared_lock lock(_mutex);
    
    auto it = _metadata_map.find(path);
    if (it != _metadata_map.end()) {
        return it->second;
    }
    return nullopt;
}


void FileMetadataManager::remove_metadata(const std::string &path) {
    unique_lock lock(_mutex);
    _metadata_map.erase(path);
    remove_from_access_order(path);
}


void FileMetadataManager::cleanup_old_entries(int max_age_days) {
    unique_lock lock(_mutex);

    // 按时间清理
    if (max_age_days > 0) {
        auto now = filesystem::file_time_type::clock::now();
        auto threshold = now - chrono::hours(24 * max_age_days);

        for (auto it = _metadata_map.begin(); it != _metadata_map.end();) {
            if (it->second.last_access < threshold) {
                it = _metadata_map.erase(it);
            } else {
                ++it;
            }
        }
    }

    rebuild_access_order();
}


bool FileMetadataManager::cached_exists(const std::string &path) {
    auto meta = try_get_metadata(path);
    return meta.has_value() && meta->exists;
}


uint64_t FileMetadataManager::cached_file_size(const std::string &path) {
    auto meta = try_get_metadata(path);
    if (meta.has_value() && meta->exists && !meta->is_outdated(path)) {
        return meta->file_size;
    }

    return get_metadata(path).file_size;
}


void FileMetadataManager::update_access_order(const std::string &path) {
    remove_from_access_order(path);

    _access_order.push_front(path);

    if (_access_order.size() > MAX_ACCESS_ORDER_SIZE) {
        _access_order.resize(MAX_ACCESS_ORDER_SIZE);
    }
}

void FileMetadataManager::remove_from_access_order(const std::string& path) {
    for (auto it = _access_order.begin(); it != _access_order.end();) {
        if (*it == path) {
            it = _access_order.erase(it);
            return;
        } else {
            ++it;
        }
    }
}


void FileMetadataManager::rebuild_access_order() {
    _access_order.clear();

    vector<string> keys;
    keys.reserve(_metadata_map.size());
    for (const auto& [key, _] : _metadata_map) {
        keys.push_back(key);
    }

    sort(keys.begin(), keys.end(),
         [this](const string& a, const string& b) {
        return _metadata_map.at(a).last_access > _metadata_map.at(b).last_access;
    });

    size_t count = min(keys.size(), MAX_ACCESS_ORDER_SIZE);
    for (size_t i = 0; i < count; ++i) {
        _access_order.push_back(keys[i]);
    }
}