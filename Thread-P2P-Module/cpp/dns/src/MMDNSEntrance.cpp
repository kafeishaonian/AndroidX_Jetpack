#include "../include/MMDNSEntrance.h"
#include "../include/MMDNSCommon.h"

namespace mmdns {

// 静态成员初始化
std::unordered_map<std::string, std::shared_ptr<MMDNSEntrance>> MMDNSEntranceImpl::instances_;
std::mutex MMDNSEntranceImpl::instanceMutex_;

// ==================== MMDNSEntranceImpl ====================

MMDNSEntranceImpl::MMDNSEntranceImpl()
    : networkState_(MMDNSAppNetState::UNKNOWN),
      cacheDir_("/data/local/tmp/mmdns"),
      dohServer_("https://dns.google/dns-query"),
      systemDNSEnabled_(true),
      httpDNSEnabled_(false),
      localCacheEnabled_(true) {
    
    Logger::log(LogLevel::INFO, "MMDNSEntrance", "DNS入口已创建");
}

MMDNSEntranceImpl::~MMDNSEntranceImpl() {
    if (dnsServer_) {
        dnsServer_->stop();
    }
    Logger::log(LogLevel::INFO, "MMDNSEntrance", "DNS入口已销毁");
}

void MMDNSEntranceImpl::init() {
    std::lock_guard<std::mutex> lock(mutex_);
    
    Logger::log(LogLevel::INFO, "MMDNSEntrance", "开始初始化DNS服务");
    
    // 创建数据缓存
    dataCache_ = std::make_shared<MMDNSDataCache>(Constants::DEFAULT_CACHE_SIZE);
    dataCache_->setCacheDir(cacheDir_);
    
    // 创建主机管理器
    hostManager_ = std::make_shared<MMDNSHostManager>();
    hostManager_->setDataCache(dataCache_);
    
    // 创建测速器
    speedChecker_ = std::make_shared<MMDNSSpeedChecker>();
    
    // 创建DNS服务器
    dnsServer_ = std::make_shared<MMDNSServer>();
    dnsServer_->setHostManager(hostManager_);
    dnsServer_->setSpeedChecker(speedChecker_);
    
    // 初始化DNS处理器
    initDNSHandlers();
    
    // 启动DNS服务器
    dnsServer_->start();
    
    Logger::log(LogLevel::INFO, "MMDNSEntrance", "DNS服务初始化完成");
}

void MMDNSEntranceImpl::initDNSHandlers() {
    // 添加系统DNS
    if (systemDNSEnabled_) {
        auto systemDNS = std::make_shared<MMDNSSystemServerHandle>();
        dnsServer_->addServerHandle(DNSServerType::SYSTEM, systemDNS);
        Logger::log(LogLevel::INFO, "MMDNSEntrance", "已启用系统DNS");
    }
    
    // 添加HTTP DNS
    if (httpDNSEnabled_ && !dohServer_.empty()) {
        auto httpDNS = std::make_shared<MMDNSHttpServerHandle>(dohServer_);
        dnsServer_->addServerHandle(DNSServerType::HTTP_DNS, httpDNS);
        Logger::log(LogLevel::INFO, "MMDNSEntrance", "已启用HTTP DNS: " + dohServer_);
    }
    
    // 添加本地缓存
    if (localCacheEnabled_) {
        auto localDNS = std::make_shared<MMDNSLocalServerHandle>();
        dnsServer_->addServerHandle(DNSServerType::LOCAL, localDNS);
        Logger::log(LogLevel::INFO, "MMDNSEntrance", "已启用本地缓存");
    }
}

std::string MMDNSEntranceImpl::resolveHost(const std::string& hostname) {
    if (!dnsServer_) {
        Logger::log(LogLevel::ERROR, "MMDNSEntrance", "DNS服务未初始化");
        return "";
    }
    
    Logger::log(LogLevel::INFO, "MMDNSEntrance", "解析主机: " + hostname);
    
    auto host = dnsServer_->resolveSync(hostname);
    if (host && host->hasValidIP()) {
        std::string bestIP = host->getBestIPString();
        Logger::log(LogLevel::INFO, "MMDNSEntrance",
            "解析成功: " + hostname + " -> " + bestIP);
        return bestIP;
    }
    
    Logger::log(LogLevel::WARN, "MMDNSEntrance", "解析失败: " + hostname);
    return "";
}

void MMDNSEntranceImpl::resolveHostAsync(const std::string& hostname, TaskCallback callback) {
    if (!dnsServer_) {
        Logger::log(LogLevel::ERROR, "MMDNSEntrance", "DNS服务未初始化");
        if (callback) {
            callback(nullptr, false, nullptr);
        }
        return;
    }
    
    Logger::log(LogLevel::INFO, "MMDNSEntrance", "异步解析: " + hostname);
    dnsServer_->resolveAsync(hostname, callback);
}

std::vector<std::string> MMDNSEntranceImpl::getAllIPs(const std::string& hostname) {
    std::vector<std::string> ips;
    
    if (!dnsServer_) {
        Logger::log(LogLevel::ERROR, "MMDNSEntrance", "DNS服务未初始化");
        return ips;
    }
    
    auto host = dnsServer_->resolveSync(hostname);
    if (host) {
        auto ipList = host->getIPList();
        for (const auto& ipModel : ipList) {
            if (ipModel->isValid()) {
                ips.push_back(ipModel->getIP());
            }
        }
    }
    
    return ips;
}

void MMDNSEntranceImpl::setNetworkState(MMDNSAppNetState state) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    networkState_ = state;
    Logger::log(LogLevel::INFO, "MMDNSEntrance",
        "网络状态变更: " + std::to_string(static_cast<int>(state)));
    
