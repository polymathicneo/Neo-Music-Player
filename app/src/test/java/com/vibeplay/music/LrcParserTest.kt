package com.vibeplay.music

import com.vibeplay.music.data.local.lyrics.LrcParser
import com.vibeplay.music.domain.model.LyricLine
import com.vibeplay.music.domain.model.LyricsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parseLrc_handlesMultipleTimestampsAndSortsChronologically() {
        val raw = """
            [ar:Test Artist]
            [ti:Test Song]
            [00:12.50][00:45.00]Chorus line
            [00:05.20]First verse
        """.trimIndent()

        val result = LrcParser.parseLrc(raw)
        assertTrue(result.isSynced)
        assertEquals(LyricsSource.LRC_FILE, result.source)
        assertEquals(3, result.lines.size)
        assertEquals(5_200L, result.lines[0].timestampMs)
        assertEquals("First verse", result.lines[0].text)
        assertEquals(12_500L, result.lines[1].timestampMs)
        assertEquals("Chorus line", result.lines[1].text)
        assertEquals(45_000L, result.lines[2].timestampMs)
    }

    @Test
    fun parseLrc_appliesOffsetAndClampsZero() {
        val raw = """
            [offset:1000]
            [00:00.50]Too early
            [00:03.00]Shifted line
        """.trimIndent()

        val result = LrcParser.parseLrc(raw)
        assertEquals(0L, result.lines[0].timestampMs)
        assertEquals(2_000L, result.lines[1].timestampMs)
    }

    @Test
    fun parseEmbeddedText_detectsUnsyncedPlainLyrics() {
        val raw = "Line one\nLine two\nLine three"
        val result = LrcParser.parseEmbeddedText(raw)
        assertFalse(result.isSynced)
        assertEquals(LyricsSource.EMBEDDED_UNSYNCED, result.source)
        assertEquals(3, result.lines.size)
    }

    @Test
    fun findActiveLineIndex_returnsExactFloorMatch() {
        val lines = listOf(
            LyricLine(1_000L, "A"),
            LyricLine(5_000L, "B"),
            LyricLine(10_000L, "C")
        )
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, 500L))
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 1_000L))
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 7_500L))
        assertEquals(2, LrcParser.findActiveLineIndex(lines, 15_000L))
    }
}
