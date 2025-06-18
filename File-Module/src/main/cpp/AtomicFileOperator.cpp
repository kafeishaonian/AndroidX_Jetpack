//
// Created by 64860 on 2025/6/16.
//

#include "AtomicFileOperator.h"

AtomicFileOperator::AtomicFileOperator(FileLockManager &lock_manager) : _lock_manager(
        lock_manager) {}

bool AtomicFileOperator::create_file(const std::string &path, const std::string &content) {
    auto lock = _lock_manager.get_lock(path);

    unique_lock exclusive_lock(*lock);

    ensure_parent_directory(path);

    ofstream file(path, ios::binary);
    if (!file) {
        return false;
    }

    file.write(content.data(), content.size());
    return file.good();
}

bool AtomicFileOperator::read_file(const std::string &path, std::string &output) {
    auto lock = _lock_manager.get_lock(path);
    shared_lock shared_lock(*lock);

    if (filesystem::file_size(path) > MMAP_THRESHOLD) {
        return mmap_read(path, output);
    }
    return stream_read(path, output);
}

bool AtomicFileOperator::update_file(const std::string &path, const std::string &content) {
    auto lock = _lock_manager.get_lock(path);
    unique_lock exclusive_lock(*lock);

    // 原子更新策略：写入临时文件后重命名
    string temp_path = path + ".tmp";
    {
        ensure_parent_directory(temp_path);
        ofstream file(temp_path, ios::binary);
        if (!file) {
            return false;
        }
        file.write(content.data(), content.size());
        if  (!file.good()) {
            filesystem::remove(temp_path);
            return false;
        }
    }

    if (rename(temp_path.c_str(), path.c_str()) != 0) {
        filesystem::remove(temp_path);
        return false;
    }
    return true;
}

bool AtomicFileOperator::append_file_safely(const std::string &path, const std::string &content) {
    auto lock = _lock_manager.get_lock(path);
    unique_lock exclusive_lock(*lock);

    string temp_path = path + ".tmp";
    {
        ensure_parent_directory(temp_path);
        ofstream file(temp_path, ios::binary);
        if (!file) {
            return false;
        }
        if (!copy_file(path, temp_path)) {
            filesystem::remove(temp_path);
            return false;
        }

        if (!append_file(temp_path, content)) {
            filesystem::remove(temp_path);
            return false;
        }
    }

    if (rename(temp_path.c_str(), path.c_str()) != 0) {
        filesystem::remove(temp_path);
        return false;
    }

    return true;
}

bool AtomicFileOperator::delete_file(const std::string &path) {
    auto lock = _lock_manager.get_lock(path);
    unique_lock exclusive_lock(*lock);

    error_code ec;
    return filesystem::remove(path, ec);
}


bool AtomicFileOperator::file_exists(const std::string &path) {
    auto lock = _lock_manager.get_lock(path);
    shared_lock shared_lock(*lock);

    error_code  ec;
    return filesystem::exists(path, ec);
}


void AtomicFileOperator::ensure_parent_directory(const std::string &path) {
    error_code ec;
    auto parent_path = filesystem::path(path).parent_path();
    if (!filesystem::exists(parent_path, ec)) {
        filesystem::create_directories(parent_path, ec);
    }
}


bool AtomicFileOperator::mmap_read(const std::string &path, std::string &output) {
    int fd = open(path.c_str(), O_RDONLY);
    if (fd == -1) {
        return false;
    }

    struct stat sb;
    if (fstat(fd, &sb) == -1) {
        close(fd);
        return false;
    }

    size_t file_size = sb.st_size;
    if (file_size == 0) {
        output.clear();
        close(fd);
        return true;
    }

    void* addr = mmap(nullptr, file_size, PROT_READ, MAP_PRIVATE, fd, 0);
    if (addr == MAP_FAILED) {
        close(fd);
        return false;
    }

    output.assign(static_cast<const char*>(addr), file_size);

    munmap(addr, file_size);
    close(fd);
    return true;
}


bool AtomicFileOperator::stream_read(const std::string &path, std::string &output) {
    ifstream file(path, ios::binary | ios::ate);
    if (!file) {
        return false;
    }

    size_t size = file.tellg();
    file.seekg(0, ios::beg);

    output.resize(size);
    file.read(output.data(), size);
    return !file.fail();
}

bool AtomicFileOperator::append_file(const std::string &path, const std::string &content) {
    ofstream file(path, ios::app | ios::binary);
    if (!file) {
        return false;
    }

    file.write(content.data(), content.size());
    return file.good();
}

bool AtomicFileOperator::copy_file(const std::string &src, const std::string &dest) {
    ifstream in(src, ios::binary);
    if (!in) {
        return false;
    }

    ofstream out(dest, ios::binary | ios::trunc);
    if (!out) {
        return false;
    }

    out << in.rdbuf();

    return in.good() && out.good();
}