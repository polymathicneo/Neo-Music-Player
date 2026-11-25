package com.vibeplay.music.domain.repository

import com.vibeplay.music.domain.model.ScannerSettings
import com.vibeplay.music.domain.model.SongSortConfig
import com.vibeplay.music.domain.model.SongSortOption
import com.vibeplay.music.domain.model.SortDirection
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val scannerSettings: Flow<ScannerSettings>
    val songSortConfig: Flow<SongSortConfig>

    suspend fun setMinDurationMs(minDurationMs: Long)
    suspend fun toggleIncludedFolder(folderPath: String)
    suspend fun clearIncludedFolders()
    suspend fun toggleExcludedFolder(folderPath: String)
    suspend fun setCrossfadeDurationSec(seconds: Int)
    suspend fun setSongSort(option: SongSortOption, direction: SortDirection)
}
