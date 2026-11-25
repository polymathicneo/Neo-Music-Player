package com.vibeplay.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeplay.music.domain.model.DiscoveredFolder
import com.vibeplay.music.domain.model.ScannerSettings
import com.vibeplay.music.domain.repository.MusicRepository
import com.vibeplay.music.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: ScannerSettings = ScannerSettings(),
    val discoveredFolders: List<DiscoveredFolder> = emptyList(),
    val totalSongsCount: Int = 0,
    val isScanning: Boolean = false,
    val lastScanMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _lastScanMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.scannerSettings,
        musicRepository.discoveredFolders,
        musicRepository.observeAllSongs(),
        musicRepository.isScanning,
        _lastScanMessage
    ) { settings, folders, songs, scanning, msg ->
        SettingsUiState(
            settings = settings,
            discoveredFolders = folders,
            totalSongsCount = songs.size,
            isScanning = scanning,
            lastScanMessage = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun rescanLibrary() {
        viewModelScope.launch {
            val result = musicRepository.rescanLibrary()
            _lastScanMessage.value = "Scanned ${result.importedCount} tracks across ${result.discoveredFolders.size} folders"
        }
    }

    fun setMinDurationSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setMinDurationMs(seconds * 1000L)
            musicRepository.rescanLibrary()
        }
    }

    fun toggleIncludedFolder(path: String) {
        viewModelScope.launch {
            settingsRepository.toggleIncludedFolder(path)
            musicRepository.rescanLibrary()
        }
    }

    fun clearIncludedFolders() {
        viewModelScope.launch {
            settingsRepository.clearIncludedFolders()
            musicRepository.rescanLibrary()
        }
    }

    fun toggleExcludedFolder(path: String) {
        viewModelScope.launch {
            settingsRepository.toggleExcludedFolder(path)
            musicRepository.rescanLibrary()
        }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setCrossfadeDurationSec(seconds)
        }
    }
}
