#include <jni.h>
#include <string>
#include "../include/MMDNSEntrance.h"

using namespace mmdns;

extern "C" {

// JNI工具函数
static std::string jstringToString(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string str(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return str;
}

static jstring stringToJstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

// ==================== JNI接口实现 ====================

/**
 * 初始化DNS服务
 * Java: native void nativeInit()
 */
JNIEXPORT void JNICALL
Java_com_mmdns_MMDNSManager_nativeInit(JNIEnv* env, jobject thiz) {
    try {
        auto dns = MMDNSEntranceImpl::getInstance("default");
        auto impl = std::dynamic_pointer_cast<MMDNSEntranceImpl>(dns);
        if (impl) {
            impl->init();
        }
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "JNI", std::string("Init failed: ") + e.what());
    }
}

/**
 * 解析主机名
 * Java: native String nativeResolveHost(String hostname)
 */
JNIEXPORT jstring JNICALL
Java_com_mmdns_MMDNSManager_nativeResolveHost(JNIEnv* env, jobject thiz, jstring jhostname) {
    std::string hostname = jstringToString(env, jhostname);
    
    try {
        auto dns = MMDNSEntranceImpl::getInstance("default");
        std::string ip = dns->resolveHost(hostname);
        return stringToJstring(env, ip);
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "JNI", std::string("Resolve failed: ") + e.what());
        return stringToJstring(env, "");
    }
}

/**
 * 异步解析主机名
 * Java: native void nativeResolveHostAsync(String hostname, DNSCallback callback)
 */
JNIEXPORT void JNICALL
Java_com_mmdns_MMDNSManager_nativeResolveHostAsync(
    JNIEnv* env, jobject thiz, jstring jhostname, jobject jcallback) {
    
    std::string hostname = jstringToString(env, jhostname);
    
    // 保存全局引用
    jobject globalCallback = env->NewGlobalRef(jcallback);
    JavaVM* jvm;
    env->GetJavaVM(&jvm);
    
    try {
        auto dns = MMDNSEntranceImpl::getInstance("default");
        
        dns->resolveHostAsync(hostname, 
            [jvm, globalCallback, hostname](auto host, bool success, auto oldHost) {
                // 获取JNI环境
                JNIEnv* cbEnv;
                bool attached = false;
                
                if (jvm->GetEnv((void**)&cbEnv, JNI_VERSION_1_6) == JNI_EDETACHED) {
                    if (jvm->AttachCurrentThread(&cbEnv, nullptr) == 0) {
                        attached = true;
                    }
                }
                
                if (cbEnv) {
                    // 调用Java回调
                    jclass callbackClass = cbEnv->GetObjectClass(globalCallback);
                    jmethodID onResult = cbEnv->GetMethodID(callbackClass, 
                        "onResult", "(Ljava/lang/String;Z)V");
                    
                    if (onResult) {
                        jstring jip = cbEnv->NewStringUTF(
                            host ? host->getBestIPString().c_str() : "");
                        cbEnv->CallVoidMethod(globalCallback, onResult, jip, success);
                        cbEnv->DeleteLocalRef(jip);
                    }
                    
                    cbEnv->DeleteLocalRef(callbackClass);
                    cbEnv->DeleteGlobalRef(globalCallback);
                    
                    if (attached) {
                        jvm->DetachCurrentThread();
                    }
                }
            });
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "JNI", 
            std::string("Async resolve failed: ") + e.what());
        env->DeleteGlobalRef(globalCallback);
    }
}

/**
 * 获取所有IP地址
 * Java: native String[] nativeGetAllIPs(String hostname)
 */
JNIEXPORT jobjectArray JNICALL
Java_com_mmdns_MMDNSManager_nativeGetAllIPs(JNIEnv* env, jobject thiz, jstring jhostname) {
    std::string hostname = jstringToString(env, jhostname);
    
    try {
        auto dns = MMDNSEntranceImpl::getInstance("default");
        auto ips = dns->getAllIPs(hostname);
        
        // 创建String数组
        jclass stringClass = env->FindClass("java/lang/String");
        jobjectArray result = env->NewObjectArray(ips.size(), stringClass, nullptr);
        
        for (size_t i = 0; i < ips.size(); ++i) {
            jstring jip = stringToJstring(env, ips[i]);
            env->SetObjectArrayElement(result, i, jip);
            env->DeleteLocalRef(jip);
        }
        
        env->DeleteLocalRef(stringClass);
        return result;
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "JNI", 
            std::string("GetAllIPs failed: ") + e.what());
        return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
    }
}

