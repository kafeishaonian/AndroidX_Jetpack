#include "../include/MMDNSSocket.h"
#include "../include/MMDNSCommon.h"
#include <unistd.h>
#include <fcntl.h>
#include <sys/select.h>
#include <cstring>
#include <chrono>

namespace mmdns {

MMDNSSocket::MMDNSSocket()
    : socketFd_(-1), connected_(false), remotePort_(0) {
    memset(&serverAddr_, 0, sizeof(serverAddr_));
}

MMDNSSocket::~MMDNSSocket() {
    close();
}

bool MMDNSSocket::createSocket() {
    socketFd_ = socket(AF_INET, SOCK_STREAM, 0);
    if (socketFd_ < 0) {
        Logger::log(LogLevel::ERROR, "MMDNSSocket", "Failed to create socket");
        return false;
    }
    return true;
}

bool MMDNSSocket::connect(const std::string& ip, int port, int timeoutMs) {
    if (connected_) {
        close();
    }
    
    if (!createSocket()) {
        return false;
    }
    
    // 设置非阻塞模式
    setNonBlocking(true);
    
    // 设置服务器地址
    serverAddr_.sin_family = AF_INET;
    serverAddr_.sin_port = htons(port);
    if (inet_pton(AF_INET, ip.c_str(), &serverAddr_.sin_addr) <= 0) {
        Logger::log(LogLevel::ERROR, "MMDNSSocket", "Invalid IP address: " + ip);
        close();
        return false;
    }
    
    // 开始连接
    auto startTime = std::chrono::steady_clock::now();
    int result = ::connect(socketFd_, (struct sockaddr*)&serverAddr_, sizeof(serverAddr_));
    
    if (result < 0) {
        if (errno != EINPROGRESS) {
            Logger::log(LogLevel::ERROR, "MMDNSSocket", "Connect failed immediately");
            close();
            return false;
        }
        
        // 等待连接完成
        fd_set writeSet;
        FD_ZERO(&writeSet);
        FD_SET(socketFd_, &writeSet);
        
        struct timeval timeout;
        timeout.tv_sec = timeoutMs / 1000;
        timeout.tv_usec = (timeoutMs % 1000) * 1000;
        
        result = select(socketFd_ + 1, nullptr, &writeSet, nullptr, &timeout);
        if (result <= 0) {
            Logger::log(LogLevel::ERROR, "MMDNSSocket", "Connect timeout");
            close();
            return false;
        }
        
        // 检查连接是否成功
        int error = 0;
        socklen_t len = sizeof(error);
        if (getsockopt(socketFd_, SOL_SOCKET, SO_ERROR, &error, &len) < 0 || error != 0) {
            Logger::log(LogLevel::ERROR, "MMDNSSocket", "Connect failed: " + std::string(strerror(error)));
            close();
            return false;
        }
    }
    
    // 恢复阻塞模式
    setNonBlocking(false);
    
    connected_ = true;
    remoteIP_ = ip;
    remotePort_ = port;
    
    auto endTime = std::chrono::steady_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime).count();
    Logger::log(LogLevel::DEBUG, "MMDNSSocket", 
        "Connected to " + ip + ":" + std::to_string(port) + " in " + std::to_string(duration) + "ms");
    
    return true;
}

void MMDNSSocket::close() {
    if (socketFd_ >= 0) {
        ::close(socketFd_);
        socketFd_ = -1;
    }
    connected_ = false;
    remoteIP_.clear();
    remotePort_ = 0;
}

ssize_t MMDNSSocket::send(const void* data, size_t len) {
    if (!connected_ || socketFd_ < 0) {
        return -1;
    }
    return ::send(socketFd_, data, len, 0);
}

ssize_t MMDNSSocket::recv(void* buffer, size_t len) {
    if (!connected_ || socketFd_ < 0) {
        return -1;
    }
    return ::recv(socketFd_, buffer, len, 0);
}

bool MMDNSSocket::setNonBlocking(bool nonBlocking) {
    if (socketFd_ < 0) {
        return false;
    }
    
    int flags = fcntl(socketFd_, F_GETFL, 0);
    if (flags < 0) {
        return false;
    }
    
    if (nonBlocking) {
        flags |= O_NONBLOCK;
    } else {
        flags &= ~O_NONBLOCK;
    }
    
    return fcntl(socketFd_, F_SETFL, flags) >= 0;
}

bool MMDNSSocket::setReuseAddr(bool reuse) {
    if (socketFd_ < 0) {
        return false;
    }
    
    int optval = reuse ? 1 : 0;
    return setsockopt(socketFd_, SOL_SOCKET, SO_REUSEADDR, &optval, sizeof(optval)) >= 0;
}

bool MMDNSSocket::setSendTimeout(int timeoutMs) {
    return setSocketTimeout(SO_SNDTIMEO, timeoutMs);
}

bool MMDNSSocket::setRecvTimeout(int timeoutMs) {
    return setSocketTimeout(SO_RCVTIMEO, timeoutMs);
}

bool MMDNSSocket::setSocketTimeout(int optname, int timeoutMs) {
    if (socketFd_ < 0) {
        return false;
    }
    
    struct timeval timeout;
    timeout.tv_sec = timeoutMs / 1000;
    timeout.tv_usec = (timeoutMs % 1000) * 1000;
    
    return setsockopt(socketFd_, SOL_SOCKET, optname, &timeout, sizeof(timeout)) >= 0;
}

} // namespace mmdns