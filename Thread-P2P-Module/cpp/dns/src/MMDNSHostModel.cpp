#include "../include/MMDNSHostModel.h"
#include "../include/MMDNSCommon.h"
#include <algorithm>
#include <sstream>
#include <chrono>

namespace mmdns {

MMDNSHostModel::MMDNSHostModel()
    : updateTime_(0), serverType_(DNSServerType::SYSTEM) {
    updateTime_ = std::chrono::duration_cast<std::chrono::seconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
}

MMDNSHostModel::MMDNSHostModel(const std::string& hostname)
    : hostname_(hostname), serverType_(DNSServerType::SYSTEM) {
    updateTime_ = std::chrono::duration_cast<std::chrono::seconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
}

void MMDNSHostModel::addIP(std::shared_ptr<MMDNSIPModel> ip) {
    if (ip && ip->isValid()) {
        ipList_.push_back(ip);
    }
}

bool MMDNSHostModel::hasValidIP() const {
    return !ipList_.empty() && ipList_[0]->isValid();
}

std::shared_ptr<MMDNSIPModel> MMDNSHostModel::getBestIP() const {
    if (ipList_.empty()) {
        return nullptr;
    }
    
    // 返回速度最快的IP（假设列表已排序）
    for (const auto& ip : ipList_) {
        if (ip->isValid() && ip->getSpeed() >= 0) {
            return ip;
        }
    }
    
    // 如果都没有测速，返回第一个
    return ipList_[0];
}

std::string MMDNSHostModel::getBestIPString() const {
    auto bestIP = getBestIP();
    return bestIP ? bestIP->getIP() : "";
}

void MMDNSHostModel::sortBySpeed() {
    std::sort(ipList_.begin(), ipList_.end(),
        [](const std::shared_ptr<MMDNSIPModel>& a, 
           const std::shared_ptr<MMDNSIPModel>& b) {
            // 无效的IP排在后面
            if (!a->isValid()) return false;
            if (!b->isValid()) return true;
            
            // 未测速的IP排在后面
            if (a->getSpeed() < 0) return false;
            if (b->getSpeed() < 0) return true;
            
            // 按速度从快到慢排序
            return a->getSpeed() < b->getSpeed();
        });
}

std::string MMDNSHostModel::toString() const {
    std::ostringstream oss;
    oss << "Host: " << hostname_ << "\n";
    oss << "IPs (" << ipList_.size() << "):\n";
    for (const auto& ip : ipList_) {
        oss << "  - " << ip->toString() << "\n";
    }
    return oss.str();
}

std::string MMDNSHostModel::toJson() const {
    std::ostringstream oss;
    oss << "{"
        << "\"hostname\":\"" << hostname_ << "\","
        << "\"updateTime\":" << updateTime_ << ","
        << "\"serverType\":" << static_cast<int>(serverType_) << ","
        << "\"ipList\":[";
    
    for (size_t i = 0; i < ipList_.size(); ++i) {
        if (i > 0) oss << ",";
        oss << ipList_[i]->toJson();
    }
    
    oss << "]}";
    return oss.str();
}

std::shared_ptr<MMDNSHostModel> MMDNSHostModel::fromJson(const std::string& json) {
    auto model = std::make_shared<MMDNSHostModel>();
    
    // 简单的JSON解析
    size_t hostnamePos = json.find("\"hostname\":\"");
    if (hostnamePos != std::string::npos) {
        size_t start = hostnamePos + 12;
        size_t end = json.find("\"", start);
        if (end != std::string::npos) {
            model->hostname_ = json.substr(start, end - start);
        }
    }
    
    // 解析IP列表（简化处理）
    size_t ipListPos = json.find("\"ipList\":[");
    if (ipListPos != std::string::npos) {
        size_t start = ipListPos + 10;
        size_t end = json.find("]", start);
        if (end != std::string::npos) {
            std::string ipListStr = json.substr(start, end - start);
            // 这里应该进一步解析每个IP对象
            // 为简化，这里不实现完整解析
        }
    }
    
    return model;
}

} // namespace mmdns