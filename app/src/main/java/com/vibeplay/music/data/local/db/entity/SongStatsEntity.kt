package com.vibeplay.music.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_stats")
data class SongStatsEntity(
    @PrimaryKey val songId: Long,
    val isFavorite: Boolean = false,
    val favoritedAt: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L
)
