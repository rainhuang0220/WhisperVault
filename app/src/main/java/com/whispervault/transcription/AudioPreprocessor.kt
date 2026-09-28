package com.whispervault.transcription

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Decodes supported audio containers to 16 kHz mono float PCM for whisper.cpp.
 * Uses MediaExtractor/MediaCodec (and a simple WAV path). Fully on-device.
 */
object AudioPreprocessor {

    private const val TARGET_SAMPLE_RATE = 16_000

    fun decodeToPcm16kMono(context: Context, uri: Uri): FloatArray {
        return try {
            decodeWithMediaExtractor(context, uri)
        } catch (first: Exception) {
            try {
                decodeWavIfPossible(context, uri)
            } catch (second: Exception) {
                throw TranscriptionException.DecodeFailed(
                    first.message ?: "unknown",
                    first,
                )
            }
        }
    }

    private fun decodeWithMediaExtractor(context: Context, uri: Uri): FloatArray {
        val extractor = MediaExtractor()
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

            val pcmBytes = ByteArrayOutputStream()
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
                            pcmBytes.write(chunk)
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

            val pcm16 = pcmBytes.toByteArray()
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

    private fun decodeWavIfPossible(context: Context, uri: Uri): FloatArray {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val header = ByteArray(44)
            val read = input.read(header)
            if (read < 12) throw TranscriptionException.DecodeFailed("WAV too short")
            val riff = String(header, 0, 4)
            if (riff != "RIFF") throw TranscriptionException.DecodeFailed("Not RIFF")

            // Minimal WAV parse: seek fmt + data chunks.
            val all = header + input.readBytes()
            val buffer = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(12)
            var audioFormat = 1
            var channels = 1
            var sampleRate = TARGET_SAMPLE_RATE
            var bitsPerSample = 16
            var dataOffset = -1
            var dataSize = 0

            while (buffer.remaining() >= 8) {
                val chunkIdBytes = ByteArray(4)
                buffer.get(chunkIdBytes)
                val chunkId = String(chunkIdBytes)
                val chunkSize = buffer.int
                when (chunkId) {
                    "fmt " -> {
                        audioFormat = buffer.short.toInt() and 0xFFFF
                        channels = buffer.short.toInt() and 0xFFFF
                        sampleRate = buffer.int
                        buffer.int // byte rate
                        buffer.short // block align
                        bitsPerSample = buffer.short.toInt() and 0xFFFF
                        val consumed = 16
                        if (chunkSize > consumed) {
                            buffer.position(buffer.position() + (chunkSize - consumed))
                        }
                    }
                    "data" -> {
                        dataOffset = buffer.position()
                        dataSize = chunkSize
                        break
                    }
                    else -> {
                        if (chunkSize < 0 || chunkSize > buffer.remaining()) break
                        buffer.position(buffer.position() + chunkSize)
                    }
                }
            }

            if (dataOffset < 0 || audioFormat != 1 || bitsPerSample != 16) {
                throw TranscriptionException.DecodeFailed("Unsupported WAV encoding")
            }
            val pcm = ByteArray(dataSize)
            System.arraycopy(all, dataOffset, pcm, 0, min(dataSize, all.size - dataOffset))
            return resampleTo16kMono(pcm, sampleRate, channels)
        } ?: throw TranscriptionException.DecodeFailed("Cannot open WAV")
    }

    private fun resampleTo16kMono(
        pcm16le: ByteArray,
        sampleRate: Int,
        channelCount: Int,
    ): FloatArray {
        val channels = channelCount.coerceAtLeast(1)
        val totalFrames = pcm16le.size / (2 * channels)
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
