//
// Created by 64860 on 2025/5/6.
//

#ifndef ANDROIDX_JETPACK_P2P_NODE_H
#define ANDROIDX_JETPACK_P2P_NODE_H

#include <string>
#include <memory>
#include <unordered_map>
#include <atomic>
#include "event_loop.h"
#include "network_utils.h"
#include "../thread_pool/thread_pool.h"

using namespace std;

namespace p2p {

    class P2PNode {

    public:
        struct PeerInfo {
            string ip;
            int port;
            int fd;
            time_t last_active;
        };

        P2PNode(int tcp_port, int udp_port);
        ~P2PNode();

        bool start();
        void stop();

        void sendData(const string& peer_ip, const string& data);
        void broadcastData(const string& data);
        void requestConnectToPeer(const string& ip, int port);

        //回调函数
        using DataReceivedCallback = function<void(const string& peer_ip, const string& data)>;
        using PeerConnectedCallback = function<void(const string& peer_ip)>;
        using PeerDisconnectedCallback = function<void(const string& peer_ip)>;

        void setDataReceivedCallback(const DataReceivedCallback& callback);
        void setPeerConnectedCallback(const PeerConnectedCallback& callback);
        void setPeerDisconnectedCallback(const PeerDisconnectedCallback& callback);


    private:
        void handleUDPRead(int fd, uint32_t events);
        void handleTCPAccept(int fd, uint32_t events);
        void handleTCPRead(int fd, uint32_t events);

        void discoverPeers();
        void connectToPeer(const string& ip, int port);
        void disconnectPeer(const string& ip);


        int _tcp_prot;
        int _udp_prot;
        int _tcp_socket;
        int _udp_socket;
        atomic<bool> _running;

        unique_ptr<EventLoop> _event_loop;
        unordered_map<string, PeerInfo> _peers;

        DataReceivedCallback _data_received_callback;
        PeerConnectedCallback _peer_connected_callback;
        PeerDisconnectedCallback _peer_disconnected_callback;
    };
}

#endif //ANDROIDX_JETPACK_P2P_NODE_H
