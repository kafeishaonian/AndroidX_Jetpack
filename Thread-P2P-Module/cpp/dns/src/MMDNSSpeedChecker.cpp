#include "../include/MMDNSSpeedChecker.h"
#include "../include/MMDNSCommon.h"
#include <algorithm>
#include <future>
#include <chrono>

namespace mmdns {

// ==================== MMDNSSpeedChecker ====================

MMDNSSpeedChecker::MMDNSSpeedChecker() {
    pinger_ = std::make_shared<MMPing>();
}

MMDNSSpeedChecker::~MMDNSSpeedChecker() = default;

int MMDNSSpeedChecker::checkSpeed(const std::string& ip, int port) {
    // 优先使用Socket连接测速
    int socketSpeed = checkSpeedWithSocket(ip, port);
    if (socketSpeed >= 0) {
        return socketSpeed;
    }
    
    // Socket失败，尝试Ping
    return checkSpeedWithPing(ip);
}

int MMDNSSpeedChecker::checkSpeedWithSocket(const std::string& ip, int port) {
    MMDNSSocket socket;
    
    auto startTime = std::chrono::steady_clock::now();
    bool connected = socket.connect(ip, port, timeout_);
    auto endTime = std::chrono::steady_clock::now();
    
    if (!connected) {
        return -1;
    }
    
    int latency = std::chrono::duration_cast<std::chrono::milliseconds>(
        endTime - startTime).count();
    
    socket.close();
    return latency;
}

int MMDNSSpeedChecker::checkSpeedWithPing(const std::string& ip) {
    PingResult result = pinger_->ping(ip, 3, timeout_);
    return result.avgRtt;
}

void MMDNSSpeedChecker::checkMultiple(std::vector<std::shared_ptr<MMDNSIPModel>>& ipList, int port) {
    if (ipList.empty()) {
        return;
    }
    
    Logger::log(LogLevel::INFO, "MMDNSSpeedChecker", 
        "开始检测 " + std::to_string(ipList.size()) + " 个IP的速度");
    
    // 使用并发测速
    std::vector<std::future<void>> futures;
    
    for (auto& ipModel : ipList) {
        futures.push_back(std::async(std::launch::async, [this, ipModel, port]() {
            int speed = checkSpeed(ipModel->getIP(), port);
            ipModel->setSpeed(speed);
            
            if (speed >= 0) {
                Logger::log(LogLevel::DEBUG, "MMDNSSpeedChecker",
                    "IP: " + ipModel->getIP() + " 速度: " + std::to_string(speed) + "ms");
            }
        }));
    }
    
    // 等待所有测速完成
    for (auto& future : futures) {
        future.wait();
    }
    
    // 按速度排序（速度快的在前）
    std::sort(ipList.begin(), ipList.end(),
        [](const std::shared_ptr<MMDNSIPModel>& a,
           const std::shared_ptr<MMDNSIPModel>& b) {
            // 无效的IP排在后面
            if (!a->isValid()) return false;
            if (!b->isValid()) return true;
            
            // 未测速的IP排在后面
            int speedA = a->getSpeed();
            int speedB = b->getSpeed();
            if (speedA < 0) return false;
            if (speedB < 0) return true;
            
            // 按速度从快到慢排序
            return speedA < speedB;
        });
    
    if (!ipList.empty() && ipList[0]->getSpeed() >= 0) {
        Logger::log(LogLevel::INFO, "MMDNSSpeedChecker",
            "最快IP: " + ipList[0]->getIP() + " (" + std::to_string(ipList[0]->getSpeed()) + "ms)");
    }
}

// ==================== MMIMSpeedChecker ====================

MMIMSpeedChecker::MMIMSpeedChecker() : imServerPort_(0) {
}

MMIMSpeedChecker::~MMIMSpeedChecker() = default;

int MMIMSpeedChecker::checkSpeed(const std::string& ip, int port) {
    // IM服务器特定的测速逻辑
    return performIMHandshake(ip, port > 0 ? port : imServerPort_);
}

int MMIMSpeedChecker::performIMHandshake(const std::string& ip, int port) {
    MMDNSSocket socket;
    
    auto startTime = std::chrono::steady_clock::now();
    
    // 连接服务器
    if (!socket.connect(ip, port, timeout_)) {
        return -1;
    }
    
    // 这里可以添加IM特定的握手协议
    // 例如发送特定的握手包并等待响应
    
    // 简化实现：只测量连接时间
    auto endTime = std::chrono::steady_clock::now();
    int latency = std::chrono::duration_cast<std::chrono::milliseconds>(
        endTime - startTime).count();
    
    socket.close();
    return latency;
}

} // namespace mmdns