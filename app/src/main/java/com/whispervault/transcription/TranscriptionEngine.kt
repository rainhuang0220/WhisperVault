package com.whispervault.transcription

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * Offline ASR facade backed by whisper.cpp via JNI.
 * Never performs network I/O.
 * Long audio is processed in overlapping windows and transcripts are concatenated.
 */
class TranscriptionEngine {

    private val handle = AtomicLong(0L)
    private var loadedModelPath: String? = null

    @Synchronized
    fun loadModel(modelPath: String) {
        val file = File(modelPath)
        if (!file.isFile || !file.canRead()) {
            throw TranscriptionException.ModelMissing(modelPath)
        }
        release()
        val ptr = NativeBridge.nativeInit(modelPath)
        if (ptr == 0L) {
            throw TranscriptionException.ModelLoadFailed(modelPath)
        }
        handle.set(ptr)
        loadedModelPath = modelPath
    }

    @Synchronized
    fun isModelLoaded(): Boolean = handle.get() != 0L

    @Synchronized
    fun currentModelPath(): String? = loadedModelPath

    @Synchronized
    fun release() {
        val ptr = handle.getAndSet(0L)
        if (ptr != 0L) {
            NativeBridge.nativeFree(ptr)
        }
        loadedModelPath = null
    }

    fun transcribePcm(
        pcm: FloatArray,
        language: String = "auto",
        numThreads: Int = preferredThreadCount(),
        timeOffsetMs: Long = 0L,
    ): TranscriptionResult {
        val ptr = handle.get()
        if (ptr == 0L) {
            throw TranscriptionException.ModelMissing(loadedModelPath ?: "(none)")
        }
        val rc = NativeBridge.nativeFullTranscribe(ptr, pcm, numThreads, language)
        if (rc != 0) {
            throw TranscriptionException.InferenceFailed("code=$rc")
        }
        val count = NativeBridge.nativeSegmentCount(ptr)
        val offsetCs = timeOffsetMs / 10
        val segments = ArrayList<TranscriptSegment>(count)
        val textBuilder = StringBuilder()
        for (i in 0 until count) {
            val text = NativeBridge.nativeSegmentText(ptr, i).trim()
            val segment = TranscriptSegment(
                text = text,
                t0Centiseconds = NativeBridge.nativeSegmentT0(ptr, i) + offsetCs,
                t1Centiseconds = NativeBridge.nativeSegmentT1(ptr, i) + offsetCs,
            )
            segments.add(segment)
            if (text.isNotEmpty()) {
                if (textBuilder.isNotEmpty()) textBuilder.append(' ')
                textBuilder.append(text)
            }
        }
        return TranscriptionResult(
            text = textBuilder.toString().trim(),
            segments = segments,
            modelPath = loadedModelPath.orEmpty(),
        )
    }

    fun transcribeUri(
        context: Context,
        uri: Uri,
        language: String = "auto",
    ): TranscriptionResult {
        return try {
            val windows = AudioPreprocessor.decodeWindowed(context, uri)
            if (windows.isEmpty()) {
                return TranscriptionResult("", emptyList(), loadedModelPath.orEmpty())
            }
            if (windows.size == 1) {
                return transcribePcm(windows[0], language)
            }
            val allSegments = ArrayList<TranscriptSegment>()
            val textBuilder = StringBuilder()
            var offsetSamples = 0
            windows.forEachIndexed { index, window ->
                val offsetMs = (offsetSamples.toLong() * 1000L) / AudioPreprocessor.TARGET_SAMPLE_RATE
                val part = transcribePcm(window, language, timeOffsetMs = offsetMs)
                allSegments.addAll(part.segments)
                if (part.text.isNotEmpty()) {
                    if (textBuilder.isNotEmpty()) textBuilder.append(' ')
                    textBuilder.append(part.text)
                }
                val advance = if (index == windows.lastIndex) {
                    window.size
                } else {
                    window.size - AudioPreprocessor.OVERLAP_SAMPLES
                }
                offsetSamples += advance.coerceAtLeast(0)
            }
            TranscriptionResult(
                text = textBuilder.toString().trim(),
                segments = allSegments,
                modelPath = loadedModelPath.orEmpty(),
            )
        } catch (oom: OutOfMemoryError) {
            System.gc()
            throw TranscriptionException.InferenceFailed("Out of memory while transcribing")
        }
    }

    fun transcribeFile(
        context: Context,
        audioPath: String,
        modelPath: String,
        language: String = "auto",
    ): TranscriptionResult {
        if (!isModelLoaded() || loadedModelPath != modelPath) {
            loadModel(modelPath)
        }
        return transcribeUri(context, Uri.fromFile(File(audioPath)), language)
    }

    companion object {
        fun preferredThreadCount(): Int {
            val cores = Runtime.getRuntime().availableProcessors()
            return max(2, cores / 2).coerceAtMost(6)
        }

        fun engineVersion(): String = try {
            NativeBridge.nativeVersion()
        } catch (_: UnsatisfiedLinkError) {
            "native-unavailable"
        }
    }
}
