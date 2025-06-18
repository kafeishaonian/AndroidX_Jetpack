//
// Created by 64860 on 2025/6/16.
//

#ifndef ANDROIDX_JETPACK_FILEMETADATAMANAGER_H
#define ANDROIDX_JETPACK_FILEMETADATAMANAGER_H

#include <string>
#include <unordered_map>
#include <mutex>
#include <shared_mutex>
#include <chrono>
#include <filesystem>
#include <optional>
#include <list>
#include <system_error>
#include <atomic>

using namespace std;

class FileMetadataManager {

public:
    struct FileMetadata{
        filesystem::file_time_type last_access;
        filesystem::file_time_type last_modified;
        uint64_t file_size = 0;
        bool exists = false;

        FileMetadata() = default;

        explicit FileMetadata(const string& path);

        void refresh(const string& path);

        bool is_outdated(const string& path) const;
    };


    void update_metadata(const string& path);

    const FileMetadata& get_metadata(const string& path);

    optional<FileMetadata> try_get_metadata(const string& path) const;

    void remove_metadata(const string& path);

    void cleanup_old_entries(int max_age_days = 14);

    bool cached_exists(const string& path);

    uint64_t cached_file_size(const string& path);


private:
    void update_access_order(const string& path);

    void remove_from_access_order(const string& path);

    void rebuild_access_order();


    unordered_map<string, FileMetadata> _metadata_map;
    list<string> _access_order;
    mutable shared_mutex _mutex;
    static constexpr size_t MAX_ACCESS_ORDER_SIZE = 2000;
};


#endif //ANDROIDX_JETPACK_FILEMETADATAMANAGER_H
