//
// Created by 64860 on 2025/5/6.
//

#include "p2p_node.h"
#include "message_protocol.h"
#include <unistd.h>
#include <cstring>
#include <android/log.h>
#include "../utils/log_utils.h"

#define TAG "p2p_node.h"

namespace p2p {

    P2PNode::P2PNode(int tcp_port, int udp_port)
            : _tcp_prot(tcp_port), _udp_prot(udp_port),
            _tcp_socket(-1), _udp_socket(-1), _running(false) {
    }

    P2PNode::~P2PNode() {
        stop();
    }

    void P2PNode::setDataReceivedCallback(const p2p::P2PNode::DataReceivedCallback &callback) {
        _data_received_callback = callback;
    }

    void P2PNode::setPeerConnectedCallback(const p2p::P2PNode::PeerConnectedCallback &callback) {
        _peer_connected_callback = callback;
    }

    void P2PNode::setPeerDisconnectedCallback(const p2p::P2PNode::PeerDisconnectedCallback &callback) {
        _peer_disconnected_callback = callback;
    }

    bool P2PNode::start() {
        _tcp_socket = NetworkUtils::createTCPSocket();
        if (_tcp_socket < 0) {
            LOGE(TAG, "p2p socket < 0");
            return false;
        }
        LOGI(TAG, "tcp socket:= %d", _tcp_socket);
        if (!NetworkUtils::setSocketReusable(_tcp_socket)) {
            LOGE(TAG, "tcp socket setSocketReusable is false");
            close(_tcp_socket);
            return false;
        }

        if (!NetworkUtils::bindSocket(_tcp_socket, "0.0.0.0", _tcp_prot)) {
            LOGE(TAG, "tcp socket bindSocket is false");
            close(_tcp_socket);
            return false;
        }

        if (listen(_tcp_socket, SOMAXCONN) < 0) {
            LOGE(TAG, "tcp socket listen < 0");
            close(_tcp_socket);
            return false;
        }

        _udp_socket = NetworkUtils::createUDPSocket();
        if (_udp_socket < 0) {
            close(_tcp_socket);
            LOGE(TAG, "udp socket create < 0");
            return false;
        }
        LOGI(TAG, "udp socket := %d", _udp_socket);
        if (!NetworkUtils::setSocketReusable(_udp_socket)) {
            close(_tcp_socket);
            close(_udp_socket);
            LOGE(TAG, "udp socket setSocketReusable is false");
            return false;
        }

        if (!NetworkUtils::bindSocket(_udp_socket, "0.0.0.0", _udp_prot)) {
            close(_tcp_socket);
            close(_udp_socket);
            LOGE(TAG, "udp socket bindSocket is false");
            return false;
        }

        int broadcast = 1;
        if (setsockopt(_udp_socket, SOL_SOCKET, SO_BROADCAST,
                       &broadcast, sizeof(broadcast)) < 0) {
            close(_tcp_socket);
            close(_udp_socket);
            LOGE(TAG, "udp socket setsockopt < 0");
            return false;
        }

        _event_loop = make_unique<EventLoop>();
        if (!_event_loop->init()) {
            close(_tcp_socket);
            close(_udp_socket);
            LOGE(TAG, "event loop init fail");
            return false;
        }

        if (!_event_loop->addEvent(_tcp_socket, EPOLLIN, bind(&P2PNode::handleTCPAccept, this, placeholders::_1, placeholders::_2))) {
            close(_tcp_socket);
            close(_udp_socket);
            LOGE(TAG, "event loop add tcp socket fail");
            return false;
        }

        if (!_event_loop->addEvent(_udp_socket, EPOLLIN, bind(&P2PNode::handleUDPRead, this, placeholders::_1, placeholders::_2))) {
            close(_tcp_socket);
            close(_udp_socket);
            LOGE(TAG, "event loop add udp socket fail");
            return false;
        }

        _running = true;

        thread([this]() {
            _event_loop->run();
        }).detach();

        //启动心跳检测线程
        thread([this]() {
            while (_running) {
                time_t now = time(nullptr);
                for (auto it = _peers.begin(); it != _peers.end();) {
                    if (now - it->second.last_active > 30) { //30秒超时断开连接
                        close(it->second.fd);
                        if (_peer_disconnected_callback) {
                            _peer_disconnected_callback(it->first);
                        }
                        it = _peers.erase(it);
                    } else {
                        //发送心跳包
                        auto heartbeat = MessageProtocol::serializeHeartbeatMessage();
                        send(it->second.fd, heartbeat.data(), heartbeat.size(), 0);
                        ++it;
                    }
                }
                this_thread::sleep_for(chrono::seconds(10));
            }
        }).detach();

        //启动服务发现
        discoverPeers();

        return true;
    }


