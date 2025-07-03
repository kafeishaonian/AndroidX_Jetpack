//
// Created by 魏红明 on 2025/6/30.
//

#ifndef ALAEATPOSAPP_FILEOPERATIONLOG_H
#define ALAEATPOSAPP_FILEOPERATIONLOG_H

#include <chrono>
#include <string>
#include <string_view>
#include <sstream>
#include <vector>
#include <type_traits>

using namespace std;

enum class FileOperation {
    CREATE,
    UPDATE,
    DELETE,
    EXISTS,
    RENAME,
    ACCESS
};

enum class ErrorCode {
    FS_FILE_OPEN_FAILED = 1001,
    FS_WRITE_FILE_FAILED = 1002,
    FS_FILE_RENAME_FAILED = 1003,
    FS_FILE_FD_NOT_FOUND = 1004,
    FS_FILE_DELETE_FAILED = 1005,
    FS_FILE_NOT_FOUND = 1006,
};

struct FileOperationLog {
    time_t timestamp;           // 时间戳
    FileOperation operation;    // 操作类型
    string file_path;           // 文件路径
    string dest_path;           // 移动操作的目标路径
    string error_message;       // 错误信息
    string calling_module;      // 调用模块信息
    uint64_t file_id = 0;       // 文件唯一ID
    int error_code = 0;         // 错误代码码

    string to_string() const {
        return format("[{}] {} for {} failed: {} (code: {})",
                      format_time(timestamp),
                      operation_to_string(operation),
                      file_path,
                      error_message,
                      error_code);
    }


    string to_json() const {
        return format(R"({{
    "timestamp": {},
    "operation": "{}",
    "file_path": "{}",
    "dest_path": "{}",
    "error_code": {},
    "error_message": "{}",
    "module": "{}",
    "file_id": {}
}})", get_date_time(timestamp),
                      operation_to_string(operation),
                      file_path,
                      dest_path,
                      error_code,
                      error_message,
                      calling_module,
                      file_id);
    }

private:

    static string format_time(time_t t) {
        char buffer[80];
        strftime(buffer, sizeof(buffer), "%Y-%m-%d %H:%M:%S", localtime(&t));
        return buffer;
    }

    static string operation_to_string(FileOperation op) {
        switch (op) {
            case FileOperation::CREATE:
                return "CREATE";
            case FileOperation::UPDATE:
                return "UPDATE";
            case FileOperation::DELETE:
                return "DELETE";
            case FileOperation::EXISTS:
                return "EXISTS";
            case FileOperation::RENAME:
                return "RENAME";
            case FileOperation::ACCESS:
                return "ACCESS";
            default:
                return "UNKNOWN";
        }
    }

    static string get_date_time(time_t time) {
        tm tm = *localtime(&time);
        char buffer[20];
        strftime(buffer, sizeof(buffer), "%Y-%m-%d %H:%M:%S", &tm);
        return buffer;
    }


    template<typename T>
    static string any_to_string(T value) {
        ostringstream oss;
        oss << value;
        return oss.str();
    }

    template<typename... Args>
    static string format(const string &fmt, Args &&... args) {
        vector<string> parsedArgs = {any_to_string(std::forward<Args>(args))...};
        ostringstream oss;
        size_t last = 0;
        size_t index = 0;

        for (size_t pos = 0; pos < fmt.size(); ++pos) {
            if (fmt[pos] == '{') {
                //检查是否是转义
                if (pos + 1 < fmt.size() && fmt[pos + 1] == '{') {
                    oss << fmt.substr(last, pos - last);
                    oss << '{';
                    ++pos;
                    last = pos + 1;
                } else if (pos + 1 < fmt.size() && fmt[pos + 1] == '}') {
                    oss << fmt.substr(last, pos - last);
                    if (index < parsedArgs.size()) {
                        oss << parsedArgs[index++];
                    }
                    ++pos;
                    last = pos + 1;
                }
            } else if (fmt[pos] == '}' && pos + 1 < fmt.size() && fmt[pos + 1] == '}') {
                oss << fmt.substr(last, pos - last);
                oss << '}';
                ++pos;
                last = pos + 1;
            }
        }
        oss << fmt.substr(last);
        return oss.str();
    }

};

#endif //ALAEATPOSAPP_FILEOPERATIONLOG_H
