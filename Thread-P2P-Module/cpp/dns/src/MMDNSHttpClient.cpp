#include "../include/MMDNSHttpClient.h"
#include <sstream>
#include <algorithm>

namespace mmdns {

MMDNSHttpClient::MMDNSHttpClient()
    : timeout_(5)
    , connectTimeout_(3)
    , verifySSL_(true)
    , userAgent_("MMDNS/1.0") {
    
    // 初始化libcurl（全局）
    curl_global_init(CURL_GLOBAL_DEFAULT);
}

MMDNSHttpClient::~MMDNSHttpClient() {
    // 清理libcurl（全局）
    curl_global_cleanup();
}

size_t MMDNSHttpClient::writeCallback(void* contents, size_t size, size_t nmemb, void* userp) {
    size_t totalSize = size * nmemb;
    std::string* response = static_cast<std::string*>(userp);
    response->append(static_cast<char*>(contents), totalSize);
    return totalSize;
}

CURL* MMDNSHttpClient::createCurlHandle() {
    CURL* curl = curl_easy_init();
    if (!curl) {
        return nullptr;
    }
    
    // 设置超时
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, timeout_);
    curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, connectTimeout_);
    
    // SSL设置
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, verifySSL_ ? 1L : 0L);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, verifySSL_ ? 2L : 0L);
    
    // User-Agent
    curl_easy_setopt(curl, CURLOPT_USERAGENT, userAgent_.c_str());
    
    // 跟随重定向
    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
    curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 3L);
    
    return curl;
}

std::string MMDNSHttpClient::sendDohRequest(const std::string& url,
                                            const std::string& hostname,
                                            const std::string& recordType) {
    
    CURL* curl = createCurlHandle();
    if (!curl) {
        Logger::log(LogLevel::ERROR, "HttpClient", "Failed to create CURL handle");
        return "";
    }
    
    // 构建完整URL
    std::string fullUrl = url;
    if (fullUrl.find('?') == std::string::npos) {
        fullUrl += "?";
    } else {
        fullUrl += "&";
    }
    fullUrl += "name=" + hostname + "&type=" + recordType;
    
    Logger::log(LogLevel::DEBUG, "HttpClient", "DoH request: " + fullUrl);
    
    // 设置URL
    curl_easy_setopt(curl, CURLOPT_URL, fullUrl.c_str());
    
    // 设置HTTP头
    struct curl_slist* headers = nullptr;
    headers = curl_slist_append(headers, "Accept: application/dns-json");
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    
    // 设置响应回调
    std::string response;
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeCallback);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);
    
    // 执行请求
    CURLcode res = curl_easy_perform(curl);
    
    // 清理
    curl_slist_free_all(headers);
    
    if (res != CURLE_OK) {
        Logger::log(LogLevel::ERROR, "HttpClient", 
            std::string("CURL failed: ") + curl_easy_strerror(res));
        curl_easy_cleanup(curl);
        return "";
    }
    
    // 检查HTTP状态码
    long httpCode = 0;
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &httpCode);
    curl_easy_cleanup(curl);
    
    if (httpCode != 200) {
        Logger::log(LogLevel::ERROR, "HttpClient", 
            "HTTP error code: " + std::to_string(httpCode));
        return "";
    }
    
    Logger::log(LogLevel::DEBUG, "HttpClient", 
        "DoH response received: " + std::to_string(response.length()) + " bytes");
    
    return response;
}

std::vector<std::string> MMDNSHttpClient::extractIPsFromJson(const std::string& json) {
    std::vector<std::string> ips;
    
    // 简单的JSON解析 - 查找 "data" 字段中的IP地址
    // 格式: "data":"93.184.216.34"
    
    size_t pos = 0;
    const std::string dataKey = "\"data\"";
    
    while ((pos = json.find(dataKey, pos)) != std::string::npos) {
        // 找到 "data" 后的值
        pos += dataKey.length();
        
        // 跳过空格和冒号
        while (pos < json.length() && (json[pos] == ' ' || json[pos] == ':')) {
            pos++;
        }
        
        // 应该是引号开始
        if (pos < json.length() && json[pos] == '"') {
            pos++; // 跳过开始引号
            
            // 提取到下一个引号之间的内容
            size_t endPos = json.find('"', pos);
            if (endPos != std::string::npos) {
                std::string value = json.substr(pos, endPos - pos);
                
                // 验证是否是IP地址（简单检查）
                if (value.find('.') != std::string::npos || value.find(':') != std::string::npos) {
                    ips.push_back(value);
                    Logger::log(LogLevel::DEBUG, "HttpClient", "Extracted IP: " + value);
                }
            }
        }
        
        pos++;
    }
    
    return ips;
}

std::vector<std::string> MMDNSHttpClient::parseDohResponse(const std::string& jsonResponse) {
    if (jsonResponse.empty()) {
        return {};
    }
    
    // 检查Status字段
    size_t statusPos = jsonResponse.find("\"Status\"");
    if (statusPos != std::string::npos) {
        size_t colonPos = jsonResponse.find(':', statusPos);
        if (colonPos != std::string::npos) {
            size_t numStart = colonPos + 1;
            while (numStart < jsonResponse.length() && 
                   (jsonResponse[numStart] == ' ' || jsonResponse[numStart] == '\t')) {
                numStart++;
            }
            if (numStart < jsonResponse.length() && jsonResponse[numStart] != '0') {
                Logger::log(LogLevel::WARN, "HttpClient", "DoH response status non-zero");
            }
        }
    }
    
    // 提取IP地址
    auto ips = extractIPsFromJson(jsonResponse);
    
    Logger::log(LogLevel::INFO, "HttpClient", 
        "Parsed " + std::to_string(ips.size()) + " IP addresses from DoH response");
    
    return ips;
}

} // namespace mmdns