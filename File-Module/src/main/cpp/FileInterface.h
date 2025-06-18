//
// Created by 64860 on 2025/6/17.
//

#ifndef ANDROIDX_JETPACK_FILEINTERFACE_H
#define ANDROIDX_JETPACK_FILEINTERFACE_H

#include <vector>
#include <string>
#include "FileManager.h"
#include "utils/log_utils.h"

using namespace std;

class FileInterface {

public:
    static FileInterface &getInstance();

    bool initManager(const string& base_path, size_t cache_capacity, bool use_async_writer);

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
    FileInterface();

    ~FileInterface();

    std::unique_ptr<FileManager> g_file_manager;
};


#endif //ANDROIDX_JETPACK_FILEINTERFACE_H
