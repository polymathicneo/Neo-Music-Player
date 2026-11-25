package com.vibeplay.music.domain.repository

import com.vibeplay.music.domain.model.LyricsData
import com.vibeplay.music.domain.model.Song

interface LyricsRepository {
    suspend fun loadLyricsForSong(song: Song): LyricsData
}
