package com.whispervault.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.whispervault.transcription.TranscriptSegment
import java.io.IOException

data class ExportResult(
    val txtUri: String?,
    val jsonUri: String?,
    val srtUri: String?,
)

/**
 * Writes transcripts under Download/WhisperVault/ via MediaStore (no broad storage permission).
 */
object TranscriptExporter {

    private const val RELATIVE_DIR = "Download/WhisperVault"

    fun exportAll(
        context: Context,
        baseName: String,
        text: String,
        segments: List<TranscriptSegment>,
        modelPath: String,
    ): ExportResult {
        val safe = sanitize(baseName)
        val txt = writeText(context, "$safe.txt", "text/plain", TranscriptFormatter.toTxt(text))
        val json = writeText(
            context,
            "$safe.json",
            "application/json",
            TranscriptFormatter.toJson(baseName, text, segments, modelPath),
        )
        val srtBody = TranscriptFormatter.toSrt(segments)
        val srt = if (srtBody.isNotBlank()) {
            writeText(context, "$safe.srt", "application/x-subrip", srtBody)
        } else {
            // Still emit a valid single-cue SRT from full text without inventing timestamps.
            val fallback = TranscriptFormatter.toSrt(
                listOf(
                    TranscriptSegment(text = text, t0Centiseconds = 0, t1Centiseconds = 0),
                ),
            )
            writeText(context, "$safe.srt", "application/x-subrip", fallback)
        }
        return ExportResult(txt, json, srt)
    }

    private fun writeText(
        context: Context,
        displayName: String,
        mime: String,
        content: String,
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DIR)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val uri = resolver.insert(collection, values)
            ?: throw IOException("MediaStore insert failed for $displayName")
        resolver.openOutputStream(uri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Cannot open output stream for $displayName")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri.toString()
    }

    private fun sanitize(name: String): String {
        val base = name.substringBeforeLast('.').ifBlank { name }
        return base.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "transcript" }
    }

    fun exportDirectoryLabel(): String =
        "${Environment.DIRECTORY_DOWNLOADS}/WhisperVault"
}
