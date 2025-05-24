//
// Created by 64860 on 2025/5/8.
//

#ifndef ANDROIDX_JETPACK_P2P_MANAGER_H
#define ANDROIDX_JETPACK_P2P_MANAGER_H

#include <jni.h>
#include <string>
#include "p2p_node.h"

using namespace std;

namespace p2p {
    class P2PManager {

    public:
        static P2PManager &getInstance();

        bool initialize(JNIEnv *env, jobject java_instance, int tcp_port, int udp_port);

        void sendData(const string &peer_ip, const string &data);

        void requestConnectedToPeer(const string& peer_ip, int port);

        void terminate(JNIEnv *env);

        P2PManager(const P2PManager &) = delete;

        void operator=(const P2PManager &) = delete;


    private:
        P2PManager();

        ~P2PManager();

        void bindCallbacks() {
            p2p_node_->setDataReceivedCallback(
                    [this](const std::string &ip, const string &data) {
                        this->onDataReceived(ip, data);
                    });
            p2p_node_->setPeerConnectedCallback(
                    [this](const std::string &ip) {
                        this->onPeerConnected(ip);
                    });
            p2p_node_->setPeerDisconnectedCallback(
                    [this](const string &ip) {
                        this->onPeerDisconnected(ip);
                    });
        }

        // 实际回调处理
        void onDataReceived(const std::string &peer_ip, const std::string &data);

        void onPeerConnected(const std::string &peer_ip);

        void onPeerDisconnected(const string &peer_ip);

        P2PNode *p2p_node_ = nullptr;
        JavaVM *java_vm_ = nullptr;
        jobject java_instance_ = nullptr;
    };
}


#endif //ANDROIDX_JETPACK_P2P_MANAGER_H
