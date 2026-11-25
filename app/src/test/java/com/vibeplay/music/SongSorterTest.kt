package com.vibeplay.music

import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.domain.model.SongSortConfig
import com.vibeplay.music.domain.model.SongSortOption
import com.vibeplay.music.domain.model.SongSorter
import com.vibeplay.music.domain.model.SortDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class SongSorterTest {

    private val songA = Song(
        id = 1L, title = "Alpha", artist = "Zeta Band", album = "Beta Album", albumId = 10L,
        durationMs = 180_000L, contentUri = "content://1", filePath = "/m/a.mp3",
        folderPath = "/m", folderName = "m", dateAdded = 100L, trackNumber = 2,
        year = 2019, mimeType = "audio/mpeg", sizeBytes = 4_000_000L, albumArtUri = null,
        isFavorite = false, playCount = 15, lastPlayedAt = 500L
    )

    private val songB = Song(
        id = 2L, title = "Gamma", artist = "Aaron", album = "Delta Album", albumId = 20L,
        durationMs = 300_000L, contentUri = "content://2", filePath = "/m/b.flac",
        folderPath = "/m", folderName = "m", dateAdded = 300L, trackNumber = 1,
        year = 2024, mimeType = "audio/flac", sizeBytes = 25_000_000L, albumArtUri = null,
        isFavorite = true, playCount = 3, lastPlayedAt = 900L
    )

    private val songC = Song(
        id = 3L, title = "Beta", artist = "Maya", album = "Alpha Album", albumId = 30L,
        durationMs = 120_000L, contentUri = "content://3", filePath = "/m/c.mp3",
        folderPath = "/m", folderName = "m", dateAdded = 200L, trackNumber = 3,
        year = 2021, mimeType = "audio/mpeg", sizeBytes = 2_000_000L, albumArtUri = null,
        isFavorite = false, playCount = 42, lastPlayedAt = 200L
    )

    private val sample = listOf(songA, songB, songC)

    @Test
    fun sortByDateAdded_descendingAndAscending() {
        val newest = SongSorter.sort(sample, SongSortConfig(SongSortOption.DATE_ADDED, SortDirection.DESCENDING))
        assertEquals(listOf(2L, 3L, 1L), newest.map { it.id })

        val oldest = SongSorter.sort(sample, SongSortConfig(SongSortOption.DATE_ADDED, SortDirection.ASCENDING))
        assertEquals(listOf(1L, 3L, 2L), oldest.map { it.id })
    }

    @Test
    fun sortByMostPlayedAndRecentlyPlayed() {
        val mostPlayed = SongSorter.sort(sample, SongSortConfig(SongSortOption.MOST_PLAYED, SortDirection.DESCENDING))
        assertEquals(listOf(3L, 1L, 2L), mostPlayed.map { it.id })

        val recentlyPlayed = SongSorter.sort(sample, SongSortConfig(SongSortOption.RECENTLY_PLAYED, SortDirection.DESCENDING))
        assertEquals(listOf(2L, 1L, 3L), recentlyPlayed.map { it.id })
    }

    @Test
    fun sortByTitleArtistAlbumDurationYearAndFileSize() {
        assertEquals(
            listOf(1L, 3L, 2L),
            SongSorter.sort(sample, SongSortConfig(SongSortOption.TITLE, SortDirection.ASCENDING)).map { it.id }
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            SongSorter.sort(sample, SongSortConfig(SongSortOption.ARTIST, SortDirection.ASCENDING)).map { it.id }
        )
        assertEquals(
            listOf(3L, 1L, 2L),
            SongSorter.sort(sample, SongSortConfig(SongSortOption.ALBUM, SortDirection.ASCENDING)).map { it.id }
        )
        assertEquals(
            listOf(2L, 1L, 3L),
            SongSorter.sort(sample, SongSortConfig(SongSortOption.DURATION, SortDirection.DESCENDING)).map { it.id }
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            SongSorter.sort(sample, SongSortConfig(SongSortOption.YEAR, SortDirection.DESCENDING)).map { it.id }
        )
        assertEquals(
            listOf(2L, 1L, 3L),
            SongSorter.sort(sample, SongSortConfig(SongSortOption.FILE_SIZE, SortDirection.DESCENDING)).map { it.id }
        )
    }
}
