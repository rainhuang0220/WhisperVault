package com.whispervault.transcription

/**
 * JNI entry points for the offline whisper.cpp engine (arm64-v8a).
 */
object NativeBridge {
    init {
        System.loadLibrary("whispervault")
    }

    @JvmStatic
    external fun nativeVersion(): String

    @JvmStatic
    external fun nativeInit(modelPath: String): Long

    @JvmStatic
    external fun nativeFree(handle: Long)

    @JvmStatic
    external fun nativeFullTranscribe(
        handle: Long,
        pcm: FloatArray,
        numThreads: Int,
        language: String,
    ): Int

    @JvmStatic
    external fun nativeSegmentCount(handle: Long): Int

    @JvmStatic
    external fun nativeSegmentText(handle: Long, index: Int): String

    @JvmStatic
    external fun nativeSegmentT0(handle: Long, index: Int): Long

    @JvmStatic
    external fun nativeSegmentT1(handle: Long, index: Int): Long
}
