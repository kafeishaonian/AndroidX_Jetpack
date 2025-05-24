//
// Created by 64860 on 2025/5/8.
//

#include "p2p_manager.h"
#include "../utils/log_utils.h"

#define TAG "P2PManager.h"

namespace p2p {

    P2PManager::P2PManager() {

    }

    P2PManager::~P2PManager() {
        if (p2p_node_) {
            delete p2p_node_;
        }
    }

    P2PManager &P2PManager::getInstance() {
        static P2PManager instance;
        return instance;
    }


    bool P2PManager::initialize(JNIEnv *env, jobject java_instance, int tcp_port, int udp_port) {
        if (p2p_node_) {
            return true;
        }

        env->GetJavaVM(&java_vm_);
        java_instance_ = env->NewGlobalRef(java_instance);

        // 初始化P2P节点
        p2p_node_ = new P2PNode(tcp_port, udp_port);
        bindCallbacks();

        if (!p2p_node_->start()) {
            LOGE(TAG, "Failed to start P2P node");
            delete p2p_node_;
            p2p_node_ = nullptr;
            env->DeleteGlobalRef(java_instance_);
            java_instance_ = nullptr;
            return false;
        }


        return true;
    }

    void P2PManager::requestConnectedToPeer(const std::string &peer_ip, int port) {
        if (p2p_node_) {
            p2p_node_->requestConnectToPeer(peer_ip, port);
        }
    }

    void P2PManager::terminate(JNIEnv *env) {
        if (!p2p_node_) return;

        LOGI(TAG, "terminate");
        p2p_node_->stop();
        delete p2p_node_;
        p2p_node_ = nullptr;

        if (java_instance_) {
            env->DeleteGlobalRef(java_instance_);
            java_instance_ = nullptr;
        }
    }

    void P2PManager::sendData(const std::string &peer_ip, const std::string &data) {
        if (p2p_node_) {
            p2p_node_->sendData(peer_ip, data);
        }
    }



    // 实际回调处理
    void P2PManager::onDataReceived(const std::string &peer_ip,
                                    const std::string &data) {
        if (!java_vm_ || !java_instance_) return;

        JNIEnv *env;
        bool attached = false;
        jint result = java_vm_->GetEnv((void **) &env, JNI_VERSION_1_6);

        if (result == JNI_EDETACHED) {
            if (java_vm_->AttachCurrentThread(&env, nullptr) == JNI_OK) {
                attached = true;
            } else {
                LOGE(TAG, "Failed to attach thread");
                return;
            }
        }

        jclass cls = env->GetObjectClass(java_instance_);
        if (cls) {
            jmethodID method = env->GetMethodID(
                    cls,
                    "onDataReceived",
                    "(Ljava/lang/String;Ljava/lang/String;)V"
            );

            if (method) {
                jstring j_peer_ip = env->NewStringUTF(peer_ip.c_str());
                jstring j_data = env->NewStringUTF(data.c_str());

                env->CallVoidMethod(
                        java_instance_,
                        method,
                        j_peer_ip,
                        j_data
                );

                env->DeleteLocalRef(j_peer_ip);
                env->DeleteLocalRef(j_data);
            }
            env->DeleteLocalRef(cls);
        } else {
            LOGE(TAG, "onDataReceived jclass create fail");
        }

        if (attached) {
            java_vm_->DetachCurrentThread();
        }
    }

    void P2PManager::onPeerConnected(const std::string &peer_ip) {
        if (!java_vm_ || !java_instance_) return;

        JNIEnv *env;
        bool attached = false;
        jint result = java_vm_->GetEnv((void **) &env, JNI_VERSION_1_6);

        if (result == JNI_EDETACHED) {
            if (java_vm_->AttachCurrentThread(&env, nullptr) == JNI_OK) {
                attached = true;
            } else {
                LOGE(TAG, "Failed to attach thread");
                return;
            }
        }

        jclass cls = env->GetObjectClass(java_instance_);
        if (cls) {
            jmethodID method = env->GetMethodID(
                    cls,
                    "onPeerConnected",
                    "(Ljava/lang/String;)V"
            );

            if (method) {
                jstring j_peer_ip = env->NewStringUTF(peer_ip.c_str());
                env->CallVoidMethod(java_instance_, method, j_peer_ip);
                env->DeleteLocalRef(j_peer_ip);
            }
            env->DeleteLocalRef(cls);
        } else {
            LOGE(TAG, "onPeerConnected jclass create fail");
        }

        if (attached) {
            java_vm_->DetachCurrentThread();
        }
    }

    void P2PManager::onPeerDisconnected(const std::string &peer_ip) {
        if (!java_vm_ || !java_instance_) {
            return;
        }

        JNIEnv* env;
        bool attached = false;
        jint result = java_vm_->GetEnv((void **)&env, JNI_VERSION_1_6);

        if (result == JNI_EDETACHED) {
            if (java_vm_->AttachCurrentThread(&env, nullptr) == JNI_OK) {
                attached = true;
            } else {
                LOGE(TAG, "Failed to attach thread");
                return;
            }
        }
        jclass cls = env->GetObjectClass(java_instance_);
        if (cls) {
            jmethodID method = env->GetMethodID(cls, "onPeerDisconnected", "(Ljava/lang/String;)V");

            if (method) {
                jstring j_peer_ip = env->NewStringUTF(peer_ip.c_str());
                env->CallVoidMethod(java_instance_, method, j_peer_ip);
                env->DeleteLocalRef(j_peer_ip);
            }
            env->DeleteLocalRef(cls);
        } else {
            LOGE(TAG, "onPeerDisconnected jclass create fail");
        }

        if (attached) {
            java_vm_->DetachCurrentThread();
        }
    }

}