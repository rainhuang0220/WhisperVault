package com.whispervault.storage

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.whispervault.data.AudioFile

/**
 * Storage Access Framework helpers for selecting and reading audio on-device.
 */
object StorageGateway {

    fun resolveAudioFiles(context: Context, uris: List<Uri>): List<AudioFile> {
        val resolver = context.contentResolver
        return uris.mapNotNull { uri ->
            takePersistableReadPermission(resolver, uri)
            resolveAudioFile(resolver, uri)
        }.filter { it.isSupportedFormat() }
    }

    fun resolveAudioFile(resolver: ContentResolver, uri: Uri): AudioFile? {
        var displayName = uri.lastPathSegment ?: "audio"
        var sizeBytes = -1L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }
        val mimeType = resolver.getType(uri)
        val durationMs = readDurationMs(resolver, uri)
        return AudioFile(
            uri = uri,
            displayName = displayName,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            durationMs = durationMs,
        )
    }

    private fun takePersistableReadPermission(resolver: ContentResolver, uri: Uri) {
        try {
            resolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Some providers do not support persistable permissions; openable grants still work.
        }
    }

    private fun readDurationMs(resolver: ContentResolver, uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(resolver.openFileDescriptor(uri, "r")?.fileDescriptor)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
