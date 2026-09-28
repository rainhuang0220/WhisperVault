package com.whispervault.data

import android.net.Uri

/**
 * Metadata for a user-selected audio file from Storage Access Framework.
 */
data class AudioFile(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val durationMs: Long? = null,
) {
    val extension: String
        get() = displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase()

    fun isSupportedFormat(): Boolean = extension in SUPPORTED_EXTENSIONS ||
        (mimeType != null && mimeType in SUPPORTED_MIME_TYPES)

    companion object {
        val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "m4a", "flac", "aac", "ogg")
        val SUPPORTED_MIME_TYPES = setOf(
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/x-wav",
            "audio/wave",
            "audio/mp4",
            "audio/m4a",
            "audio/aac",
            "audio/flac",
            "audio/x-flac",
            "audio/ogg",
            "application/ogg",
            "audio/*",
        )
        val PICKER_MIME_TYPES = arrayOf(
            "audio/mpeg",
            "audio/wav",
            "audio/x-wav",
            "audio/mp4",
            "audio/aac",
            "audio/flac",
            "audio/ogg",
            "audio/*",
        )
    }
}
