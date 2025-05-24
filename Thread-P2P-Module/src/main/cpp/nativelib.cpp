#include <jni.h>
#include <string>
#include <android/log.h>
#include <thread>
#include <chrono>
#include "./p2p/p2p_manager.h"
#include "./utils/log_utils.h"

#define TAG "nativelib.cpp"

using namespace std;

static bool initP2P(JNIEnv* env, jobject instance, jint tcp_port, jint udp_port) {
    return p2p::P2PManager::getInstance().initialize(env, instance, tcp_port, udp_port) ? JNI_TRUE : JNI_FALSE;
}

static void sendData(JNIEnv* env, jobject instance, jstring peer_ip, jstring data) {
    const char* ip = env->GetStringUTFChars(peer_ip, nullptr);
    const char* msg = env->GetStringUTFChars(data, nullptr);

    p2p::P2PManager::getInstance().sendData(ip, msg);

    env->ReleaseStringUTFChars(peer_ip, ip);
    env->ReleaseStringUTFChars(data, msg);
}

static void destroyP2p(JNIEnv* env, jobject instance) {
    p2p::P2PManager::getInstance().terminate(env);
}

static void requestConnectToPeer(JNIEnv* env, jobject instance, jstring peer_ip, jint port) {
    const char* ip = env->GetStringUTFChars(peer_ip, nullptr);

    p2p::P2PManager::getInstance().requestConnectedToPeer(ip, port);

    env->ReleaseStringUTFChars(peer_ip, ip);
}


static const JNINativeMethod gMethod[] = {
        {"initP2P", "(II)Z", (void *) initP2P},
        {"sendData", "(Ljava/lang/String;Ljava/lang/String;)V", (void *) sendData},
        {"destroyP2P", "()V", (void *) destroyP2p},
        {"requestConnectToPeer", "(Ljava/lang/String;I)V", (void *) requestConnectToPeer}
};


extern "C"
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;

    if (vm->GetEnv((void **) & env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("com/aj/nativelib/ThreadDemo");
    if (!clazz) {
        return JNI_ERR;
    }

    if (env->RegisterNatives(clazz, gMethod, sizeof(gMethod) / sizeof(gMethod[0]))) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}