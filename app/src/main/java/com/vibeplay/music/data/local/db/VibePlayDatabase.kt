package com.vibeplay.music.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vibeplay.music.data.local.db.dao.EqualizerDao
import com.vibeplay.music.data.local.db.dao.PlaylistDao
import com.vibeplay.music.data.local.db.dao.SearchHistoryDao
import com.vibeplay.music.data.local.db.dao.SongDao
import com.vibeplay.music.data.local.db.entity.EqualizerPresetEntity
import com.vibeplay.music.data.local.db.entity.PlaylistEntity
import com.vibeplay.music.data.local.db.entity.PlaylistSongCrossRef
import com.vibeplay.music.data.local.db.entity.SearchHistoryEntity
import com.vibeplay.music.data.local.db.entity.SongEntity
import com.vibeplay.music.data.local.db.entity.SongStatsEntity

@Database(
    entities = [
        SongEntity::class,
        SongStatsEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        SearchHistoryEntity::class,
        EqualizerPresetEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VibePlayDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun equalizerDao(): EqualizerDao
}
