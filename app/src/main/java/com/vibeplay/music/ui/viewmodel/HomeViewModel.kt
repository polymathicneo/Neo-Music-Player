package com.vibeplay.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeplay.music.domain.model.HomeQuickItem
import com.vibeplay.music.domain.model.QuickItemType
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.domain.repository.MusicRepository
import com.vibeplay.music.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val greeting: String = "Good evening",
    val quickItems: List<HomeQuickItem> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val allSongsCount: Int = 0,
    val isScanning: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    playlistRepository: PlaylistRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        musicRepository.observeAllSongs(),
        musicRepository.observeRecentlyPlayed(16),
        musicRepository.observeRecentlyAdded(15),
        musicRepository.observeMostPlayed(12),
        playlistRepository.observePlaylists(),
        musicRepository.observeLikedSongs(),
        musicRepository.isScanning
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allSongs = values[0] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val recentlyPlayed = values[1] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val recentlyAdded = values[2] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val mostPlayed = values[3] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val playlists = values[4] as List<com.vibeplay.music.domain.model.Playlist>
        @Suppress("UNCHECKED_CAST")
        val likedSongs = values[5] as List<Song>
        val scanning = values[6] as Boolean

        val quickItems = buildList {
            if (likedSongs.isNotEmpty() || allSongs.isNotEmpty()) {
                val firstLiked = likedSongs.firstOrNull()
                add(
                    HomeQuickItem(
                        id = "liked_songs",
                        title = "Liked Songs",
                        subtitle = "${likedSongs.size} songs",
                        type = QuickItemType.LIKED_SONGS,
                        targetId = "liked",
                        songId = firstLiked?.id,
                        contentUri = firstLiked?.contentUri,
                        artworkUri = firstLiked?.albumArtUri
                    )
                )
            }

            playlists.take(3).forEach { playlist ->
                add(
                    HomeQuickItem(
                        id = "playlist_${playlist.id}",
                        title = playlist.name,
                        subtitle = "${playlist.songCount} songs",
                        type = QuickItemType.PLAYLIST,
                        targetId = playlist.id.toString(),
                        songId = playlist.coverSongId,
                        contentUri = playlist.coverContentUri,
                        artworkUri = playlist.coverArtUri
                    )
                )
            }

            val recentAlbums = recentlyPlayed
                .distinctBy { it.albumId }
                .ifEmpty { allSongs.distinctBy { it.albumId } }

            for (song in recentAlbums) {
                if (size >= 6) break
                add(
                    HomeQuickItem(
                        id = "album_${song.albumId}",
                        title = song.album,
                        subtitle = song.artist,
                        type = QuickItemType.ALBUM,
                        targetId = song.albumId.toString(),
                        songId = song.id,
                        contentUri = song.contentUri,
                        artworkUri = song.albumArtUri
                    )
                )
            }
        }.take(6)

        HomeUiState(
            greeting = computeGreeting(),
            quickItems = quickItems,
            recentlyAdded = recentlyAdded,
            mostPlayed = mostPlayed.ifEmpty { allSongs.take(8) },
            allSongsCount = allSongs.size,
            isScanning = scanning
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(greeting = computeGreeting()))

    fun rescanLibrary() {
        viewModelScope.launch {
            musicRepository.rescanLibrary()
        }
    }

    private fun computeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
