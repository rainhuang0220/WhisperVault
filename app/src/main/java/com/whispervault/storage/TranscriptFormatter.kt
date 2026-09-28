package com.whispervault.storage

import com.whispervault.transcription.TranscriptSegment
import org.json.JSONArray
import org.json.JSONObject

/**
 * Formats transcription results as plain text, JSON, and SubRip (SRT).
 * SRT timestamps use whisper.cpp segment times (centiseconds → hh:mm:ss,mmm).
 */
object TranscriptFormatter {

    fun toTxt(text: String): String = text.trim().let { if (it.endsWith('\n')) it else "$it\n" }

    fun toJson(
        displayName: String,
        text: String,
        segments: List<TranscriptSegment>,
        modelPath: String,
    ): String {
        val root = JSONObject()
        root.put("source", displayName)
        root.put("model", modelPath)
        root.put("text", text)
        val arr = JSONArray()
        segments.forEach { seg ->
            arr.put(
                JSONObject()
                    .put("text", seg.text)
                    .put("start_ms", seg.startMs)
                    .put("end_ms", seg.endMs)
                    .put("t0_centiseconds", seg.t0Centiseconds)
                    .put("t1_centiseconds", seg.t1Centiseconds),
            )
        }
        root.put("segments", arr)
        return root.toString(2) + "\n"
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
        val arr = JSONArray()
        segments.forEach { seg ->
            arr.put(
                JSONObject()
                    .put("text", seg.text)
                    .put("t0", seg.t0Centiseconds)
                    .put("t1", seg.t1Centiseconds),
            )
        }
        return arr.toString()
    }

    fun segmentsFromJson(json: String?): List<TranscriptSegment> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        TranscriptSegment(
                            text = obj.optString("text"),
                            t0Centiseconds = obj.optLong("t0"),
                            t1Centiseconds = obj.optLong("t1"),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
