package com.vibeplay.music.di

import android.content.Context
import androidx.room.Room
import com.vibeplay.music.data.local.db.VibePlayDatabase
import com.vibeplay.music.data.local.db.dao.EqualizerDao
import com.vibeplay.music.data.local.db.dao.PlaylistDao
import com.vibeplay.music.data.local.db.dao.SearchHistoryDao
import com.vibeplay.music.data.local.db.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VibePlayDatabase {
        return Room.databaseBuilder(
            context,
            VibePlayDatabase::class.java,
            "vibeplay_database.db"
        ).build()
    }

    @Provides
    fun provideSongDao(db: VibePlayDatabase): SongDao = db.songDao()

    @Provides
    fun providePlaylistDao(db: VibePlayDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideSearchHistoryDao(db: VibePlayDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideEqualizerDao(db: VibePlayDatabase): EqualizerDao = db.equalizerDao()
}
