#pragma once

#include "MMDNSCommon.h"
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

namespace mmdns {

class MMDNSSocket {
public:
    MMDNSSocket();
    ~MMDNSSocket();
    
    // 连接操作
    bool connect(const std::string& ip, int port, int timeoutMs = Constants::DEFAULT_TIMEOUT_MS);
    void close();
    
    // 数据收发
    ssize_t send(const void* data, size_t len);
    ssize_t recv(void* buffer, size_t len);
    
    // 状态查询
    bool isConnected() const { return connected_; }
    int getSocketFd() const { return socketFd_; }
    std::string getRemoteIP() const { return remoteIP_; }
    int getRemotePort() const { return remotePort_; }
    
    // 设置选项
    bool setNonBlocking(bool nonBlocking);
    bool setReuseAddr(bool reuse);
    bool setSendTimeout(int timeoutMs);
    bool setRecvTimeout(int timeoutMs);
    
private:
    int socketFd_;
    bool connected_;
    std::string remoteIP_;
    int remotePort_;
    struct sockaddr_in serverAddr_;
    
    bool createSocket();
    bool setSocketTimeout(int optname, int timeoutMs);
};

} // namespace mmdns