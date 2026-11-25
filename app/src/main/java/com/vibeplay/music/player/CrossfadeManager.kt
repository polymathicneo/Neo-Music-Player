package com.vibeplay.music.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CrossfadeManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPrimaryVolumeScaleChanged: (Float) -> Unit
) {
    private var crossfadeSec: Int = 0
    private var monitorJob: Job? = null
    private var fadeJob: Job? = null
    private var fadingOutPlayer: ExoPlayer? = null
    private var isCrossfading: Boolean = false

    fun setCrossfadeDurationSec(seconds: Int) {
        crossfadeSec = seconds.coerceIn(0, 12)
        if (crossfadeSec == 0 && isCrossfading) {
            cancelActiveFade()
        }
    }

    fun onPlaybackStateUpdated(player: ExoPlayer) {
        if (player.isPlaying && crossfadeSec > 0) {
            startMonitor(player)
        } else {
            stopMonitor()
            if (!player.isPlaying && isCrossfading) {
                cancelActiveFade()
            }
        }
    }

    fun cancelActiveFade() {
        fadeJob?.cancel()
        fadeJob = null
        releaseFadingOutPlayer()
        isCrossfading = false
        onPrimaryVolumeScaleChanged(1f)
    }

    fun release() {
        stopMonitor()
        cancelActiveFade()
    }

    private fun startMonitor(player: ExoPlayer) {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                val cfMs = crossfadeSec * 1000L
                if (cfMs > 0 &&
                    player.isPlaying &&
                    !isCrossfading &&
                    player.repeatMode != Player.REPEAT_MODE_ONE &&
                    player.hasNextMediaItem()
                ) {
                    val duration = player.duration
                    val position = player.currentPosition
                    if (duration != C.TIME_UNSET && duration > cfMs * 2) {
                        val remaining = duration - position
                        if (remaining in 150L..cfMs) {
                            triggerCrossfade(player, cfMs)
                        }
                    }
                }
                delay(50L)
            }
        }
    }

    private fun stopMonitor() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun triggerCrossfade(primaryPlayer: ExoPlayer, durationMs: Long) {
        val currentItem = primaryPlayer.currentMediaItem ?: return
        val outgoingPos = primaryPlayer.currentPosition.coerceAtLeast(0L)

        isCrossfading = true
        fadeJob?.cancel()
        releaseFadingOutPlayer()

        val secondary = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false // Do not steal audio focus from primary player
            )
            .build()
        fadingOutPlayer = secondary
        secondary.setMediaItem(currentItem)
        secondary.seekTo(outgoingPos)
        secondary.volume = 1f
        secondary.prepare()
        secondary.playWhenReady = true

        onPrimaryVolumeScaleChanged(0f)
        primaryPlayer.seekToNextMediaItem()

        fadeJob = scope.launch {
            val startTime = System.currentTimeMillis()
            try {
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    val (outgoingVol, incomingVol) = computeFadeCurve(progress)
                    secondary.volume = outgoingVol
                    onPrimaryVolumeScaleChanged(incomingVol)
                    if (progress >= 1f) break
                    delay(35L)
                }
            } finally {
                releaseFadingOutPlayer()
                isCrossfading = false
                onPrimaryVolumeScaleChanged(1f)
            }
        }
    }

    private fun releaseFadingOutPlayer() {
        runCatching {
            fadingOutPlayer?.stop()
            fadingOutPlayer?.release()
        }
        fadingOutPlayer = null
    }

    companion object {
        /**
         * Equal-power crossfade curve in [0f, 1f].
         * Returns Pair(outgoingVolume, incomingVolume).
         */
        fun computeFadeCurve(progress: Float): Pair<Float, Float> {
            val p = progress.coerceIn(0f, 1f)
            val angle = p * (PI / 2.0)
            val outgoing = cos(angle).toFloat().coerceIn(0f, 1f)
            val incoming = sin(angle).toFloat().coerceIn(0f, 1f)
            return outgoing to incoming
        }
    }
}
