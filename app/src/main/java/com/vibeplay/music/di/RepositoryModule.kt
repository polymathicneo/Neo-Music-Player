package com.vibeplay.music.di

import com.vibeplay.music.data.local.lyrics.LyricsRepositoryImpl
import com.vibeplay.music.data.repository.MusicRepositoryImpl
import com.vibeplay.music.data.repository.PlaylistRepositoryImpl
import com.vibeplay.music.data.repository.SettingsRepositoryImpl
import com.vibeplay.music.domain.repository.LyricsRepository
import com.vibeplay.music.domain.repository.MusicRepository
import com.vibeplay.music.domain.repository.PlaylistRepository
import com.vibeplay.music.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(impl: LyricsRepositoryImpl): LyricsRepository
}
