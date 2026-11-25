package com.vibeplay.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeplay.music.domain.model.Album
import com.vibeplay.music.domain.model.Artist
import com.vibeplay.music.domain.model.MusicFolder
import com.vibeplay.music.domain.model.Playlist
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.domain.model.SongSortConfig
import com.vibeplay.music.domain.model.SongSortOption
import com.vibeplay.music.domain.model.SongSorter
import com.vibeplay.music.domain.repository.MusicRepository
import com.vibeplay.music.domain.repository.PlaylistRepository
import com.vibeplay.music.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    PLAYLISTS("Playlists"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    FOLDERS("Folders"),
    LIKED_SONGS("Liked Songs")
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.SONGS,
    val sortConfig: SongSortConfig = SongSortConfig(),
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val folders: List<MusicFolder> = emptyList(),
    val likedSongs: List<Song> = emptyList()
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LibraryTab.SONGS)

    val uiState: StateFlow<LibraryUiState> = combine(
        _selectedTab,
        settingsRepository.songSortConfig,
        musicRepository.observeAllSongs(),
        playlistRepository.observePlaylists(),
        musicRepository.observeArtists(),
        musicRepository.observeAlbums(),
        musicRepository.observeFolders(),
        musicRepository.observeLikedSongs()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val sortConfig = values[1] as SongSortConfig
        @Suppress("UNCHECKED_CAST")
        val allSongs = values[2] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val likedSongs = values[7] as List<Song>

        @Suppress("UNCHECKED_CAST")
        LibraryUiState(
            selectedTab = values[0] as LibraryTab,
            sortConfig = sortConfig,
            songs = SongSorter.sort(allSongs, sortConfig),
            playlists = values[3] as List<Playlist>,
            artists = values[4] as List<Artist>,
            albums = values[5] as List<Album>,
            folders = values[6] as List<MusicFolder>,
            likedSongs = SongSorter.sort(likedSongs, sortConfig)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun selectSortOption(option: SongSortOption) {
        val current = uiState.value.sortConfig
        val nextDirection = if (current.option == option) {
            current.direction.toggle()
        } else {
            option.defaultDirection
        }
        viewModelScope.launch {
            settingsRepository.setSongSort(option, nextDirection)
        }
    }

    fun toggleSortDirection() {
        val current = uiState.value.sortConfig
        viewModelScope.launch {
            settingsRepository.setSongSort(current.option, current.direction.toggle())
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }
}