    // 网络状态变化时可以清理缓存或重新初始化
    if (state == MMDNSAppNetState::NONE) {
        // 网络断开，可以保存缓存
        if (hostManager_) {
            hostManager_->saveToCache();
        }
    }
}

void MMDNSEntranceImpl::clear() {
    std::lock_guard<std::mutex> lock(mutex_);
    
    if (hostManager_) {
        hostManager_->clearCache();
    }
    
    if (dataCache_) {
        dataCache_->clear();
    }
    
    Logger::log(LogLevel::INFO, "MMDNSEntrance", "清空所有缓存");
}

void MMDNSEntranceImpl::setDohServer(const std::string& server) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    dohServer_ = server;
    Logger::log(LogLevel::INFO, "MMDNSEntrance", "设置DoH服务器: " + server);
    
    // 如果已初始化，重新设置HTTP DNS处理器
    if (dnsServer_ && httpDNSEnabled_) {
        auto httpDNS = std::make_shared<MMDNSHttpServerHandle>(server);
        dnsServer_->addServerHandle(DNSServerType::HTTP_DNS, httpDNS);
    }
}

void MMDNSEntranceImpl::enableSystemDNS(bool enable) {
    std::lock_guard<std::mutex> lock(mutex_);
    systemDNSEnabled_ = enable;
    Logger::log(LogLevel::INFO, "MMDNSEntrance",
        std::string("系统DNS: ") + (enable ? "启用" : "禁用"));
}

void MMDNSEntranceImpl::enableHttpDNS(bool enable) {
    std::lock_guard<std::mutex> lock(mutex_);
    httpDNSEnabled_ = enable;
    Logger::log(LogLevel::INFO, "MMDNSEntrance",
        std::string("HTTP DNS: ") + (enable ? "启用" : "禁用"));
}

void MMDNSEntranceImpl::enableLocalCache(bool enable) {
    std::lock_guard<std::mutex> lock(mutex_);
    localCacheEnabled_ = enable;
    Logger::log(LogLevel::INFO, "MMDNSEntrance",
        std::string("本地缓存: ") + (enable ? "启用" : "禁用"));
}

void MMDNSEntranceImpl::setCacheDir(const std::string& dir) {
    std::lock_guard<std::mutex> lock(mutex_);
    cacheDir_ = dir;
    if (dataCache_) {
        dataCache_->setCacheDir(dir);
    }
}

void MMDNSEntranceImpl::setCacheSize(size_t size) {
    // 需要重新创建缓存
    if (!dnsServer_ || !dnsServer_->isRunning()) {
        dataCache_ = std::make_shared<MMDNSDataCache>(size);
    }
}

void MMDNSEntranceImpl::setCacheExpireTime(int seconds) {
    // 可以存储到配置中，在创建LocalServerHandle时使用
    Logger::log(LogLevel::INFO, "MMDNSEntrance",
        "设置缓存过期时间: " + std::to_string(seconds) + "秒");
}

void MMDNSEntranceImpl::setThreadCount(int count) {
    if (dnsServer_) {
        dnsServer_->setThreadCount(count);
    }
}

MMDNSServer::ServerStats MMDNSEntranceImpl::getStats() const {
    if (dnsServer_) {
        return dnsServer_->getStats();
    }
    return MMDNSServer::ServerStats();
}

std::shared_ptr<MMDNSEntrance> MMDNSEntranceImpl::getInstance(const std::string& key) {
    std::lock_guard<std::mutex> lock(instanceMutex_);
    
    auto it = instances_.find(key);
    if (it != instances_.end()) {
        return it->second;
    }
    
    auto instance = std::make_shared<MMDNSEntranceImpl>();
    instances_[key] = instance;
    
    Logger::log(LogLevel::INFO, "MMDNSEntrance",
        "创建新实例: " + key);
    
    return instance;
}

void MMDNSEntranceImpl::removeInstance(const std::string& key) {
    std::lock_guard<std::mutex> lock(instanceMutex_);
    instances_.erase(key);
    
    Logger::log(LogLevel::INFO, "MMDNSEntrance",
        "删除实例: " + key);
}

void MMDNSEntranceImpl::clearAllInstances() {
    std::lock_guard<std::mutex> lock(instanceMutex_);
    instances_.clear();
    
    Logger::log(LogLevel::INFO, "MMDNSEntrance", "清空所有实例");
}

} // namespace mmdns