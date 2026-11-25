package com.vibeplay.music.data.local.lyrics

import com.vibeplay.music.domain.model.LyricLine
import com.vibeplay.music.domain.model.LyricsData
import com.vibeplay.music.domain.model.LyricsSource

object LrcParser {

    private val TIME_TAG_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val OFFSET_REGEX = Regex("""\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)
    private val METADATA_TAG_REGEX = Regex("""^\[(?:ar|ti|al|by|au|length|re|ve|offset):.*]$""", RegexOption.IGNORE_CASE)

    fun parseLrc(rawContent: String, source: LyricsSource = LyricsSource.LRC_FILE): LyricsData {
        if (rawContent.isBlank()) return LyricsData()

        var offsetMs = 0L
        val parsedLines = mutableListOf<LyricLine>()
        val plainLines = mutableListOf<String>()

        rawContent.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            val offsetMatch = OFFSET_REGEX.find(line)
            if (offsetMatch != null) {
                offsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
                return@forEach
            }

            if (METADATA_TAG_REGEX.matches(line)) {
                return@forEach
            }

            val matches = TIME_TAG_REGEX.findAll(line).toList()
            if (matches.isEmpty()) {
                plainLines += line
                return@forEach
            }

            val textStart = matches.last().range.last + 1
            val lyricText = if (textStart <= line.length) {
                line.substring(textStart).trim()
            } else {
                ""
            }
            if (lyricText.isEmpty()) return@forEach

            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionStr = match.groupValues[3]
                val fractionMs = when (fractionStr.length) {
                    0 -> 0L
                    1 -> (fractionStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fractionStr.toLongOrNull() ?: 0L) * 10L
                    else -> fractionStr.take(3).toLongOrNull() ?: 0L
                }
                val rawTimeMs = minutes * 60_000L + seconds * 1_000L + fractionMs
                val adjustedTimeMs = (rawTimeMs - offsetMs).coerceAtLeast(0L)
                parsedLines += LyricLine(timestampMs = adjustedTimeMs, text = lyricText)
            }
        }

        return if (parsedLines.isNotEmpty()) {
            LyricsData(
                lines = parsedLines.sortedBy { it.timestampMs },
                isSynced = true,
                source = source
            )
        } else if (plainLines.isNotEmpty()) {
            LyricsData(
                lines = plainLines.mapIndexed { idx, text -> LyricLine(idx * 1000L, text) },
                isSynced = false,
                source = LyricsSource.EMBEDDED_UNSYNCED
            )
        } else {
            LyricsData()
        }
    }

    fun parseEmbeddedText(rawText: String): LyricsData {
        if (rawText.isBlank()) return LyricsData()
        return if (TIME_TAG_REGEX.containsMatchIn(rawText)) {
            parseLrc(rawText, source = LyricsSource.EMBEDDED_SYNCED)
        } else {
            val lines = rawText.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapIndexed { idx, text -> LyricLine(idx.toLong(), text) }
                .toList()
            LyricsData(
                lines = lines,
                isSynced = false,
                source = if (lines.isNotEmpty()) LyricsSource.EMBEDDED_UNSYNCED else LyricsSource.NONE
            )
        }
    }

    fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timestampMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
