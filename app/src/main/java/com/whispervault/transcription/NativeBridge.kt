package com.whispervault.transcription

/**
 * JNI entry points for the offline whisper engine.
 * Phase 1 exposes a version probe so the native build is verified.
 */
object NativeBridge {
    init {
        System.loadLibrary("whispervault")
    }

    @JvmStatic
    external fun nativeVersion(): String
}
