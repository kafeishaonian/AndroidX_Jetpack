//
// Created by 64860 on 2025/6/16.
//

#ifndef ANDROIDX_JETPACK_BUSINESSDIRECTORYMANAGER_H
#define ANDROIDX_JETPACK_BUSINESSDIRECTORYMANAGER_H

#include <string>
#include <filesystem>
#include <shared_mutex>
#include <unordered_map>
#include <system_error>

using namespace std;

/**
 * 业务目录管理系统
 */
class BusinessDirectoryManager {

public:
    explicit BusinessDirectoryManager(const string& base_path);

    string resolve_path(const string& business_id,
                        const string& filename);

private:

    void create_directory(const string& path);

    string get_business_path(const string& business_id);

    string _base_path;
    unordered_map<string, string> _business_paths;
    shared_mutex m_mutex;
};


#endif //ANDROIDX_JETPACK_BUSINESSDIRECTORYMANAGER_H
