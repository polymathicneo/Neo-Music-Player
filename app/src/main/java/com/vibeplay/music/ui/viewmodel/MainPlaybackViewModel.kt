package com.vibeplay.music.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.vibeplay.music.domain.model.LyricsData
import com.vibeplay.music.domain.model.PlaybackState
import com.vibeplay.music.domain.model.Playlist
import com.vibeplay.music.domain.model.SleepTimerState
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.domain.repository.LyricsRepository
import com.vibeplay.music.domain.repository.MusicRepository
import com.vibeplay.music.domain.repository.PlaylistRepository
import com.vibeplay.music.player.MusicPlayerController
import com.vibeplay.music.player.SleepTimerManager
import com.vibeplay.music.ui.theme.VibeSurfaceElevated
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainPlaybackViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerController: MusicPlayerController,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val lyricsRepository: LyricsRepository,
    private val sleepTimerManager: SleepTimerManager
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState
    val sleepTimerState: StateFlow<SleepTimerState> = sleepTimerManager.state

    val playlists: StateFlow<List<Playlist>> = playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _dominantColor = MutableStateFlow(VibeSurfaceElevated)
    val dominantColor: StateFlow<Color> = _dominantColor.asStateFlow()

    private val _lyricsData = MutableStateFlow(LyricsData())
    val lyricsData: StateFlow<LyricsData> = _lyricsData.asStateFlow()

    init {
        viewModelScope.launch {
            playbackState
                .distinctUntilChangedBy { it.currentSong?.id }
                .collectLatest { state ->
                    val song = state.currentSong
                    if (song == null) {
                        _dominantColor.value = VibeSurfaceElevated
                        _lyricsData.value = LyricsData()
                    } else {
                        launch { _dominantColor.value = extractDominantColor(song) }
                        launch { _lyricsData.value = lyricsRepository.loadLyricsForSong(song) }
                    }
                }
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            if (musicRepository.getAllSongsOnce().isEmpty()) {
                musicRepository.rescanLibrary()
            }
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) =
        playerController.playSong(song, queue)

    fun playQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) =
        playerController.playQueue(songs, startIndex, shuffle)

    fun togglePlayPause() = playerController.togglePlayPause()
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()
    fun playNext(song: Song) = playerController.playNext(song)
    fun addToQueue(song: Song) = playerController.addToQueue(song)
    fun moveQueueItem(from: Int, to: Int) = playerController.moveQueueItem(from, to)
    fun removeQueueItem(index: Int) = playerController.removeQueueItem(index)
    fun playQueueIndex(index: Int) = playerController.playQueueIndex(index)

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(songId)
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long?) {
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            if (songId != null) {
                playlistRepository.addSongToPlaylist(playlistId, songId)
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun startSleepTimerMinutes(minutes: Int) = sleepTimerManager.startDurationTimer(minutes)
    fun startSleepTimerEndOfTrack() = sleepTimerManager.startEndOfTrackTimer()
    fun cancelSleepTimer() = sleepTimerManager.cancelTimer()

    private suspend fun extractDominantColor(song: Song): Color = withContext(Dispatchers.IO) {
        val bitmap = loadSmallBitmap(song) ?: return@withContext VibeSurfaceElevated
        try {
            val palette = Palette.from(bitmap).generate()
            val rgb = palette.getVibrantColor(
                palette.getDarkVibrantColor(
                    palette.getMutedColor(
                        palette.getDominantColor(0xFF282828.toInt())
                    )
                )
            )
            Color(rgb)
        } catch (_: Throwable) {
            VibeSurfaceElevated
        } finally {
            bitmap.recycle()
        }
    }

    private fun loadSmallBitmap(song: Song): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(song.contentUri))
            val embedded = retriever.embeddedPicture
            if (embedded != null && embedded.isNotEmpty()) {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                return BitmapFactory.decodeByteArray(embedded, 0, embedded.size, opts)
            }
        } catch (_: Throwable) {
            // Fall through to MediaStore album art
        } finally {
            runCatching { retriever.release() }
        }

        val artUri = song.albumArtUri ?: return null
        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(artUri))?.use { stream ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()
    }
}
