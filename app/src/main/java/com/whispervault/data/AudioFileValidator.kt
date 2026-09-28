package com.whispervault.data

object AudioFileValidator {
    fun isSupportedExtension(extension: String): Boolean =
        extension.lowercase() in AudioFile.SUPPORTED_EXTENSIONS

    fun isSupportedMime(mime: String?): Boolean {
        if (mime == null) return false
        if (mime in AudioFile.SUPPORTED_MIME_TYPES) return true
        return mime.startsWith("audio/")
    }

    fun isSupported(displayName: String, mimeType: String?): Boolean {
        val ext = displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return isSupportedExtension(ext) || isSupportedMime(mimeType)
    }
}
