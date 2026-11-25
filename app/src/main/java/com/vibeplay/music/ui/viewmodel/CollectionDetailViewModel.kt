package com.vibeplay.music.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.domain.model.SongSortConfig
import com.vibeplay.music.domain.model.SongSortOption
import com.vibeplay.music.domain.model.SongSorter
import com.vibeplay.music.domain.model.SortDirection
import com.vibeplay.music.domain.repository.MusicRepository
import com.vibeplay.music.domain.repository.PlaylistRepository
import com.vibeplay.music.ui.navigation.CollectionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionDetailUiState(
    val type: CollectionType = CollectionType.LIKED_SONGS,
    val title: String = "",
    val subtitle: String = "",
    val songs: List<Song> = emptyList(),
    val sortConfig: SongSortConfig = SongSortConfig(),
    val playlistId: Long? = null
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val typeArg: CollectionType = runCatching {
        CollectionType.valueOf(savedStateHandle.get<String>("type").orEmpty())
    }.getOrDefault(CollectionType.LIKED_SONGS)

    private val idArg: String = Uri.decode(savedStateHandle.get<String>("id").orEmpty())
    private val titleArg: String = Uri.decode(savedStateHandle.get<String>("title").orEmpty())

    private val defaultSortConfig = when (typeArg) {
        CollectionType.PLAYLIST, CollectionType.ALBUM -> SongSortConfig(
            option = SongSortOption.DEFAULT,
            direction = SortDirection.ASCENDING
        )
        else -> SongSortConfig(
            option = SongSortOption.DATE_ADDED,
            direction = SortDirection.DESCENDING
        )
    }

    private val _sortConfig = MutableStateFlow(defaultSortConfig)

    private val baseStateFlow = when (typeArg) {
        CollectionType.PLAYLIST -> {
            val playlistId = idArg.toLongOrNull() ?: -1L
            playlistRepository.observePlaylistWithSongs(playlistId).map { data ->
                val songs = data?.songs.orEmpty()
                CollectionDetailUiState(
                    type = CollectionType.PLAYLIST,
                    title = data?.playlist?.name ?: titleArg,
                    subtitle = "${songs.size} songs",
                    songs = songs,
                    playlistId = playlistId
                )
            }
        }
        CollectionType.ALBUM -> {
            val albumId = idArg.toLongOrNull() ?: -1L
            musicRepository.observeSongsByAlbum(albumId).map { songs ->
                CollectionDetailUiState(
                    type = CollectionType.ALBUM,
                    title = titleArg.ifBlank { songs.firstOrNull()?.album ?: "Album" },
                    subtitle = songs.firstOrNull()?.artist ?: "${songs.size} songs",
                    songs = songs
                )
            }
        }
        CollectionType.ARTIST -> {
            musicRepository.observeSongsByArtist(idArg).map { songs ->
                CollectionDetailUiState(
                    type = CollectionType.ARTIST,
                    title = titleArg.ifBlank { idArg },
                    subtitle = "${songs.size} songs",
                    songs = songs
                )
            }
        }
        CollectionType.FOLDER -> {
            musicRepository.observeSongsByFolder(idArg).map { songs ->
                CollectionDetailUiState(
                    type = CollectionType.FOLDER,
                    title = titleArg.ifBlank { idArg.substringAfterLast('/') },
                    subtitle = "${songs.size} songs • $idArg",
                    songs = songs
                )
            }
        }
        CollectionType.LIKED_SONGS -> {
            musicRepository.observeLikedSongs().map { songs ->
                CollectionDetailUiState(
                    type = CollectionType.LIKED_SONGS,
                    title = "Liked Songs",
                    subtitle = "${songs.size} songs",
                    songs = songs
                )
            }
        }
    }

    val uiState: StateFlow<CollectionDetailUiState> = combine(
        baseStateFlow,
        _sortConfig
    ) { base, sortConfig ->
        base.copy(
            songs = SongSorter.sort(base.songs, sortConfig),
            sortConfig = sortConfig
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CollectionDetailUiState(
            type = typeArg,
            title = titleArg,
            sortConfig = defaultSortConfig
        )
    )

    fun selectSortOption(option: SongSortOption) {
        _sortConfig.update { current ->
            if (current.option == option) {
                current.copy(direction = current.direction.toggle())
            } else {
                SongSortConfig(option = option, direction = option.defaultDirection)
            }
        }
    }

    fun toggleSortDirection() {
        _sortConfig.update { current ->
            current.copy(direction = current.direction.toggle())
        }
    }

    fun renamePlaylist(newName: String) {
        val pid = uiState.value.playlistId ?: return
        viewModelScope.launch {
            playlistRepository.renamePlaylist(pid, newName)
        }
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        val pid = uiState.value.playlistId ?: return
        viewModelScope.launch {
            playlistRepository.deletePlaylist(pid)
            onDeleted()
        }
    }

    fun removeSongFromPlaylist(songId: Long) {
        val pid = uiState.value.playlistId ?: return
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(pid, songId)
        }
    }
}
