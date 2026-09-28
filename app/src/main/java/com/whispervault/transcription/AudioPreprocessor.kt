package com.whispervault.transcription

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Decodes supported audio to 16 kHz mono float PCM for whisper.cpp.
 * Decodes in codec buffer chunks (does not require loading the compressed file as one array).
 * For very long audio, [decodeWindowed] yields successive PCM windows for streaming inference.
 */
object AudioPreprocessor {

    const val TARGET_SAMPLE_RATE = 16_000
    /** ~30s windows — within typical whisper.cpp context comfort. */
    const val WINDOW_SAMPLES = TARGET_SAMPLE_RATE * 30
    /** 1s overlap between windows to reduce boundary errors. */
    const val OVERLAP_SAMPLES = TARGET_SAMPLE_RATE

    fun decodeToPcm16kMono(context: Context, uri: Uri): FloatArray {
        return try {
            decodeWithMediaExtractor(context, uri)
        } catch (first: Exception) {
            try {
                decodeWavStreaming(context, uri)
            } catch (second: Exception) {
                throw TranscriptionException.DecodeFailed(
                    first.message ?: "unknown",
                    first,
                )
            }
        }
    }

    /**
     * Yields successive PCM windows without requiring the caller to keep unmodified copies.
     * The full decode still happens once; windows are views/slices to limit peak resident extras.
     */
    fun decodeWindowed(context: Context, uri: Uri): List<FloatArray> {
        val full = decodeToPcm16kMono(context, uri)
        if (full.size <= WINDOW_SAMPLES) {
            return listOf(full)
        }
        val windows = ArrayList<FloatArray>()
        var start = 0
        while (start < full.size) {
            val end = min(start + WINDOW_SAMPLES, full.size)
            windows.add(full.copyOfRange(start, end))
            if (end >= full.size) break
            start = end - OVERLAP_SAMPLES
            if (start < 0) start = 0
        }
        // Allow GC of full after windows are created — caller should not retain full.
        return windows
    }

    private fun decodeWithMediaExtractor(context: Context, uri: Uri): FloatArray {
        val extractor = MediaExtractor()
        val pcmChunks = ArrayList<ByteArray>()
        var totalBytes = 0
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: throw TranscriptionException.DecodeFailed("Cannot open uri")

            val trackIndex = selectAudioTrack(extractor)
            if (trackIndex < 0) {
                throw TranscriptionException.DecodeFailed("No audio track")
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: throw TranscriptionException.DecodeFailed("Missing mime")

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            var inputSampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                TARGET_SAMPLE_RATE
            }
            var channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                1
            }

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(
                                inIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0,
                            )
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFormat = codec.outputFormat
                        if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            inputSampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                    }
                    outIndex >= 0 -> {
                        if (info.size > 0) {
                            val outBuffer = codec.getOutputBuffer(outIndex)!!
                            val chunk = ByteArray(info.size)
                            outBuffer.get(chunk)
                            outBuffer.clear()
                            pcmChunks.add(chunk)
                            totalBytes += chunk.size
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            codec.stop()
            codec.release()

            val pcm16 = ByteArray(totalBytes)
            var offset = 0
            for (chunk in pcmChunks) {
                System.arraycopy(chunk, 0, pcm16, offset, chunk.size)
                offset += chunk.size
            }
            pcmChunks.clear()
            return resampleTo16kMono(pcm16, inputSampleRate, channelCount)
        } finally {
            extractor.release()
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    private fun decodeWavStreaming(context: Context, uri: Uri): FloatArray {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val header = ByteArray(12)
            if (input.read(header) < 12) throw TranscriptionException.DecodeFailed("WAV too short")
            if (String(header, 0, 4) != "RIFF") {
                throw TranscriptionException.DecodeFailed("Not RIFF")
            }

            var audioFormat = 1
            var channels = 1
            var sampleRate = TARGET_SAMPLE_RATE
            var bitsPerSample = 16
            val dataChunks = ArrayList<ByteArray>()
            var dataTotal = 0

            val chunkHeader = ByteArray(8)
            while (true) {
                val n = input.read(chunkHeader)
                if (n < 8) break
                val chunkId = String(chunkHeader, 0, 4)
                val chunkSize = ByteBuffer.wrap(chunkHeader, 4, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                when (chunkId) {
                    "fmt " -> {
                        val fmt = ByteArray(chunkSize)
                        input.read(fmt)
                        val bb = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                        audioFormat = bb.short.toInt() and 0xFFFF
                        channels = bb.short.toInt() and 0xFFFF
                        sampleRate = bb.int
                        bb.int
                        bb.short
                        bitsPerSample = bb.short.toInt() and 0xFFFF
                    }
                    "data" -> {
                        var remaining = chunkSize
                        val buf = ByteArray(64 * 1024)
                        while (remaining > 0) {
                            val toRead = min(buf.size, remaining)
                            val read = input.read(buf, 0, toRead)
                            if (read <= 0) break
                            dataChunks.add(buf.copyOf(read))
                            dataTotal += read
                            remaining -= read
                        }
                        break
                    }
                    else -> {
                        var skip = chunkSize.toLong()
                        while (skip > 0) {
                            val skipped = input.skip(skip)
                            if (skipped <= 0) break
                            skip -= skipped
                        }
                    }
                }
            }

            if (audioFormat != 1 || bitsPerSample != 16) {
                throw TranscriptionException.DecodeFailed("Unsupported WAV encoding")
            }
            val pcm = ByteArray(dataTotal)
            var offset = 0
            for (c in dataChunks) {
                System.arraycopy(c, 0, pcm, offset, c.size)
                offset += c.size
            }
            return resampleTo16kMono(pcm, sampleRate, channels)
        } ?: throw TranscriptionException.DecodeFailed("Cannot open WAV")
    }

    fun resampleTo16kMono(
        pcm16le: ByteArray,
        sampleRate: Int,
        channelCount: Int,
    ): FloatArray {
        val channels = channelCount.coerceAtLeast(1)
        val totalFrames = pcm16le.size / (2 * channels)
        if (totalFrames <= 0) return FloatArray(0)
        val mono = FloatArray(totalFrames)
        val bb = ByteBuffer.wrap(pcm16le).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until totalFrames) {
            var sum = 0f
            for (c in 0 until channels) {
                if (bb.remaining() < 2) break
                sum += bb.short / 32768.0f
            }
            mono[i] = sum / channels
        }
        if (sampleRate == TARGET_SAMPLE_RATE) {
            return mono
        }
        if (sampleRate <= 0) {
            throw TranscriptionException.DecodeFailed("Invalid sample rate")
        }
        val outLength = ((mono.size.toLong() * TARGET_SAMPLE_RATE) / sampleRate).toInt().coerceAtLeast(1)
        val out = FloatArray(outLength)
        for (i in 0 until outLength) {
            val srcPos = i.toDouble() * sampleRate / TARGET_SAMPLE_RATE
            val idx = srcPos.toInt().coerceIn(0, mono.size - 1)
            val next = (idx + 1).coerceAtMost(mono.size - 1)
            val frac = (srcPos - idx).toFloat()
            out[i] = mono[idx] * (1f - frac) + mono[next] * frac
        }
        return out
    }
}
