#pragma once

#include "MMDNSCommon.h"
#include "MMDNSIPModel.h"
#include "MMDNSSocket.h"
#include "MMPing.h"

namespace mmdns {

// 测速检查器基类
class MMDNSBaseSpeedChecker {
public:
    virtual ~MMDNSBaseSpeedChecker() = default;
    
    // 检测单个IP的速度
    virtual int checkSpeed(const std::string& ip, int port = Constants::HTTP_PORT) = 0;
    
    // 设置参数
    void setTimeout(int timeoutMs) { timeout_ = timeoutMs; }
    void setRetryCount(int count) { retryCount_ = count; }
    
    int getTimeout() const { return timeout_; }
    int getRetryCount() const { return retryCount_; }
    
protected:
    int timeout_;
    int retryCount_;
    
    MMDNSBaseSpeedChecker() 
        : timeout_(Constants::DEFAULT_TIMEOUT_MS),
          retryCount_(Constants::DEFAULT_RETRY_COUNT) {}
};

// 通用测速检查器（使用Socket连接）
class MMDNSSpeedChecker : public MMDNSBaseSpeedChecker {
public:
    MMDNSSpeedChecker();
    ~MMDNSSpeedChecker() override;
    
    // 检测单个IP
    int checkSpeed(const std::string& ip, int port = Constants::HTTP_PORT) override;
    
    // 检测多个IP并排序
    void checkMultiple(std::vector<std::shared_ptr<MMDNSIPModel>>& ipList, int port = Constants::HTTP_PORT);
    
    // 使用Ping测速
    int checkSpeedWithPing(const std::string& ip);
    
private:
    std::shared_ptr<MMPing> pinger_;
    
    // 使用Socket连接测速
    int checkSpeedWithSocket(const std::string& ip, int port);
};

// IM专用测速检查器
class MMIMSpeedChecker : public MMDNSBaseSpeedChecker {
public:
    MMIMSpeedChecker();
    ~MMIMSpeedChecker() override;
    
    // 检测IM服务器速度（可能包含特定协议握手）
    int checkSpeed(const std::string& ip, int port = Constants::HTTP_PORT) override;
    
    // 设置IM服务器特定参数
    void setIMServerPort(int port) { imServerPort_ = port; }
    
private:
    int imServerPort_;
    
    // IM特定的连接测试
    int performIMHandshake(const std::string& ip, int port);
};

} // namespace mmdns