#pragma once

#include "MMDNSCommon.h"
#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <curl/curl.h>

namespace mmdns {

/**
 * HTTP客户端 - 用于DNS over HTTPS (DoH)
 * 使用libcurl实现HTTP请求
 */
class MMDNSHttpClient {
public:
    MMDNSHttpClient();
    ~MMDNSHttpClient();
    
    /**
     * 发送DoH请求
     * @param url DoH服务器URL (例如: https://dns.google/dns-query)
     * @param hostname 要解析的主机名
     * @param recordType DNS记录类型 ("A" 或 "AAAA")
     * @return JSON格式的响应字符串
     */
    std::string sendDohRequest(const std::string& url,
                               const std::string& hostname,
                               const std::string& recordType = "A");
    
    /**
     * 解析DoH JSON响应
     * @param jsonResponse JSON格式的响应
     * @return IP地址列表
     */
    std::vector<std::string> parseDohResponse(const std::string& jsonResponse);
    
    /**
     * 设置超时时间（秒）
     */
    void setTimeout(long seconds) { timeout_ = seconds; }
    
    /**
     * 设置连接超时（秒）
     */
    void setConnectTimeout(long seconds) { connectTimeout_ = seconds; }
    
    /**
     * 启用/禁用SSL验证
     */
    void setVerifySSL(bool verify) { verifySSL_ = verify; }
    
    /**
     * 设置User-Agent
     */
    void setUserAgent(const std::string& ua) { userAgent_ = ua; }
    
private:
    long timeout_;              // 总超时时间
    long connectTimeout_;       // 连接超时时间
    bool verifySSL_;           // 是否验证SSL证书
    std::string userAgent_;    // User-Agent字符串
    
    /**
     * CURL写回调函数
     */
    static size_t writeCallback(void* contents, size_t size, size_t nmemb, void* userp);
    
    /**
     * 初始化CURL句柄
     */
    CURL* createCurlHandle();
    
    /**
     * 简单的JSON解析（提取IP地址）
     */
    std::vector<std::string> extractIPsFromJson(const std::string& json);
};

} // namespace mmdns