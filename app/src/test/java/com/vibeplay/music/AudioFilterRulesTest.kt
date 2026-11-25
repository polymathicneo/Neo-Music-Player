package com.vibeplay.music

import com.vibeplay.music.data.local.scanner.AudioFilterRules
import com.vibeplay.music.domain.model.ScannerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFilterRulesTest {

    @Test
    fun supportsRequestedAudioFormats() {
        listOf("track.mp3", "song.FLAC", "beat.wav", "clip.aac", "album.m4a", "demo.ogg").forEach {
            assertTrue("Expected $it to be supported", AudioFilterRules.isSupportedAudioFormat(it, null))
        }
        assertFalse(AudioFilterRules.isSupportedAudioFormat("video.mp4", "video/mp4"))
        assertFalse(AudioFilterRules.isSupportedAudioFormat("ringtone.amr", "audio/amr"))
    }

    @Test
    fun enforcesDurationAndFolderIncludeExcludeRules() {
        val settings = ScannerSettings(
            minDurationMs = 15_000L,
            includedFolders = setOf("/storage/emulated/0/Music"),
            excludedFolders = setOf("/storage/emulated/0/WhatsApp Audio")
        )

        assertFalse(
            AudioFilterRules.shouldIncludeTrack(
                fileNameOrPath = "/storage/emulated/0/Music/short.mp3",
                mimeType = "audio/mpeg",
                durationMs = 10_000L,
                folderPath = "/storage/emulated/0/Music",
                settings = settings
            )
        )
        assertTrue(
            AudioFilterRules.shouldIncludeTrack(
                fileNameOrPath = "/storage/emulated/0/Music/full.flac",
                mimeType = "audio/flac",
                durationMs = 180_000L,
                folderPath = "/storage/emulated/0/Music",
                settings = settings
            )
        )
        assertFalse(
            AudioFilterRules.shouldIncludeTrack(
                fileNameOrPath = "/storage/emulated/0/Downloads/song.mp3",
                mimeType = "audio/mpeg",
                durationMs = 180_000L,
                folderPath = "/storage/emulated/0/Downloads",
                settings = settings
            )
        )
    }

    @Test
    fun extractFolderPathAndName_handlesFullAndRelativePaths() {
        val (path1, name1) = AudioFilterRules.extractFolderPathAndName(
            fullPath = "/storage/emulated/0/Music/Rock/song.mp3",
            relativePath = null
        )
        assertEquals("/storage/emulated/0/Music/Rock", path1)
        assertEquals("Rock", name1)

        val (path2, name2) = AudioFilterRules.extractFolderPathAndName(
            fullPath = null,
            relativePath = "Music/Chill/"
        )
        assertEquals("Music/Chill", path2)
        assertEquals("Chill", name2)
    }
}
