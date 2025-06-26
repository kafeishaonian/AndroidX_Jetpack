//
// Created by 魏红明 on 2025/6/26.
//

#ifndef ANDROIDX_JETPACK_IM_CLIENT_H
#define ANDROIDX_JETPACK_IM_CLIENT_H

#include <string>
#include <functional>
#include <atomic>
#include <mutex>
#include <queue>
#include <thread>
#include <sys/epoll.h>
#include <netinet/in.h>

using namespace std;

class IMClient {

public:
    enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    };

    enum ErrorCode {
        NETWORK_ERROR,
        TIMEOUT,
        SERVER_ERROR
    };

    using StateCallback = function<void(State)>;
    using MessageCallback = function<void(const string&)>;
    using ErrorCallback = function<void(ErrorCode, const string&)>;

    static IMClient& getInstance();

    bool connect(const string& host, int port);
    void disconnect();
    bool sendRawMessage(const string& message);
    State getCurrentState() const;

    void setStateCallback(StateCallback cb);
    void setMessageCallback(MessageCallback cb);
    void setErrorCallback(ErrorCallback cb);



private:
    IMClient();
    ~IMClient();

    //禁止拷贝
    IMClient(const IMClient&) = delete;
    IMClient& operator=(const IMClient&) = delete;

    void networkThreadFunc();
    void processIncomingData(const char* data, size_t length);
    bool readData();
    void handleError(ErrorCode code, const string& msg);

    //线程管理
    void startNetworkThread();
    void stopNetworkThread();

    //socket操作
    bool createSocket();
    bool setupSocket();
    bool connectToServer(const string& host, int prot);

    //状态管理
    void changeState(State newState);

    //发送对列处理
    void processSendQueue();


    atomic<State> _current_state;
    atomic<bool> _running;
    int _sockfd;
    thread _network_thread;

    mutex _callback_mutex;
    StateCallback  _state_callback;
    MessageCallback _message_callback;
    ErrorCallback _error_callback;

    mutex _queue_mutex;
    condition_variable _queue_cv;
    queue<string> _send_queue;

    string _host;
    int _port;

    //接受缓冲区
    static const size_t BUFFER_SIZE = 8192;
    char _recv_buffer[BUFFER_SIZE];
};


#endif //ANDROIDX_JETPACK_IM_CLIENT_H
