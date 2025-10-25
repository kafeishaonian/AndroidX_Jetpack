#pragma once

#include "MMDNSCommon.h"
#include "MMDNSIPModel.h"
#include <vector>
#include <memory>
#include <string>
#include <functional>

namespace mmdns {

/**
 * 双栈优化器
 * 实现Happy Eyeballs算法(RFC 8305)
 * 智能选择IPv4/IPv6地址
 */
class MMDNSDualStackOptimizer {
public:
    /**
     * 连接测试结果
     */
    struct TestResult {
        std::string ip;
        int rtt;           // 往返时间(毫秒)
        bool success;      // 是否成功
    };
    
    MMDNSDualStackOptimizer();
    ~MMDNSDualStackOptimizer() = default;
    
    /**
     * 选择最佳IP地址（Happy Eyeballs算法）
     * @param ipv4List IPv4地址列表
     * @param ipv6List IPv6地址列表
     * @param port 测试端口
     * @return 最佳IP地址
     */
    std::string selectBestIP(
        const std::vector<std::shared_ptr<MMDNSIPModel>>& ipv4List,
        const std::vector<std::shared_ptr<MMDNSIPModel>>& ipv6List,
        int port = 80);
    
    /**
     * 并行测试连接
     * @param ips IP地址列表
     * @param port 测试端口
     * @param callback 结果回调
     */
    void testConnections(
        const std::vector<std::shared_ptr<MMDNSIPModel>>& ips,
        int port,
        std::function<void(const TestResult&)> callback);
    
    /**
     * 设置IPv6优先
     */
    void setPreferIPv6(bool prefer) { preferIPv6_ = prefer; }
    
    /**
     * 设置连接尝试延迟（毫秒）
     */
    void setConnectionAttemptDelay(int delay) { connectionAttemptDelay_ = delay; }
    
    /**
     * 设置测试超时（毫秒）
     */
    void setTestTimeout(int timeout) { testTimeout_ = timeout; }
    
private:
    bool preferIPv6_;              // 是否优先IPv6
    int connectionAttemptDelay_;   // 连接尝试延迟（毫秒）
    int testTimeout_;              // 测试超时（毫秒）
    
    /**
     * 测试单个IP的连接性
     */
    TestResult testSingleIP(const std::string& ip, int port);
    
    /**
     * 比较两个测试结果
     */
    bool isBetterResult(const TestResult& a, const TestResult& b) const;
};

} // namespace mmdns