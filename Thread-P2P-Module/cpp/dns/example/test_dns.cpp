#include "../include/MMDNSEntrance.h"
#include <iostream>
#include <thread>
#include <chrono>

using namespace mmdns;

int main() {
    std::cout << "====== MMDNS 测试程序 ======\n" << std::endl;
    
    // 设置日志级别
    Logger::setLevel(LogLevel::DEBUG);
    
    // 获取DNS服务实例
    auto dnsBase = MMDNSEntranceImpl::getInstance("test");
    auto dns = std::dynamic_pointer_cast<MMDNSEntranceImpl>(dnsBase);
    
    if (!dns) {
        std::cerr << "Failed to get DNS instance" << std::endl;
        return -1;
    }
    
    // 配置DNS服务
    dns->enableSystemDNS(true);
    dns->enableHttpDNS(false);  // HTTP DNS需要额外的HTTP客户端库
    dns->enableLocalCache(true);
    dns->setThreadCount(4);
    
    // 初始化
    std::cout << "初始化DNS服务..." << std::endl;
    dns->init();
    
    // 等待初始化完成
    std::this_thread::sleep_for(std::chrono::seconds(1));
    
    // 测试同步解析
    std::cout << "\n>>> 测试同步解析:" << std::endl;
    std::vector<std::string> testHosts = {
        "www.google.com",
        "www.baidu.com",
        "www.github.com"
    };
    
    for (const auto& host : testHosts) {
        std::cout << "\n解析: " << host << std::endl;
        std::string ip = dns->resolveHost(host);
        if (!ip.empty()) {
            std::cout << "  最优IP: " << ip << std::endl;
            
            // 获取所有IP
            auto allIPs = dns->getAllIPs(host);
            std::cout << "  所有IP (" << allIPs.size() << "个): ";
            for (const auto& i : allIPs) {
                std::cout << i << " ";
            }
            std::cout << std::endl;
        } else {
            std::cout << "  解析失败!" << std::endl;
        }
    }
    
    // 测试异步解析
    std::cout << "\n>>> 测试异步解析:" << std::endl;
    std::atomic<int> asyncCount(0);
    
    dns->resolveHostAsync("www.example.com", 
        [&asyncCount](auto host, bool success, auto oldHost) {
            if (success && host) {
                std::cout << "异步解析成功: " << host->getHostname() 
                         << " -> " << host->getBestIPString() << std::endl;
            } else {
                std::cout << "异步解析失败" << std::endl;
            }
            asyncCount++;
        });
    
    // 等待异步结果
    while (asyncCount.load() == 0) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    
    // 获取统计信息
    std::cout << "\n>>> DNS服务统计:" << std::endl;
    auto stats = dns->getStats();
    std::cout << "  总请求数: " << stats.totalRequests << std::endl;
    std::cout << "  成功数: " << stats.successRequests << std::endl;
    std::cout << "  失败数: " << stats.failedRequests << std::endl;
    std::cout << "  缓存命中: " << stats.cachedRequests << std::endl;
    std::cout << "  队列大小: " << stats.queueSize << std::endl;
    std::cout << "  工作线程: " << stats.threadCount << std::endl;
    
    // 清理
    std::cout << "\n清理资源..." << std::endl;
    dns->clear();
    
    std::cout << "\n====== 测试完成 ======" << std::endl;
    
    return 0;
}