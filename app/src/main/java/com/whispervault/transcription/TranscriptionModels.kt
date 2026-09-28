package com.whispervault.transcription

/**
 * Supported ggml Whisper model sizes (user-provided files; never downloaded by the app).
 */
enum class WhisperModelSize(val recommendedFileName: String) {
    TINY("ggml-tiny.bin"),
    BASE("ggml-base.bin"),
    SMALL("ggml-small.bin");

    companion object {
        fun fromFileName(name: String): WhisperModelSize? {
            val lower = name.lowercase()
            return entries.firstOrNull { lower.contains(it.name.lowercase()) }
        }
    }
}

data class TranscriptSegment(
    val text: String,
    /** Segment start in centiseconds (whisper.cpp units). */
    val t0Centiseconds: Long,
    /** Segment end in centiseconds (whisper.cpp units). */
    val t1Centiseconds: Long,
) {
    val startMs: Long get() = t0Centiseconds * 10
    val endMs: Long get() = t1Centiseconds * 10
}

data class TranscriptionResult(
    val text: String,
    val segments: List<TranscriptSegment>,
    val modelPath: String,
)

sealed class TranscriptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class ModelMissing(path: String) :
        TranscriptionException("Model file missing or unreadable: $path")

    class ModelLoadFailed(path: String) :
        TranscriptionException("Failed to load ggml model: $path")

    class InferenceFailed(detail: String) :
        TranscriptionException("whisper_full failed: $detail")

    class DecodeFailed(detail: String, cause: Throwable? = null) :
        TranscriptionException("Audio decode failed: $detail", cause)
}
