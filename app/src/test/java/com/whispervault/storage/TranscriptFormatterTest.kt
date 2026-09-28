package com.whispervault.storage

import com.whispervault.transcription.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptFormatterTest {
    private val segments = listOf(
        TranscriptSegment("Hello", 0, 150),
        TranscriptSegment("world", 150, 300),
    )

    @Test
    fun txtTrimsAndEndsWithNewline() {
        assertEquals("Hello world\n", TranscriptFormatter.toTxt("Hello world"))
    }

    @Test
    fun jsonContainsTextAndSegments() {
        val json = TranscriptFormatter.toJson("a.wav", "Hello world", segments, "/models/ggml-tiny.bin")
        assertTrue(json.contains("\"text\": \"Hello world\""))
        assertTrue(json.contains("start_ms"))
        assertTrue(json.contains("ggml-tiny.bin"))
    }

    @Test
    fun srtUsesWhisperTimestamps() {
        val srt = TranscriptFormatter.toSrt(segments)
        assertTrue(srt.startsWith("1\n"))
        assertTrue(srt.contains("00:00:00,000 --> 00:00:01,500"))
        assertTrue(srt.contains("Hello"))
        assertTrue(srt.contains("00:00:01,500 --> 00:00:03,000"))
    }

    @Test
    fun srtTimestampFormatting() {
        assertEquals("00:00:00,000", TranscriptFormatter.formatSrtTimestamp(0))
        assertEquals("01:01:01,001", TranscriptFormatter.formatSrtTimestamp(3_661_001))
    }

    @Test
    fun segmentJsonRoundTrip() {
        val encoded = TranscriptFormatter.segmentsToJson(segments)
        val decoded = TranscriptFormatter.segmentsFromJson(encoded)
        assertEquals(2, decoded.size)
        assertEquals("Hello", decoded[0].text)
        assertEquals(150L, decoded[1].t0Centiseconds)
    }
}
