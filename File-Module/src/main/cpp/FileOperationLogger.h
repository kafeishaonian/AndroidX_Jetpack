//
// Created by 魏红明 on 2025/7/1.
//

#ifndef ALAEATPOSAPP_FILEOPERATIONLOGGER_H
#define ALAEATPOSAPP_FILEOPERATIONLOGGER_H

#include <fstream>
#include <filesystem>
#include <queue>
#include <mutex>
#include <atomic>
#include <thread>
#include <map>
#include <iomanip>
#include <condition_variable>

#include "FileOperationLog.h"

using namespace std;

class FileOperationLogger {

public:
    FileOperationLogger(const FileOperationLogger &) = delete;

    FileOperationLogger &operator=(const FileOperationLogger &) = delete;

    static FileOperationLogger &instance();

    void log_failure(FileOperation op,
                     const string &file_path,
                     const string &error_msg,
                     const string &module,
                     int error_code = 0,
                     const string &dest_path = "");

    bool start(const string& log_path,
               const string& log_file);

    void stop();


private:

    FileOperationLogger();

    void run_logger();

    static string get_date_time(time_t time);

    ofstream open_log_file();

    static string resolve_path(const std::string &error_path, const std::string &error_file);

    string _log_path;
    string _log_file;
    atomic<bool> _active;
    thread _writer_thread;

    mutable mutex _queue_mutex;
    queue<FileOperationLog> _log_queue;
    atomic<int> _pending_count;
    condition_variable _queue_cv;

};


#endif //ALAEATPOSAPP_FILEOPERATIONLOGGER_H
