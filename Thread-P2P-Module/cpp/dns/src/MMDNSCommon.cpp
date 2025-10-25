#include "../include/MMDNSCommon.h"
#include <android/log.h>
#include <ctime>

namespace mmdns {

// 静态成员初始化
LogLevel Logger::minLevel_ = LogLevel::INFO;

void Logger::log(LogLevel level, const std::string& tag, const std::string& message) {
    if (level < minLevel_) {
        return;
    }
    
    android_LogPriority priority;
    switch (level) {
        case LogLevel::DEBUG:
            priority = ANDROID_LOG_DEBUG;
            break;
        case LogLevel::INFO:
            priority = ANDROID_LOG_INFO;
            break;
        case LogLevel::WARN:
            priority = ANDROID_LOG_WARN;
            break;
        case LogLevel::ERROR:
            priority = ANDROID_LOG_ERROR;
            break;
        default:
            priority = ANDROID_LOG_INFO;
    }
    
    __android_log_print(priority, tag.c_str(), "%s", message.c_str());
}

void Logger::setLevel(LogLevel minLevel) {
    minLevel_ = minLevel;
}

} // namespace mmdns