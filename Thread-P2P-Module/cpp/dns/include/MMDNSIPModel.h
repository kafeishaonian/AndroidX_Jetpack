#pragma once

#include "MMDNSCommon.h"

namespace mmdns {

class MMDNSIPModel {
public:
    enum class IPVersion {
        IPv4,
        IPv6,
        UNKNOWN
    };
    
    MMDNSIPModel();
    explicit MMDNSIPModel(const std::string& ip, int port = 0);
    
    // Getter
    std::string getIP() const { return ip_; }
    int getPort() const { return port_; }
    int getSpeed() const { return speed_; }
    long getTimestamp() const { return timestamp_; }
    bool isValid() const { return isValid_; }
    
    // IP版本检测
    IPVersion getIPVersion() const { return ipVersion_; }
    bool isIPv4() const { return ipVersion_ == IPVersion::IPv4; }
    bool isIPv6() const { return ipVersion_ == IPVersion::IPv6; }
    
    // IPv6特定功能
    std::string getIPv6Compressed() const;
    bool isIPv6LinkLocal() const;
    bool isIPv6SiteLocal() const;
    bool isIPv6UniqueLocal() const;
    bool isIPv6Global() const;
    
    // Setter
    void setIP(const std::string& ip) { ip_ = ip; }
    void setPort(int port) { port_ = port; }
    void setSpeed(int speed) { speed_ = speed; }
    void setTimestamp(long timestamp) { timestamp_ = timestamp; }
    void setValid(bool valid) { isValid_ = valid; }
    
    // 序列化
    std::string toString() const;
    std::string toJson() const;
    static std::shared_ptr<MMDNSIPModel> fromJson(const std::string& json);
    
    // 比较运算符（用于排序）
    bool operator<(const MMDNSIPModel& other) const {
        return speed_ < other.speed_;
    }
    
private:
    std::string ip_;
    int port_;
    int speed_;          // 延迟时间（毫秒），-1表示未测速或失败
    long timestamp_;     // 创建/更新时间戳
    bool isValid_;       // 是否有效
    IPVersion ipVersion_; // IP版本
    
    // 检测IP版本
    void detectIPVersion();
};

} // namespace mmdns