#include <jni.h>
#include <android/log.h>

#define LOG_TAG "WhisperVaultJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_whispervault_transcription_NativeBridge_nativeVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    LOGI("WhisperVault JNI skeleton loaded");
    return env->NewStringUTF("whispervault-jni-skeleton");
}