/**
 * 设置DoH服务器
 * Java: native void nativeSetDohServer(String server)
 */
JNIEXPORT void JNICALL
Java_com_mmdns_MMDNSManager_nativeSetDohServer(JNIEnv* env, jobject thiz, jstring jserver) {
    std::string server = jstringToString(env, jserver);
    
    try {
        auto dns = MMDNSEntranceImpl::getInstance("default");
        auto impl = std::dynamic_pointer_cast<MMDNSEntranceImpl>(dns);
        if (impl) {
            impl->setDohServer(server);
        }
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "JNI", 
            std::string("SetDohServer failed: ") + e.what());
    }
}

/**
 * 设置网络状态
 * Java: native void nativeSetNetworkState(int state)
 */
JNIEXPORT void JNICALL
Java_com_mmdns_MMDNSManager_nativeSetNetworkState(JNIEnv* env, jobject thiz, jint jstate) {
    try {
        auto dns = MMDNSEntranceImpl::getInstance("default");
        MMDNSAppNetState state = static_cast<MMDNSAppNetState>(jstate);
        dns->setNetworkState(state);
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "JNI", 
            std::string("SetNetworkState failed: ") + e.what());
    }
}

/**
 * 清空缓存
 * Java: native void nativeClearCache()
 */
JNIEXPORT void JNICALL
Java_com_mmdns_MMDNSManager_nativeClearCache(JNIEnv* env, jobject thiz) {
    try {
        auto dns = MMDNSEntranceImpl::getInstance("default");
        dns->clear();
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "JNI", 
            std::string("ClearCache failed: ") + e.what());
    }
}

/**
 * 设置日志级别
 * Java: native void nativeSetLogLevel(int level)
 */
JNIEXPORT void JNICALL
Java_com_mmdns_MMDNSManager_nativeSetLogLevel(JNIEnv* env, jobject thiz, jint jlevel) {
    LogLevel level = static_cast<LogLevel>(jlevel);
    Logger::setLevel(level);
}

/**
 * 启用/禁用系统DNS
 * Java: native void nativeEnableSystemDNS(boolean enable)
 */
JNIEXPORT void JNICALL
Java_com_mmdns_MMDNSManager_nativeEnableSystemDNS(JNIEnv* env, jobject thiz, jboolean jenable) {
    try {
        auto dns = MMDNSEntranceImpl::getInstance("default");
        auto impl = std::dynamic_pointer_cast<MMDNSEntranceImpl>(dns);
        if (impl) {
            impl->enableSystemDNS(jenable);
        }
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "JNI", 
            std::string("EnableSystemDNS failed: ") + e.what());
    }
}

/**
 * 启用/禁用HTTP DNS
 * Java: native void nativeEnableHttpDNS(boolean enable)
 */
JNIEXPORT void JNICALL
Java_com_mmdns_MMDNSManager_nativeEnableHttpDNS(JNIEnv* env, jobject thiz, jboolean jenable) {
    try {
        auto dns = MMDNSEntranceImpl::getInstance("default");
        auto impl = std::dynamic_pointer_cast<MMDNSEntranceImpl>(dns);
        if (impl) {
            impl->enableHttpDNS(jenable);
        }
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "JNI", 
            std::string("EnableHttpDNS failed: ") + e.what());
    }
}

/**
 * 设置缓存目录
 * Java: native void nativeSetCacheDir(String dir)
 */
JNIEXPORT void JNICALL
Java_com_mmdns_MMDNSManager_nativeSetCacheDir(JNIEnv* env, jobject thiz, jstring jdir) {
    std::string dir = jstringToString(env, jdir);
    
    try {
        auto dns = MMDNSEntranceImpl::getInstance("default");
        auto impl = std::dynamic_pointer_cast<MMDNSEntranceImpl>(dns);
        if (impl) {
            impl->setCacheDir(dir);
        }
    } catch (const std::exception& e) {
        Logger::log(LogLevel::ERROR, "JNI", 
            std::string("SetCacheDir failed: ") + e.what());
    }
}

} // extern "C"