    void P2PNode::stop() {
        if (!_running) {
            return;
        }

        _running = false;
        _event_loop->stop();

        for (auto& peer : _peers) {
            LOGE(TAG, "stop peer");
            close(peer.second.fd);
        }

        _peers.clear();

        if (_tcp_socket >= 0) {
            close(_tcp_socket);
            _tcp_socket = -1;
        }

        if (_udp_socket >= 0) {
            close(_udp_socket);
            _udp_socket = -1;
        }
    }


    void P2PNode::handleUDPRead(int fd, uint32_t events) {

        char buffer[1024];
        sockaddr_in address;
        socklen_t address_len = sizeof(address);

        ssize_t len = recvfrom(fd, buffer, sizeof(buffer), 0, (sockaddr*)&address, &address_len);

        if (len <= 0) {
            return;
        }

        vector<uint8_t> data(buffer, buffer + len);
        string peer_ip = inet_ntoa(address.sin_addr);
        int peer_port = ntohs(address.sin_port);

        //发现新消息
        string discovered_ip;
        int discovered_port;
        if (MessageProtocol::parseDiscoveryMessage(data, discovered_ip, discovered_port)) {
            if (discovered_ip != NetworkUtils::getLocalIP()) {
                connectToPeer(discovered_ip, discovered_port);
            }
        }
    }


    void P2PNode::handleTCPAccept(int fd, uint32_t events) {
        sockaddr_in address;
        socklen_t address_len = sizeof(address);

        int client_fd = accept(fd, (sockaddr*)&address, &address_len);
        if (client_fd < 0) {
            return;
        }

        string peer_ip = inet_ntoa(address.sin_addr);

        if (!NetworkUtils::setSocketNonBlocking(client_fd)) {
            close(client_fd);
            return;
        }

        //添加到对等节点列表
        PeerInfo peer;
        peer.ip = peer_ip;
        peer.port = 0;
        peer.fd = client_fd;
        peer.last_active = time(nullptr);

        _peers[peer_ip] = peer;
        LOGE(TAG, "handle tcp accept ip:= %s", peer_ip.c_str());

        _event_loop->addEvent(client_fd, EPOLLIN, bind(&P2PNode::handleTCPRead, this, placeholders::_1, placeholders::_2));

        if (_peer_connected_callback) {
            _peer_connected_callback(peer_ip);
        }
    }

    void P2PNode::requestConnectToPeer(const std::string &ip, int port) {
        LOGE(TAG, "request connect to peer ip:= %s:%d    localIP:= %s", ip.c_str(), port, NetworkUtils::getLocalIP().c_str());
        if (ip != NetworkUtils::getLocalIP()) {
            connectToPeer(ip, port);
        }
    }

    void P2PNode::handleTCPRead(int fd, uint32_t events) {
        char buffer[4096];
        ssize_t len = recv(fd, buffer, sizeof(buffer), 0);

        if (len <= 0) {
            //连接关闭或错误
            for (auto it = _peers.begin(); it != _peers.end(); ++it) {
                if (it->second.fd == fd) {
                    string peer_ip = it->first;
                    _peers.erase(it);

                    if (_peer_disconnected_callback) {
                        _peer_disconnected_callback(peer_ip);
                    }
                    break;
                }
            }
            close(fd);
            _event_loop->delEvent(fd);
            return;
        }


        //更新活跃时间
        for (auto& peer : _peers) {
            if (peer.second.fd == fd) {
                peer.second.last_active = time(nullptr);

                vector<uint8_t> data(buffer, buffer + len);
                string content;

                if (MessageProtocol::isHeartbeatMessage(data)) {
                    //心跳包
                } else if (MessageProtocol::parseDataMessage(data, content)) {
                    if (_data_received_callback) {
                        _data_received_callback(peer.first, content);
                    }
                }
                break;
            }
        }
    }

