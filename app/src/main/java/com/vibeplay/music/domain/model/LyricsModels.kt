package com.vibeplay.music.domain.model

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

enum class LyricsSource {
    NONE,
    LRC_FILE,
    EMBEDDED_SYNCED,
    EMBEDDED_UNSYNCED
}

data class LyricsData(
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false,
    val source: LyricsSource = LyricsSource.NONE
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}
