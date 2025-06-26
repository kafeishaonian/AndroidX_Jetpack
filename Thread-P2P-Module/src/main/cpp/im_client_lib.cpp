#include <jni.h>
#include "im/im_client.h"
#include <android/log.h>

#define TAG "IMClientJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// JavaVM实例
JavaVM* g_jvm = nullptr;
// Java回调类全局引用
jobject g_callbackObj = nullptr;
jmethodID g_onStateChanged = nullptr;
jmethodID g_onMessageReceived = nullptr;
jmethodID g_onErrorOccurred = nullptr;

// 转换C++状态为Java状态
jint toJavaState(IMClient::State state) {
    switch (state) {
        case IMClient::DISCONNECTED: return 0;
        case IMClient::CONNECTING: return 1;
        case IMClient::CONNECTED: return 2;
        case IMClient::RECONNECTING: return 3;
        default: return 0;
    }
}

// 转换C++错误码为Java错误码
jint toJavaErrorCode(IMClient::ErrorCode code) {
    switch (code) {
        case IMClient::NETWORK_ERROR: return 0;
        case IMClient::TIMEOUT: return 1;
        case IMClient::SERVER_ERROR: return 2;
        default: return 0;
    }
}

// C++回调函数实现
void onStateChanged(IMClient::State state) {
    JNIEnv* env;
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return;
    }

    if (g_callbackObj && g_onStateChanged) {
        jint javaState = toJavaState(state);
        env->CallVoidMethod(g_callbackObj, g_onStateChanged, javaState);
    }
}

void onMessageReceived(const std::string& rawData) {
    JNIEnv* env;
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return;
    }

    if (g_callbackObj && g_onMessageReceived) {
        jstring jData = env->NewStringUTF(rawData.c_str());
        env->CallVoidMethod(g_callbackObj, g_onMessageReceived, jData);
        env->DeleteLocalRef(jData);
    }
}

void onErrorOccurred(IMClient::ErrorCode code, const std::string& msg) {
    JNIEnv* env;
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return;
    }

    if (g_callbackObj && g_onErrorOccurred) {
        jint javaCode = toJavaErrorCode(code);
        jstring jMsg = env->NewStringUTF(msg.c_str());

        env->CallVoidMethod(g_callbackObj, g_onErrorOccurred, javaCode, jMsg);

        env->DeleteLocalRef(jMsg);
    }
}

// JNI方法实现
extern "C" JNIEXPORT void JNICALL
Java_com_example_imclient_IMClient_init(JNIEnv* env, jobject /* this */) {
    // 初始化客户端
    IMClient::getInstance();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_imclient_IMClient_connect(
        JNIEnv* env,
        jobject /* this */,
        jstring jHost,
        jint jPort) {

    const char* host = env->GetStringUTFChars(jHost, nullptr);
    int port = static_cast<int>(jPort);

    IMClient::getInstance().connect(host, port);

    env->ReleaseStringUTFChars(jHost, host);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_imclient_IMClient_disconnect(JNIEnv* env, jobject /* this */) {
    IMClient::getInstance().disconnect();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_imclient_IMClient_sendRawMessage(
        JNIEnv* env,
        jobject /* this */,
        jstring jMessage) {

    const char* message = env->GetStringUTFChars(jMessage, nullptr);

    IMClient::getInstance().sendRawMessage(message);

    env->ReleaseStringUTFChars(jMessage, message);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_imclient_IMClient_registerCallbacks(
        JNIEnv* env,
        jobject thiz,
        jobject callback) {

    // 设置全局回调对象
    if (g_callbackObj) {
        env->DeleteGlobalRef(g_callbackObj);
        g_callbackObj = nullptr;
    }

    g_callbackObj = env->NewGlobalRef(callback);

    // 获取回调方法ID
    jclass clazz = env->GetObjectClass(callback);
    g_onStateChanged = env->GetMethodID(clazz, "onStateChanged", "(I)V");
    g_onMessageReceived = env->GetMethodID(clazz, "onMessageReceived", "(Ljava/lang/String;)V");
    g_onErrorOccurred = env->GetMethodID(clazz, "onErrorOccurred", "(ILjava/lang/String;)V");

    // 设置C++回调
    IMClient::getInstance().setStateCallback(onStateChanged);
    IMClient::getInstance().setMessageCallback(onMessageReceived);
    IMClient::getInstance().setErrorCallback(onErrorOccurred);
}

// JNI_OnLoad
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    g_jvm = vm;
    return JNI_VERSION_1_6;
}