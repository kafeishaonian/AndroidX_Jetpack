//
// Created by 魏红明 on 2025/6/26.
//

#include "im_client.h"

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>
#include <cstring>
#include <system_error>
#include <iostream>


// 最大事件数
const int MAX_EVENTS = 16;

IMClient &IMClient::getInstance() {
    static IMClient instance;
    return instance;
}

IMClient::IMClient()
        : _current_state(DISCONNECTED),
          _running(false),
          _sockfd(-1),
          _port(0) {

    //初始化接受缓冲区
    memset(_recv_buffer, 0, BUFFER_SIZE);
}

IMClient::~IMClient() {
    disconnect();
}

bool IMClient::connect(const std::string &host, int port) {
    if (_current_state != DISCONNECTED) {
        return false;
    }

    _host = host;
    _port = port;

    //创建socket
    if (!createSocket()) {
        handleError(NETWORK_ERROR, "Socket creation failed");
        return false;
    }

    //设置非阻塞
    if (!setupSocket()) {
        close(_sockfd);
        handleError(NETWORK_ERROR, "Socket setup failed");
        return false;
    }

    //尝试连接
    if (!connectToServer(host, port)) {
        close(_sockfd);
        handleError(NETWORK_ERROR, "Connection failed");
        return false;
    }

    changeState(CONNECTING);
    startNetworkThread();
    return true;
}


void IMClient::disconnect() {
    if (!_running) {
        return;
    }

    _running = false;
    if (_network_thread.joinable()) {
        _network_thread.join();
    }

    if (_sockfd != -1) {
        close(_sockfd);
        _sockfd = -1;
    }

    //晴空发送对列
    lock_guard<mutex> lock(_queue_mutex);
    while (!_send_queue.empty()) {
        _send_queue.pop();
    }

    changeState(DISCONNECTED);
}

bool IMClient::sendRawMessage(const std::string &message) {
    if (_current_state != CONNECTED) {
        return false;
    }

    {
        lock_guard<mutex> lock(_queue_mutex);
        _send_queue.push(message);
    }

    //唤醒发送线程
    _queue_cv.notify_one();
    return true;
}


void IMClient::networkThreadFunc() {
    _running = true;
    changeState(CONNECTING);

    int epoll_fd = epoll_create1(0);
    if (epoll_fd < 0) {
        handleError(NETWORK_ERROR, "epoll_create1 failed");
        return;
    }

    // 添加socket到epoll
    struct epoll_event event;
    memset(&event, 0, sizeof(event));
    event.events = EPOLLIN | EPOLLOUT | EPOLLET | EPOLLRDHUP;
    event.data.fd = _sockfd;

    if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, _sockfd, &event) < 0) {
        close(epoll_fd);
        handleError(NETWORK_ERROR, "Epoll control railed");
        return;
    }

    struct epoll_event events[MAX_EVENTS];
    while (_running) {
        int num_events = epoll_wait(epoll_fd, events, MAX_EVENTS, 500);
        if (!_running) {
            break;
        }

        if (num_events < 0) {
            if (errno != EINTR) {
                handleError(NETWORK_ERROR, "Epoll wait error");
                break;
            }
            continue;
        }

        for (int i = 0; i < num_events; ++i) {
            if (events[i].data.fd != _sockfd) {
                continue;
            }

            if (events[i].events & EPOLLRDHUP) {
                // 连接断开
                handleError(NETWORK_ERROR, "Server closed connection");
                break;
            } else if (events[i].events & (EPOLLERR | EPOLLHUP)) {
                // 错误或挂起
                handleError(NETWORK_ERROR, "Socket error");
                break;
            } else {
                // 可写事件 - 连接成功或可发送数据
                if (events[i].events & EPOLLOUT) {
                    if (_current_state == CONNECTING) {
                        changeState(CONNECTED);
                    }
                    processSendQueue();
                }

                // 可读事件 - 接收数据
                if (events[i].events & EPOLLIN) {
                    if (!readData()) {
                        break;
                    }
                }
            }
        }
    }

    close(epoll_fd);
    if (_sockfd != -1) {
        close(_sockfd);
        _sockfd = -1;
    }
    changeState(DISCONNECTED);
}


