#include "../include/MMDNSIPModel.h"
#include "../include/MMDNSCommon.h"
#include <sstream>
#include <chrono>

namespace mmdns {

MMDNSIPModel::MMDNSIPModel()
    : port_(0), speed_(-1), timestamp_(0), isValid_(true), ipVersion_(IPVersion::UNKNOWN) {
    timestamp_ = std::chrono::duration_cast<std::chrono::seconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
}

MMDNSIPModel::MMDNSIPModel(const std::string& ip, int port)
    : ip_(ip), port_(port), speed_(-1), isValid_(true), ipVersion_(IPVersion::UNKNOWN) {
    timestamp_ = std::chrono::duration_cast<std::chrono::seconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
    detectIPVersion();
}

void MMDNSIPModel::detectIPVersion() {
    // 检测是否为IPv4（包含点号但不包含冒号）
    if (ip_.find('.') != std::string::npos && ip_.find(':') == std::string::npos) {
        ipVersion_ = IPVersion::IPv4;
    }
    // 检测是否为IPv6（包含冒号）
    else if (ip_.find(':') != std::string::npos) {
        ipVersion_ = IPVersion::IPv6;
    }
    else {
        ipVersion_ = IPVersion::UNKNOWN;
    }
}

std::string MMDNSIPModel::getIPv6Compressed() const {
    if (!isIPv6()) {
        return ip_;
    }
    
    // 简化的IPv6压缩（实际应用应该使用更完善的实现）
    // 这里只是示例
    return ip_;
}

bool MMDNSIPModel::isIPv6LinkLocal() const {
    if (!isIPv6()) {
        return false;
    }
    // Link-local地址: fe80::/10
    return ip_.find("fe80:") == 0 || ip_.find("FE80:") == 0;
}

bool MMDNSIPModel::isIPv6SiteLocal() const {
    if (!isIPv6()) {
        return false;
    }
    // Site-local地址 (已废弃): fec0::/10
    return ip_.find("fec0:") == 0 || ip_.find("FEC0:") == 0;
}

bool MMDNSIPModel::isIPv6UniqueLocal() const {
    if (!isIPv6()) {
        return false;
    }
    // Unique-local地址: fc00::/7 或 fd00::/8
    return ip_.find("fc") == 0 || ip_.find("FC") == 0 ||
           ip_.find("fd") == 0 || ip_.find("FD") == 0;
}

bool MMDNSIPModel::isIPv6Global() const {
    if (!isIPv6()) {
        return false;
    }
    // 全局单播地址（排除特殊地址）
    return !isIPv6LinkLocal() && !isIPv6SiteLocal() &&
           !isIPv6UniqueLocal() && ip_ != "::1";
}

std::string MMDNSIPModel::toString() const {
    std::ostringstream oss;
    oss << "IP: " << ip_;
    if (port_ > 0) {
        oss << ":" << port_;
    }
    oss << ", Speed: " << speed_ << "ms";
    oss << ", Valid: " << (isValid_ ? "true" : "false");
    return oss.str();
}

std::string MMDNSIPModel::toJson() const {
    std::ostringstream oss;
    oss << "{"
        << "\"ip\":\"" << ip_ << "\","
        << "\"port\":" << port_ << ","
        << "\"speed\":" << speed_ << ","
        << "\"timestamp\":" << timestamp_ << ","
        << "\"valid\":" << (isValid_ ? "true" : "false")
        << "}";
    return oss.str();
}

std::shared_ptr<MMDNSIPModel> MMDNSIPModel::fromJson(const std::string& json) {
    auto model = std::make_shared<MMDNSIPModel>();
    
    // 简单的JSON解析（生产环境应使用专业库）
    size_t ipPos = json.find("\"ip\":\"");
    if (ipPos != std::string::npos) {
        size_t start = ipPos + 6;
        size_t end = json.find("\"", start);
        if (end != std::string::npos) {
            model->ip_ = json.substr(start, end - start);
        }
    }
    
    size_t portPos = json.find("\"port\":");
    if (portPos != std::string::npos) {
        size_t start = portPos + 7;
        size_t end = json.find(",", start);
        if (end != std::string::npos) {
            model->port_ = std::stoi(json.substr(start, end - start));
        }
    }
    
    size_t speedPos = json.find("\"speed\":");
    if (speedPos != std::string::npos) {
        size_t start = speedPos + 8;
        size_t end = json.find(",", start);
        if (end != std::string::npos) {
            model->speed_ = std::stoi(json.substr(start, end - start));
        }
    }
    
    return model;
}

} // namespace mmdns