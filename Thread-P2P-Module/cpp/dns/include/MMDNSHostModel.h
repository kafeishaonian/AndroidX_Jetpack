#pragma once

#include "MMDNSCommon.h"
#include "MMDNSIPModel.h"

namespace mmdns {

class MMDNSHostModel {
public:
    MMDNSHostModel();
    explicit MMDNSHostModel(const std::string& hostname);
    
    // Getter
    std::string getHostname() const { return hostname_; }
    std::vector<std::shared_ptr<MMDNSIPModel>> getIPList() const { return ipList_; }
    long getUpdateTime() const { return updateTime_; }
    DNSServerType getServerType() const { return serverType_; }
    
    // Setter
    void setHostname(const std::string& hostname) { hostname_ = hostname; }
    void setUpdateTime(long time) { updateTime_ = time; }
    void setServerType(DNSServerType type) { serverType_ = type; }
    
    // IP管理
    void addIP(std::shared_ptr<MMDNSIPModel> ip);
    void clearIPs() { ipList_.clear(); }
    bool hasValidIP() const;
    
    // 获取最快的IP
    std::shared_ptr<MMDNSIPModel> getBestIP() const;
    std::string getBestIPString() const;
    
    // 排序IP列表（按速度）
    void sortBySpeed();
    
    // 序列化
    std::string toString() const;
    std::string toJson() const;
    static std::shared_ptr<MMDNSHostModel> fromJson(const std::string& json);
    
private:
    std::string hostname_;
    std::vector<std::shared_ptr<MMDNSIPModel>> ipList_;
    long updateTime_;
    DNSServerType serverType_;
};

} // namespace mmdns