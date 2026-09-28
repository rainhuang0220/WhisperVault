package com.whispervault.storage

import com.whispervault.transcription.TranscriptSegment

/**
 * Formats transcription results as plain text, JSON, and SubRip (SRT).
 * SRT timestamps use whisper.cpp segment times (centiseconds → hh:mm:ss,mmm).
 * Pure Kotlin (no org.json) so JVM unit tests can run without Android stubs.
 */
object TranscriptFormatter {

    fun toTxt(text: String): String = text.trim().let { if (it.endsWith('\n')) it else "$it\n" }

    fun toJson(
        displayName: String,
        text: String,
        segments: List<TranscriptSegment>,
        modelPath: String,
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"source\": ").append(jsonString(displayName)).append(",\n")
        sb.append("  \"model\": ").append(jsonString(modelPath)).append(",\n")
        sb.append("  \"text\": ").append(jsonString(text)).append(",\n")
        sb.append("  \"segments\": [\n")
        segments.forEachIndexed { index, seg ->
            sb.append("    {\n")
            sb.append("      \"text\": ").append(jsonString(seg.text)).append(",\n")
            sb.append("      \"start_ms\": ").append(seg.startMs).append(",\n")
            sb.append("      \"end_ms\": ").append(seg.endMs).append(",\n")
            sb.append("      \"t0_centiseconds\": ").append(seg.t0Centiseconds).append(",\n")
            sb.append("      \"t1_centiseconds\": ").append(seg.t1Centiseconds).append("\n")
            sb.append("    }")
            if (index < segments.lastIndex) sb.append(',')
            sb.append('\n')
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    fun toSrt(segments: List<TranscriptSegment>): String {
        if (segments.isEmpty()) {
            return ""
        }
        val sb = StringBuilder()
        segments.forEachIndexed { index, seg ->
            sb.append(index + 1).append('\n')
            sb.append(formatSrtTimestamp(seg.startMs))
                .append(" --> ")
                .append(formatSrtTimestamp(seg.endMs))
                .append('\n')
            sb.append(seg.text.trim()).append('\n')
            sb.append('\n')
        }
        return sb.toString()
    }

    fun formatSrtTimestamp(timeMs: Long): String {
        val total = timeMs.coerceAtLeast(0)
        val hours = total / 3_600_000
        val minutes = (total % 3_600_000) / 60_000
        val seconds = (total % 60_000) / 1_000
        val millis = total % 1_000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }

    fun segmentsToJson(segments: List<TranscriptSegment>): String {
        val sb = StringBuilder()
        sb.append('[')
        segments.forEachIndexed { index, seg ->
            if (index > 0) sb.append(',')
            sb.append('{')
            sb.append("\"text\":").append(jsonString(seg.text)).append(',')
            sb.append("\"t0\":").append(seg.t0Centiseconds).append(',')
            sb.append("\"t1\":").append(seg.t1Centiseconds)
            sb.append('}')
        }
        sb.append(']')
        return sb.toString()
    }

    fun segmentsFromJson(json: String?): List<TranscriptSegment> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val result = ArrayList<TranscriptSegment>()
        val objectRegex = Regex("""\{[^{}]*\}""")
        objectRegex.findAll(json).forEach { match ->
            val obj = match.value
            val text = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""").find(obj)?.groupValues?.get(1)
                ?.replace("\\\"", "\"")
                ?.replace("\\n", "\n")
                ?: ""
            val t0 = Regex(""""t0"\s*:\s*(-?\d+)""").find(obj)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val t1 = Regex(""""t1"\s*:\s*(-?\d+)""").find(obj)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            result.add(TranscriptSegment(text = text, t0Centiseconds = t0, t1Centiseconds = t1))
        }
        return result
    }

    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
