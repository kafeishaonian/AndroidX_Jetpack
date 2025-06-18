//
// Created by 64860 on 2025/6/17.
//

#include "FileInterface.h"

#define TAG "FileInterface.cpp"

FileInterface::FileInterface() {}


FileInterface::~FileInterface() {

}

FileInterface &FileInterface::getInstance() {
    static FileInterface instance;
    return instance;
}


bool
FileInterface::initManager(const std::string &base_path, size_t cache_capacity, bool use_async_writer) {
    try {
        g_file_manager = std::make_unique<FileManager>(
                base_path,
                cache_capacity,
                use_async_writer
        );
        return true;
    } catch (const std::exception& e) {
        LOGE(TAG, "Exception initializing FileManager: %s", e.what());
        return false;
    } catch (...) {
        LOGE(TAG, "Unknown exception initializing FileManager");
        return false;
    }
}


bool FileInterface::create_file(const std::string &business_id, const std::string &filename,
                                const std::string &content) {
    if (!g_file_manager) {
        LOGE(TAG, "FileManager not initialized");
        return false;
    }

    return g_file_manager->create_file(
            business_id,
            filename,
            content
    );
}

bool FileInterface::read_file(const std::string &business_id, const std::string &filename,
                              std::string &output) {
    if (!g_file_manager) {
        LOGE(TAG, "FileManager not initialized");
        return false;
    }
    return g_file_manager->read_file(business_id, filename, output);
}


bool FileInterface::update_file(const std::string &business_id, const std::string &filename,
                                const std::string &content) {
    if (!g_file_manager) {
        LOGE(TAG, "FileManager not initialized");
        return false;
    }

    return g_file_manager->update_file(business_id, filename, content);
}

bool FileInterface::append_file(const std::string &business_id, const std::string &filename,
                                const std::string &content) {
    if (!g_file_manager) {
        LOGE(TAG, "FileManager not initialized");
        return false;
    }

    return g_file_manager->append_file(business_id, filename, content);
}

bool FileInterface::delete_file(const std::string &business_id, const std::string &filename) {
    if (!g_file_manager) {
        LOGE(TAG, "FileManager not initialized");
        return false;
    }

    return g_file_manager->delete_file(business_id, filename);
}


bool FileInterface::file_exists(const std::string &business_id, const std::string &filename) {
    if (!g_file_manager) {
        LOGE(TAG, "FileManager not initialized");
        return false;
    }

    return g_file_manager->file_exists(business_id, filename);
}

void FileInterface::prefetch_directory(const std::string &business_id,
                                       const std::string &sub_str,
                                       const int32_t day,
                                       const bool flag,
                                       vector<std::string> &files) {
    if (!g_file_manager) {
        LOGE(TAG, "FileManager not initialized");
        return;
    }
    g_file_manager->prefetch_directory(business_id, sub_str, day, flag,files);
}