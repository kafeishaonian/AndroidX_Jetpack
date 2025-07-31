//
// Created by 魏红明 on 2025/7/31.
//
#include <jni.h>

static void init_signal_anr_detective(JNIEnv *env, jobject instance, jstring anr_trace_file_path, jstring trace_file_path) {

}

static void signal_anr_detective(JNIEnv *env, jobject instance) {

}

static const JNINativeMethod methods[] = {
        {"nativeInitSignalAnrDetective", "(Ljava/lang/String;Ljava/lang/String;)V", (void *) init_signal_anr_detective},
        {"nativeFreeSignalAnrDetective", "()V", (void *) signal_anr_detective}
};

extern "C"
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;

    if (vm->GetEnv((void **) & env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("com/aj/nativelib/AnrTrace");
    if (!clazz) {
        return JNI_ERR;
    }

    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]))) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}