bool IMClient::readData() {
    ssize_t bytes_read;

    while ((bytes_read = recv(_sockfd, _recv_buffer, BUFFER_SIZE - 1, 0)) > 0) {
        // 确保以null结尾
        _recv_buffer[bytes_read] = '\0';

        //直接处理原始数据
        processIncomingData(_recv_buffer, bytes_read);

        //重制缓冲区
        memset(_recv_buffer, 0, BUFFER_SIZE);
    }

    if (bytes_read == 0) {
        //连接关闭
        return false;
    } else if (bytes_read < 0) {
        if (errno != EAGAIN && errno != EWOULDBLOCK) {
            handleError(NETWORK_ERROR, "Read error: " + std::string(strerror(errno)));
            return false;
        }
    }

    return true;
}


void IMClient::processIncomingData(const char *data, size_t length) {
    //将接收到的原始数据直接传递给java层
    string rawData(data, length);

    {
        lock_guard<mutex> lock(_callback_mutex);
        if (_message_callback) {
            _message_callback(rawData);
        }
    }
}

// ====== 状态与回调管理 ======

void IMClient::changeState(IMClient::State newState) {
    if (_current_state != newState) {
        _current_state = newState;
        {
            lock_guard<mutex> lock(_callback_mutex);
            if (_state_callback) {
                _state_callback(newState);
            }
        }
    }
}


void IMClient::setStateCallback(IMClient::StateCallback cb) {
    lock_guard<mutex> lock(_callback_mutex);
    _state_callback = cb;
}

void IMClient::setMessageCallback(IMClient::MessageCallback cb) {
    lock_guard<mutex> lock(_callback_mutex);
    _message_callback = cb;
}

void IMClient::setErrorCallback(IMClient::ErrorCallback cb) {
    lock_guard<mutex> lock(_callback_mutex);
    _error_callback = cb;
}


void IMClient::handleError(IMClient::ErrorCode code, const std::string &msg) {
    {
        lock_guard<mutex> lock(_callback_mutex);
        if (_error_callback) {
            _error_callback(code, msg);
        }
    }

    //错误处理逻辑
    switch (code) {
        case NETWORK_ERROR:
            if (_current_state != DISCONNECTED) {
                changeState(RECONNECTING);
            }
            break;

        default:
            break;
    }
}

// ====== Socket辅助方法 ======

bool IMClient::createSocket() {
    _sockfd = socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK, 0);
    return _sockfd >= 0;
}

bool IMClient::setupSocket() {
    int opt = 1;

    if (setsockopt(_sockfd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
        return false;
    }

    // 设置TCP_NODELAY
    if (setsockopt(_sockfd, IPPROTO_TCP, SOL_TCP, &opt, sizeof(opt)) < 0) {
        return false;
    }

    return true;
}


bool IMClient::connectToServer(const std::string &host, int prot) {
    struct sockaddr_in serv_addr;
    memset(&serv_addr, 0, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(prot);

    if (inet_pton(AF_INET, host.c_str(), &serv_addr.sin_addr) <= 0) {
        return false;
    }

    int result = ::connect(_sockfd, (struct sockaddr*)&serv_addr, sizeof(serv_addr));
    if (result < 0) {
        if (errno != EINPROGRESS) {
            return false;
        }
    }

    return true;
}

void IMClient::startNetworkThread() {
    if (_running) return;

    _network_thread = std::thread(&IMClient::networkThreadFunc, this);
}


void IMClient::stopNetworkThread() {
    _running = false;
    if (_network_thread.joinable()) {
        _network_thread.join();
    }
}


void IMClient::processSendQueue() {
    std::unique_lock<std::mutex> lock(_queue_mutex);
    if (_send_queue.empty()) return;

    // 取出第一条消息
    std::string message = std::move(_send_queue.front());
    _send_queue.pop();
    lock.unlock();

    // 发送消息
    size_t totalSent = 0;
    while (totalSent < message.size()) {
        ssize_t sent = send(_sockfd, message.data() + totalSent,
                            message.size() - totalSent, MSG_NOSIGNAL);
        if (sent < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                // 稍后重试
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
                continue;
            }
            handleError(NETWORK_ERROR, "Send error: " + std::string(strerror(errno)));
            return;
        }
        totalSent += sent;
    }

    // 检查是否有更多消息
    lock.lock();
    if (!_send_queue.empty()) {
        _queue_cv.notify_one();
    }
}