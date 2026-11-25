package com.vibeplay.music.data.repository

import com.vibeplay.music.data.local.prefs.UserPreferencesDataStore
import com.vibeplay.music.domain.model.ScannerSettings
import com.vibeplay.music.domain.model.SongSortConfig
import com.vibeplay.music.domain.model.SongSortOption
import com.vibeplay.music.domain.model.SortDirection
import com.vibeplay.music.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : SettingsRepository {

    override val scannerSettings: Flow<ScannerSettings> = dataStore.scannerSettings
    override val songSortConfig: Flow<SongSortConfig> = dataStore.songSortConfig

    override suspend fun setMinDurationMs(minDurationMs: Long) {
        dataStore.setMinDurationMs(minDurationMs)
    }

    override suspend fun toggleIncludedFolder(folderPath: String) {
        dataStore.toggleIncludedFolder(folderPath)
    }

    override suspend fun clearIncludedFolders() {
        dataStore.clearIncludedFolders()
    }

    override suspend fun toggleExcludedFolder(folderPath: String) {
        dataStore.toggleExcludedFolder(folderPath)
    }

    override suspend fun setCrossfadeDurationSec(seconds: Int) {
        dataStore.setCrossfadeDurationSec(seconds)
    }

    override suspend fun setSongSort(option: SongSortOption, direction: SortDirection) {
        dataStore.setSongSort(option, direction)
    }
}
