package com.vibeplay.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.vibeplay.music.di.ApplicationScope
import com.vibeplay.music.domain.model.PlaybackState
import com.vibeplay.music.domain.model.RepeatMode
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null
    private var songLookup: Map<Long, Song> = emptyMap()

    private val controllerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromController(player)
        }
    }

    init {
        scope.launch {
            musicRepository.observeAllSongs().collectLatest { songs ->
                songLookup = songs.associateBy { it.id }
                mediaController?.let { syncFromController(it) }
            }
        }
        connect()
    }

    private fun connect() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        scope.launch {
            runCatching {
                val controller = future.await()
                mediaController = controller
                controller.addListener(controllerListener)
                syncFromController(controller)
            }
        }
    }

    private suspend fun ensureController(): MediaController? {
        mediaController?.let { return it }
        val existing = controllerFuture ?: MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        ).buildAsync().also { controllerFuture = it }
        return runCatching {
            existing.await().also { ctrl ->
                if (mediaController !== ctrl) {
                    mediaController = ctrl
                    ctrl.addListener(controllerListener)
                }
            }
        }.getOrNull()
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playQueue(queue.ifEmpty { listOf(song) }, startIndex = index, shuffle = false)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        scope.launch {
            val controller = ensureController() ?: return@launch
            val items = songs.map { it.toMediaItem() }
            val safeIndex = startIndex.coerceIn(0, items.lastIndex)
            controller.shuffleModeEnabled = shuffle
            controller.setMediaItems(items, safeIndex, 0L)
            controller.prepare()
            controller.play()
            syncFromController(controller)
        }
    }

    fun togglePlayPause() {
        scope.launch {
            val controller = ensureController() ?: return@launch
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.mediaItemCount == 0 && songLookup.isNotEmpty()) {
                    val all = songLookup.values.toList()
                    playQueue(all, 0, false)
                } else {
                    controller.play()
                }
            }
        }
    }

    fun skipNext() {
        scope.launch {
            val controller = ensureController() ?: return@launch
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            } else if (controller.mediaItemCount > 0) {
                controller.seekTo(0, 0L)
            }
        }
    }

    fun skipPrevious() {
        scope.launch {
            val controller = ensureController() ?: return@launch
            if (controller.currentPosition > 3_000L) {
                controller.seekTo(0L)
            } else if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
            } else {
                controller.seekTo(0L)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        scope.launch {
            val controller = ensureController() ?: return@launch
            val target = positionMs.coerceAtLeast(0L)
            controller.seekTo(target)
            _playbackState.update { it.copy(positionMs = target) }
        }
    }

    fun toggleShuffle() {
        scope.launch {
            val controller = ensureController() ?: return@launch
            controller.shuffleModeEnabled = !controller.shuffleModeEnabled
            syncFromController(controller)
        }
    }

    fun cycleRepeatMode() {
        scope.launch {
            val controller = ensureController() ?: return@launch
            controller.repeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            syncFromController(controller)
        }
    }

    fun playNext(song: Song) {
        scope.launch {
            val controller = ensureController() ?: return@launch
            if (controller.mediaItemCount == 0) {
                playSong(song)
            } else {
                val insertIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount)
                controller.addMediaItem(insertIndex, song.toMediaItem())
                syncFromController(controller)
            }
        }
    }

    fun addToQueue(song: Song) {
        scope.launch {
            val controller = ensureController() ?: return@launch
            if (controller.mediaItemCount == 0) {
                playSong(song)
            } else {
                controller.addMediaItem(song.toMediaItem())
                syncFromController(controller)
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        scope.launch {
            val controller = ensureController() ?: return@launch
            if (fromIndex in 0 until controller.mediaItemCount &&
                toIndex in 0 until controller.mediaItemCount &&
                fromIndex != toIndex
            ) {
                controller.moveMediaItem(fromIndex, toIndex)
                syncFromController(controller)
            }
        }
    }

    fun removeQueueItem(index: Int) {
        scope.launch {
            val controller = ensureController() ?: return@launch
            if (index in 0 until controller.mediaItemCount) {
                controller.removeMediaItem(index)
                syncFromController(controller)
            }
        }
    }

    fun playQueueIndex(index: Int) {
        scope.launch {
            val controller = ensureController() ?: return@launch
            if (index in 0 until controller.mediaItemCount) {
                controller.seekTo(index, 0L)
                controller.play()
            }
        }
    }

    private fun syncFromController(controller: Player) {
        val count = controller.mediaItemCount
        val queue = (0 until count).mapNotNull { idx ->
            val item = controller.getMediaItemAt(idx)
            val id = item.mediaId.toLongOrNull() ?: return@mapNotNull null
            songLookup[id] ?: item.toFallbackSong(id)
        }
        val currentIdx = controller.currentMediaItemIndex.takeIf { it in queue.indices } ?: -1
        val currentSong = queue.getOrNull(currentIdx)
        val rawDuration = controller.duration
        val durationMs = if (rawDuration != C.TIME_UNSET && rawDuration > 0L) {
            rawDuration
        } else {
            currentSong?.durationMs ?: 0L
        }

        val repeat = when (controller.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }

        _playbackState.value = PlaybackState(
            currentSong = currentSong,
            queue = queue,
            currentIndex = currentIdx,
            isPlaying = controller.isPlaying,
            positionMs = controller.currentPosition.coerceAtLeast(0L),
            durationMs = durationMs,
            shuffleEnabled = controller.shuffleModeEnabled,
            repeatMode = repeat
        )

        if (controller.isPlaying) {
            startPositionTicker()
        } else {
            positionTickerJob?.cancel()
        }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                val controller = mediaController ?: break
                if (!controller.isPlaying) break
                val pos = controller.currentPosition.coerceAtLeast(0L)
                val dur = controller.duration
                _playbackState.update { state ->
                    state.copy(
                        positionMs = pos,
                        durationMs = if (dur != C.TIME_UNSET && dur > 0L) dur else state.durationMs
                    )
                }
                delay(250L)
            }
        }
    }

    private fun Song.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putString(PlaybackService.EXTRA_CONTENT_URI, contentUri)
            putString("artist", artist)
            putString("album", album)
            putLong("albumId", albumId)
            putLong("durationMs", durationMs)
            putString("filePath", filePath)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri?.let(Uri::parse))
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.parse(contentUri))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toFallbackSong(id: Long): Song {
        val meta = mediaMetadata
        val extras = meta.extras
        val content = extras?.getString(PlaybackService.EXTRA_CONTENT_URI)
            ?: localConfiguration?.uri?.toString().orEmpty()
        return Song(
            id = id,
            title = meta.title?.toString() ?: "Unknown Track",
            artist = meta.artist?.toString() ?: "Unknown Artist",
            album = meta.albumTitle?.toString() ?: "Unknown Album",
            albumId = extras?.getLong("albumId") ?: 0L,
            durationMs = extras?.getLong("durationMs") ?: 0L,
            contentUri = content,
            filePath = extras?.getString("filePath").orEmpty(),
            folderPath = "",
            folderName = "",
            dateAdded = 0L,
            trackNumber = 0,
            year = 0,
            mimeType = "",
            sizeBytes = 0L,
            albumArtUri = meta.artworkUri?.toString()
        )
    }
}
