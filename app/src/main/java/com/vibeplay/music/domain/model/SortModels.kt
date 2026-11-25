package com.vibeplay.music.domain.model

enum class SortDirection {
    ASCENDING,
    DESCENDING;

    fun toggle(): SortDirection = if (this == ASCENDING) DESCENDING else ASCENDING
}

enum class SongSortOption(
    val label: String,
    val ascHint: String,
    val descHint: String,
    val defaultDirection: SortDirection
) {
    DEFAULT("Default Order", "Original order", "Reversed order", SortDirection.ASCENDING),
    DATE_ADDED("Date Added", "Oldest first", "Newest first", SortDirection.DESCENDING),
    MOST_PLAYED("Most Played", "Least played first", "Most played first", SortDirection.DESCENDING),
    RECENTLY_PLAYED("Recently Played", "Least recent first", "Most recent first", SortDirection.DESCENDING),
    TITLE("Title", "A to Z", "Z to A", SortDirection.ASCENDING),
    ARTIST("Artist", "A to Z", "Z to A", SortDirection.ASCENDING),
    ALBUM("Album", "A to Z", "Z to A", SortDirection.ASCENDING),
    DURATION("Duration", "Shortest first", "Longest first", SortDirection.DESCENDING),
    YEAR("Release Year", "Oldest year first", "Newest year first", SortDirection.DESCENDING),
    FILE_SIZE("File Size", "Smallest first", "Largest first", SortDirection.DESCENDING);

    fun hintFor(direction: SortDirection): String =
        if (direction == SortDirection.ASCENDING) ascHint else descHint
}

data class SongSortConfig(
    val option: SongSortOption = SongSortOption.DATE_ADDED,
    val direction: SortDirection = SortDirection.DESCENDING
)

object SongSorter {
    fun sort(songs: List<Song>, config: SongSortConfig): List<Song> {
        if (songs.size <= 1) return songs
        if (config.option == SongSortOption.DEFAULT) {
            return if (config.direction == SortDirection.ASCENDING) songs else songs.reversed()
        }

        val comparator: Comparator<Song> = when (config.option) {
            SongSortOption.DEFAULT -> compareBy { 0 }
            SongSortOption.DATE_ADDED -> compareBy<Song> { it.dateAdded }.thenBy { it.title.lowercase() }
            SongSortOption.MOST_PLAYED -> compareBy<Song> { it.playCount }.thenBy { it.lastPlayedAt }.thenBy { it.title.lowercase() }
            SongSortOption.RECENTLY_PLAYED -> compareBy<Song> { it.lastPlayedAt }.thenBy { it.playCount }.thenBy { it.title.lowercase() }
            SongSortOption.TITLE -> compareBy<Song> { it.title.lowercase() }.thenBy { it.artist.lowercase() }
            SongSortOption.ARTIST -> compareBy<Song> { it.artist.lowercase() }.thenBy { it.album.lowercase() }.thenBy { it.trackNumber }
            SongSortOption.ALBUM -> compareBy<Song> { it.album.lowercase() }.thenBy { it.trackNumber }.thenBy { it.title.lowercase() }
            SongSortOption.DURATION -> compareBy<Song> { it.durationMs }.thenBy { it.title.lowercase() }
            SongSortOption.YEAR -> compareBy<Song> { it.year }.thenBy { it.album.lowercase() }.thenBy { it.trackNumber }
            SongSortOption.FILE_SIZE -> compareBy<Song> { it.sizeBytes }.thenBy { it.title.lowercase() }
        }

        return if (config.direction == SortDirection.ASCENDING) {
            songs.sortedWith(comparator)
        } else {
            songs.sortedWith(comparator.reversed())
        }
    }
}
