#include "../include/MMDNSDualStackOptimizer.h"
#include "../include/MMDNSSocket.h"
#include <thread>
#include <future>
#include <chrono>
#include <algorithm>

namespace mmdns {

MMDNSDualStackOptimizer::MMDNSDualStackOptimizer()
    : preferIPv6_(false)
    , connectionAttemptDelay_(250)  // 250ms RFC 8305推荐值
    , testTimeout_(2000) {          // 2秒超时
}

MMDNSDualStackOptimizer::TestResult MMDNSDualStackOptimizer::testSingleIP(
    const std::string& ip, int port) {
    
    TestResult result;
    result.ip = ip;
    result.success = false;
    result.rtt = -1;
    
    auto startTime = std::chrono::steady_clock::now();
    
    // 使用MMDNSSocket进行连接测试
    MMDNSSocket socket;
    bool connected = socket.connect(ip, port, testTimeout_);
    
    if (connected) {
        auto endTime = std::chrono::steady_clock::now();
        result.rtt = std::chrono::duration_cast<std::chrono::milliseconds>(
            endTime - startTime).count();
        result.success = true;
        
        Logger::log(LogLevel::DEBUG, "DualStack",
            "Connection test succeeded: " + ip + " RTT=" + std::to_string(result.rtt) + "ms");
    } else {
        Logger::log(LogLevel::DEBUG, "DualStack",
            "Connection test failed: " + ip);
    }
    
    socket.close();
    return result;
}

bool MMDNSDualStackOptimizer::isBetterResult(const TestResult& a, const TestResult& b) const {
    // 如果a失败，b成功，则b更好
    if (!a.success && b.success) return false;
    if (a.success && !b.success) return true;
    
    // 都失败
    if (!a.success && !b.success) return false;
    
    // 都成功，比较RTT
    return a.rtt < b.rtt;
}

std::string MMDNSDualStackOptimizer::selectBestIP(
    const std::vector<std::shared_ptr<MMDNSIPModel>>& ipv4List,
    const std::vector<std::shared_ptr<MMDNSIPModel>>& ipv6List,
    int port) {
    
    // 如果只有一种协议的IP，直接返回
    if (ipv4List.empty() && !ipv6List.empty()) {
        return ipv6List[0]->getIP();
    }
    if (ipv6List.empty() && !ipv4List.empty()) {
        return ipv4List[0]->getIP();
    }
    if (ipv4List.empty() && ipv6List.empty()) {
        return "";
    }
    
    Logger::log(LogLevel::INFO, "DualStack",
        "Happy Eyeballs: IPv4=" + std::to_string(ipv4List.size()) +
        " IPv6=" + std::to_string(ipv6List.size()));
    
    // Happy Eyeballs算法实现
    // 1. 首先尝试首选协议
    // 2. 延迟一段时间后尝试另一个协议
    // 3. 选择最快响应的
    
    std::vector<std::future<TestResult>> futures;
    std::vector<TestResult> results;
    
    // 根据偏好决定首先测试哪个
    const auto& firstList = preferIPv6_ ? ipv6List : ipv4List;
    const auto& secondList = preferIPv6_ ? ipv4List : ipv6List;
    
    // 启动第一个协议的测试
    if (!firstList.empty()) {
        futures.push_back(std::async(std::launch::async,
            &MMDNSDualStackOptimizer::testSingleIP, this,
            firstList[0]->getIP(), port));
    }
    
    // 延迟后启动第二个协议的测试
    std::this_thread::sleep_for(std::chrono::milliseconds(connectionAttemptDelay_));
    
    if (!secondList.empty()) {
        futures.push_back(std::async(std::launch::async,
            &MMDNSDualStackOptimizer::testSingleIP, this,
            secondList[0]->getIP(), port));
    }
    
    // 收集结果
    for (auto& future : futures) {
        try {
            results.push_back(future.get());
        } catch (const std::exception& e) {
            Logger::log(LogLevel::ERROR, "DualStack",
                std::string("Test exception: ") + e.what());
        }
    }
    
    // 选择最佳结果
    if (results.empty()) {
        // 没有成功的测试，返回第一个IP
        return !firstList.empty() ? firstList[0]->getIP() :
               (!secondList.empty() ? secondList[0]->getIP() : "");
    }
    
    // 找到最佳结果
    auto best = results[0];
    for (size_t i = 1; i < results.size(); ++i) {
        if (isBetterResult(results[i], best)) {
            best = results[i];
        }
    }
    
    Logger::log(LogLevel::INFO, "DualStack",
        "Selected best IP: " + best.ip +
        (best.success ? " RTT=" + std::to_string(best.rtt) + "ms" : " (fallback)"));
    
    return best.ip;
}

void MMDNSDualStackOptimizer::testConnections(
    const std::vector<std::shared_ptr<MMDNSIPModel>>& ips,
    int port,
    std::function<void(const TestResult&)> callback) {
    
    if (!callback) {
        Logger::log(LogLevel::WARN, "DualStack", "No callback provided");
        return;
    }
    
    std::vector<std::thread> threads;
    
    for (const auto& ipModel : ips) {
        threads.emplace_back([this, ipModel, port, callback]() {
            auto result = testSingleIP(ipModel->getIP(), port);
            callback(result);
        });
    }
    
    // 等待所有线程完成
    for (auto& thread : threads) {
        if (thread.joinable()) {
            thread.join();
        }
    }
}

} // namespace mmdns