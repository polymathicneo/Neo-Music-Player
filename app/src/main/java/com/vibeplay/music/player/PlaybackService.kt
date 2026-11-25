package com.vibeplay.music.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vibeplay.music.MainActivity
import com.vibeplay.music.data.local.prefs.UserPreferencesDataStore
import com.vibeplay.music.data.local.prefs.WidgetPlaybackSnapshot
import com.vibeplay.music.domain.repository.MusicRepository
import com.vibeplay.music.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var audioEffectsManager: AudioEffectsManager
    @Inject lateinit var sleepTimerManager: SleepTimerManager
    @Inject lateinit var musicRepository: MusicRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var prefs: UserPreferencesDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var player: ExoPlayer
    private lateinit var crossfadeManager: CrossfadeManager
    private var mediaSession: MediaSession? = null

    private var crossfadeScale: Float = 1f
    private var sleepFadeScale: Float = 1f
    private var playCountJob: Job? = null
    private var endOfTrackMonitorJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            audioEffectsManager.attachToSession(audioSessionId)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                sleepTimerManager.onTrackEndedForEndOfTrackMode()
            ) {
                return
            }
            handleTrackTransition(mediaItem)
            publishWidgetSnapshot()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            crossfadeManager.onPlaybackStateUpdated(player)
            if (isPlaying) {
                startEndOfTrackMonitor()
            } else {
                endOfTrackMonitorJob?.cancel()
            }
            publishWidgetSnapshot()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                sleepTimerManager.onTrackEndedForEndOfTrackMode()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        crossfadeManager = CrossfadeManager(
            context = this,
            scope = serviceScope,
            onPrimaryVolumeScaleChanged = { scale ->
                crossfadeScale = scale
                applyCombinedVolume()
            }
        )

        sleepTimerManager.registerCallbacks(
            onPause = {
                player.pause()
            },
            onFadeChanged = { scale ->
                sleepFadeScale = scale
                applyCombinedVolume()
            }
        )

        player.addListener(playerListener)
        if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            audioEffectsManager.attachToSession(player.audioSessionId)
        }

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_NOW_PLAYING, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()

        serviceScope.launch {
            settingsRepository.scannerSettings.collectLatest { settings ->
                crossfadeManager.setCrossfadeDurationSec(settings.crossfadeDurationSec)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_WIDGET_PLAY_PAUSE -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    if (player.mediaItemCount > 0) {
                        player.play()
                    } else {
                        launchAppFromWidget()
                    }
                }
            }
            ACTION_WIDGET_NEXT -> {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                }
            }
            ACTION_WIDGET_PREV -> {
                if (player.currentPosition > 3_000L) {
                    player.seekTo(0L)
                } else if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player
        if (currentPlayer == null || !currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        playCountJob?.cancel()
        endOfTrackMonitorJob?.cancel()
        crossfadeManager.release()
        audioEffectsManager.release()
        player.removeListener(playerListener)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun applyCombinedVolume() {
        player.volume = (crossfadeScale * sleepFadeScale).coerceIn(0f, 1f)
    }

    private fun handleTrackTransition(mediaItem: MediaItem?) {
        playCountJob?.cancel()
        val songId = mediaItem?.mediaId?.toLongOrNull() ?: return
        serviceScope.launch(Dispatchers.IO) {
            musicRepository.recordLastPlayed(songId)
        }
        playCountJob = serviceScope.launch {
            delay(5_000L)
            if (player.isPlaying && player.currentMediaItem?.mediaId?.toLongOrNull() == songId) {
                launch(Dispatchers.IO) {
                    musicRepository.incrementPlayCount(songId)
                }
            }
        }
    }

    private fun startEndOfTrackMonitor() {
        endOfTrackMonitorJob?.cancel()
        endOfTrackMonitorJob = serviceScope.launch {
            while (isActive) {
                sleepTimerManager.updateEndOfTrackProgress(
                    positionMs = player.currentPosition,
                    durationMs = player.duration
                )
                delay(200L)
            }
        }
    }

    private fun publishWidgetSnapshot() {
        val current = player.currentMediaItem
        val songId = current?.mediaId?.toLongOrNull() ?: -1L
        val title = current?.mediaMetadata?.title?.toString() ?: "Nothing playing"
        val artist = current?.mediaMetadata?.artist?.toString() ?: "Tap to open NeoMusic"
        val contentUri = current?.mediaMetadata?.extras?.getString(EXTRA_CONTENT_URI)
            ?: current?.localConfiguration?.uri?.toString()
            ?: ""
        val artUri = current?.mediaMetadata?.artworkUri?.toString().orEmpty()
        val playing = player.isPlaying

        serviceScope.launch(Dispatchers.IO) {
            prefs.updateWidgetSnapshot(
                WidgetPlaybackSnapshot(
                    songId = songId,
                    title = title,
                    artist = artist,
                    contentUri = contentUri,
                    albumArtUri = artUri,
                    isPlaying = playing
                )
            )
            sendBroadcast(
                Intent(ACTION_REFRESH_WIDGET).setPackage(packageName)
            )
        }
    }

    private fun launchAppFromWidget() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
    }

    companion object {
        const val EXTRA_OPEN_NOW_PLAYING = "com.vibeplay.music.EXTRA_OPEN_NOW_PLAYING"
        const val EXTRA_CONTENT_URI = "com.vibeplay.music.EXTRA_CONTENT_URI"
        const val ACTION_WIDGET_PLAY_PAUSE = "com.vibeplay.music.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.vibeplay.music.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.vibeplay.music.ACTION_WIDGET_PREV"
        const val ACTION_REFRESH_WIDGET = "com.vibeplay.music.ACTION_REFRESH_WIDGET"
    }
}
