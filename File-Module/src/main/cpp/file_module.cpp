#include <jni.h>
#include <string>
#include "FileInterface.h"
#include "utils/log_utils.h"

#define TAG "file_module.h"

static jboolean
initManager(JNIEnv *env, jobject instance, jstring base_path, jint cache_size, jboolean use_async) {
    const char *path_chars = env->GetStringUTFChars(base_path, nullptr);
    if (!path_chars) {
        LOGE(TAG, "Failed to get base path string");
        return false;
    }

    bool result = FileInterface::getInstance().initManager(path_chars,
                                                           static_cast<size_t>(cache_size),
                                                           static_cast<bool>(use_async));
    env->ReleaseStringUTFChars(base_path, path_chars);
    return result;
}

static jboolean
createFile(JNIEnv *env, jobject instance, jstring business_id, jstring filename, jstring content) {
    const char *biz_id = env->GetStringUTFChars(business_id, nullptr);
    const char *file_name = env->GetStringUTFChars(filename, nullptr);
    const char *content_str = env->GetStringUTFChars(content, nullptr);

    if (!biz_id || !file_name || !content_str) {
        LOGE(TAG, "Failed to get string parameters");
        if (biz_id) env->ReleaseStringUTFChars(business_id, biz_id);
        if (file_name) env->ReleaseStringUTFChars(filename, file_name);
        if (content_str) env->ReleaseStringUTFChars(content, content_str);
        return false;
    }

    bool result = FileInterface::getInstance().create_file(
            biz_id,
            file_name,
            content_str
    );

    env->ReleaseStringUTFChars(business_id, biz_id);
    env->ReleaseStringUTFChars(filename, file_name);
    env->ReleaseStringUTFChars(content, content_str);

    return result;
}

static jstring readFile(JNIEnv *env, jobject instance, jstring business_id, jstring filename) {
    const char *biz_id = env->GetStringUTFChars(business_id, nullptr);
    const char *file_name = env->GetStringUTFChars(filename, nullptr);

    if (!biz_id || !file_name) {
        LOGE(TAG, "Failed to get string parameters");
        if (biz_id) env->ReleaseStringUTFChars(business_id, biz_id);
        if (file_name) env->ReleaseStringUTFChars(filename, file_name);
        return nullptr;
    }

    std::string content;
    bool success = FileInterface::getInstance().read_file(
            biz_id,
            file_name,
            content);

    env->ReleaseStringUTFChars(business_id, biz_id);
    env->ReleaseStringUTFChars(filename, file_name);

    if (!success || content.empty()) {
        return nullptr;
    }

    return env->NewStringUTF(content.c_str());
}

static jboolean
updateFile(JNIEnv *env, jobject instance, jstring business_id, jstring filename, jstring content) {
    const char *biz_id = env->GetStringUTFChars(business_id, nullptr);
    const char *file_name = env->GetStringUTFChars(filename, nullptr);
    const char *content_str = env->GetStringUTFChars(content, nullptr);

    jboolean result = false;

    if (biz_id && file_name && content_str) {
        result = FileInterface::getInstance().update_file(
                biz_id,
                file_name,
                content_str
        );
    } else {
        LOGE(TAG, "Failed to get string parameters");
    }

    if (biz_id) env->ReleaseStringUTFChars(business_id, biz_id);
    if (file_name) env->ReleaseStringUTFChars(filename, file_name);
    if (content_str) env->ReleaseStringUTFChars(content, content_str);

    return result;
}

static jboolean
appendFile(JNIEnv *env, jobject instance, jstring business_id, jstring filename, jstring content) {
    const char *biz_id = env->GetStringUTFChars(business_id, nullptr);
    const char *file_name = env->GetStringUTFChars(filename, nullptr);
    const char *content_str = env->GetStringUTFChars(content, nullptr);

    jboolean result = false;

    if (biz_id && file_name && content_str) {
        result = FileInterface::getInstance().append_file(
                biz_id,
                file_name,
                content_str
        );
    } else {
        LOGE(TAG, "Failed to get string parameters");
    }

    if (biz_id) env->ReleaseStringUTFChars(business_id, biz_id);
    if (file_name) env->ReleaseStringUTFChars(filename, file_name);
    if (content_str) env->ReleaseStringUTFChars(content, content_str);
    return result;
}

