#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>

#include "whisper.h"

#define LOG_TAG "WhisperVaultJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_whispervault_transcription_NativeBridge_nativeVersion(
        JNIEnv *env,
        jclass /* clazz */) {
    return env->NewStringUTF("whisper.cpp-1.7.5");
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_whispervault_transcription_NativeBridge_nativeInit(
        JNIEnv *env,
        jclass /* clazz */,
        jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (path == nullptr) {
        return 0;
    }
    LOGI("Loading ggml model from %s", path);
    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(model_path, path);
    if (ctx == nullptr) {
        LOGE("Failed to load model");
        return 0;
    }
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT void JNICALL
Java_com_whispervault_transcription_NativeBridge_nativeFree(
        JNIEnv * /* env */,
        jclass /* clazz */,
        jlong handle) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(handle);
    if (ctx != nullptr) {
        whisper_free(ctx);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_whispervault_transcription_NativeBridge_nativeFullTranscribe(
        JNIEnv *env,
        jclass /* clazz */,
        jlong handle,
        jfloatArray pcm,
        jint num_threads,
        jstring language) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(handle);
    if (ctx == nullptr || pcm == nullptr) {
        return -1;
    }

    jfloat *samples = env->GetFloatArrayElements(pcm, nullptr);
    const jsize n_samples = env->GetArrayLength(pcm);
    const char *lang = language != nullptr ? env->GetStringUTFChars(language, nullptr) : nullptr;

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = (lang != nullptr && lang[0] != '\0') ? lang : "auto";
    params.n_threads = num_threads > 0 ? num_threads : 4;
    params.offset_ms = 0;
    params.no_context = true;
    params.single_segment = false;

    LOGI("whisper_full n_samples=%d threads=%d", n_samples, params.n_threads);
    const int rc = whisper_full(ctx, params, samples, n_samples);

    if (lang != nullptr) {
        env->ReleaseStringUTFChars(language, lang);
    }
    env->ReleaseFloatArrayElements(pcm, samples, JNI_ABORT);
    return rc;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_whispervault_transcription_NativeBridge_nativeSegmentCount(
        JNIEnv * /* env */,
        jclass /* clazz */,
        jlong handle) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(handle);
    if (ctx == nullptr) {
        return 0;
    }
    return whisper_full_n_segments(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_whispervault_transcription_NativeBridge_nativeSegmentText(
        JNIEnv *env,
        jclass /* clazz */,
        jlong handle,
        jint index) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(handle);
    if (ctx == nullptr) {
        return env->NewStringUTF("");
    }
    const char *text = whisper_full_get_segment_text(ctx, index);
    return env->NewStringUTF(text != nullptr ? text : "");
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_whispervault_transcription_NativeBridge_nativeSegmentT0(
        JNIEnv * /* env */,
        jclass /* clazz */,
        jlong handle,
        jint index) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(handle);
    if (ctx == nullptr) {
        return 0;
    }
    return whisper_full_get_segment_t0(ctx, index);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_whispervault_transcription_NativeBridge_nativeSegmentT1(
        JNIEnv * /* env */,
        jclass /* clazz */,
        jlong handle,
        jint index) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(handle);
    if (ctx == nullptr) {
        return 0;
    }
    return whisper_full_get_segment_t1(ctx, index);
}