    void P2PNode::discoverPeers() {
        thread([this]() {
            while (_running) {
                auto local_ips = NetworkUtils::getAllIPs();
                if (local_ips.empty()) {
                    this_thread::sleep_for(chrono::seconds(1));
                    continue;
                }

//                for (auto ip: local_ips) {
//                    LOGE(TAG, "discoverPeers ip:= %s", ip.c_str());
//                }

                auto discovery_msg = MessageProtocol::serializeDiscoveryMessage(local_ips[0], _tcp_prot);

                sockaddr_in address;
                memset(&address, 0, sizeof(address));
                address.sin_family = AF_INET;
                address.sin_port = htons(_udp_prot);
                address.sin_addr.s_addr = htonl(INADDR_BROADCAST);

                sendto(_udp_socket, discovery_msg.data(), discovery_msg.size(), 0, (sockaddr*)&address,
                       sizeof(address));

                this_thread::sleep_for(chrono::seconds(5));
            }
        }).detach();
    }


    void P2PNode::connectToPeer(const std::string &ip, int port) {
        LOGE(TAG, "connectToPeer is run");
        if (_peers.find(ip) != _peers.end()) {
            return;//已连接
        }

        LOGE(TAG, "connectToPeer is run 12321");
        int sockfd = NetworkUtils::createTCPSocket();
        if (sockfd < 0) {
            LOGE(TAG, "connectToPeer create tcp socket failed");
            return;
        }

        LOGE(TAG, "connectToPeer is run ------>?111");
        sockaddr_in addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_port = htons(port);
        inet_pton(AF_INET, ip.c_str(), &addr.sin_addr);

        LOGE(TAG, "connectToPeer is run ------>?2222");
        if (connect(sockfd, (sockaddr*)&addr, sizeof(addr)) < 0) {
            LOGE(TAG, "connectToPeer tcp connect failed");
            close(sockfd);
            return;
        }

        LOGE(TAG, "connectToPeer is run ------>?3333");
        //设置非阻塞模式
        if (!NetworkUtils::setSocketNonBlocking(sockfd)) {
            LOGE(TAG, "connectToPeer tcp setSocketNonBlocking is false");
            close(sockfd);
            return;
        }

        LOGE(TAG, "connectToPeer is run ------>?4444");

        PeerInfo peer;
        peer.ip = ip;
        peer.port = port;
        peer.fd = sockfd;
        peer.last_active = time(nullptr);
        LOGE(TAG, "connect to peer ip:= %s", ip.c_str());

        _peers[ip] = peer;
        LOGE(TAG, "connectToPeer is run ------>?5555");
        _event_loop->addEvent(sockfd, EPOLLIN, bind(&P2PNode::handleTCPRead, this, placeholders::_1, placeholders::_2));

        LOGE(TAG, "connectToPeer is run ------>?66666");
        if (_peer_connected_callback) {
            _peer_connected_callback(ip);
        }
    }


    void P2PNode::sendData(const std::string &peer_ip, const std::string &data) {
        auto it = _peers.find(peer_ip);
        if (it == _peers.end()) {
            return;
        }
        auto msg = MessageProtocol::serializeDataMessage(data);
        send(it->second.fd, msg.data(), msg.size(), 0);
    }


    void P2PNode::broadcastData(const std::string &data) {
        auto msg = MessageProtocol::serializeDataMessage(data);

        for (auto& peer : _peers) {
            send(peer.second.fd, msg.data(), msg.size(), 0);
        }
    }
}