static jboolean deleteFile(JNIEnv *env, jobject instance, jstring business_id, jstring filename) {
    const char *biz_id = env->GetStringUTFChars(business_id, nullptr);
    const char *file_name = env->GetStringUTFChars(filename, nullptr);

    jboolean result = false;

    if (biz_id && file_name) {
        result = FileInterface::getInstance().delete_file(
                biz_id,
                file_name);
    } else {
        LOGE(TAG, "Failed to get string parameters");
    }

    if (biz_id) env->ReleaseStringUTFChars(business_id, biz_id);
    if (file_name) env->ReleaseStringUTFChars(filename, file_name);

    return result;
}

static jboolean fileExists(JNIEnv *env, jobject instance, jstring business_id, jstring filename) {
    const char *biz_id = env->GetStringUTFChars(business_id, nullptr);
    const char *file_name = env->GetStringUTFChars(filename, nullptr);

    jboolean result = false;

    if (biz_id && file_name) {
        result = FileInterface::getInstance().file_exists(
                biz_id,
                file_name);
    } else {
        LOGE(TAG, "Failed to get string parameters");
    }

    if (biz_id) env->ReleaseStringUTFChars(business_id, biz_id);
    if (file_name) env->ReleaseStringUTFChars(filename, file_name);

    return result;
}

static jobject prefetchDirectory(JNIEnv *env, jobject instance, jstring business_id, jstring substr, jint day, jboolean flag) {
    const char *biz_id = env->GetStringUTFChars(business_id, nullptr);
    const char *sub_str = env->GetStringUTFChars(substr, nullptr);
    const int32_t day_int = static_cast<int32_t>(day);
    const bool flag_bool = (flag == JNI_TRUE);

    if (!biz_id) {
        LOGE(TAG, "Failed to get business_id string");
        return nullptr;
    }

    std::vector<std::string> files;
    FileInterface::getInstance().prefetch_directory(
            biz_id,
            sub_str,
            day_int,
            flag_bool,
            files);

    env->ReleaseStringUTFChars(business_id, biz_id);
    env->ReleaseStringUTFChars(substr, sub_str);

    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "(I)V");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");

    jobject result = env->NewObject(arrayListClass, arrayListConstructor, files.size());

    jclass stringClass = env->FindClass("java/lang/String");

    for (const std::string &file: files) {
        jstring javaString = env->NewStringUTF(file.c_str());
        env->CallBooleanMethod(result, addMethod, javaString);
        env->DeleteLocalRef(javaString);
    }
    return result;
}


static const JNINativeMethod gMethod[] = {
        {"initManager",       "(Ljava/lang/String;IZ)Z",                                   (void *) initManager},
        {"createFile",        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *) createFile},
        {"readFile",          "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",  (void *) readFile},
        {"updateFile",        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *) updateFile},
        {"appendFile",        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *) appendFile},
        {"deleteFile",        "(Ljava/lang/String;Ljava/lang/String;)Z",                   (void *) deleteFile},
        {"fileExists",        "(Ljava/lang/String;Ljava/lang/String;)Z",                   (void *) fileExists},
        {"prefetchDirectory", "(Ljava/lang/String;Ljava/lang/String;IZ)Ljava/util/List;",  (void *) prefetchDirectory},
};


extern "C"
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("com/example/file_module/FileSystem");
    if (!clazz) {
        return JNI_ERR;
    }

    if (env->RegisterNatives(clazz, gMethod, sizeof(gMethod) / sizeof(gMethod[0]))) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}