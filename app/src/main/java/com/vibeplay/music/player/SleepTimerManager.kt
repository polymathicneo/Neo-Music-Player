package com.vibeplay.music.player

import com.vibeplay.music.di.ApplicationScope
import com.vibeplay.music.domain.model.SleepTimerMode
import com.vibeplay.music.domain.model.SleepTimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimerManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var onPausePlayback: (() -> Unit)? = null
    private var onFadeVolumeChanged: ((Float) -> Unit)? = null

    fun registerCallbacks(
        onPause: () -> Unit,
        onFadeChanged: (Float) -> Unit
    ) {
        onPausePlayback = onPause
        onFadeVolumeChanged = onFadeChanged
    }

    fun startDurationTimer(minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 360)
        val totalMs = safeMinutes * 60_000L
        timerJob?.cancel()

        _state.value = SleepTimerState(
            mode = SleepTimerMode.DURATION,
            remainingMs = totalMs,
            totalMs = totalMs,
            fadeMultiplier = 1f
        )
        onFadeVolumeChanged?.invoke(1f)

        val endTime = System.currentTimeMillis() + totalMs
        timerJob = scope.launch {
            while (isActive) {
                val remaining = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
                val multiplier = computeSleepFadeMultiplier(remaining, FADE_OUT_WINDOW_MS)
                _state.value = SleepTimerState(
                    mode = SleepTimerMode.DURATION,
                    remainingMs = remaining,
                    totalMs = totalMs,
                    fadeMultiplier = multiplier
                )
                onFadeVolumeChanged?.invoke(multiplier)

                if (remaining <= 0L) {
                    onPausePlayback?.invoke()
                    cancelTimer()
                    break
                }
                delay(150L)
            }
        }
    }

    fun startEndOfTrackTimer() {
        timerJob?.cancel()
        _state.value = SleepTimerState(
            mode = SleepTimerMode.END_OF_TRACK,
            remainingMs = 0L,
            totalMs = 0L,
            fadeMultiplier = 1f
        )
        onFadeVolumeChanged?.invoke(1f)
    }

    fun updateEndOfTrackProgress(positionMs: Long, durationMs: Long) {
        if (_state.value.mode != SleepTimerMode.END_OF_TRACK) return
        if (durationMs <= 0L) return
        val remaining = (durationMs - positionMs).coerceAtLeast(0L)
        val multiplier = computeSleepFadeMultiplier(remaining, FADE_OUT_WINDOW_MS)
        _state.value = _state.value.copy(
            remainingMs = remaining,
            totalMs = durationMs,
            fadeMultiplier = multiplier
        )
        onFadeVolumeChanged?.invoke(multiplier)
    }

    fun onTrackEndedForEndOfTrackMode(): Boolean {
        if (_state.value.mode != SleepTimerMode.END_OF_TRACK) return false
        onPausePlayback?.invoke()
        cancelTimer()
        return true
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState()
        onFadeVolumeChanged?.invoke(1f)
    }

    companion object {
        const val FADE_OUT_WINDOW_MS = 5_000L

        fun computeSleepFadeMultiplier(remainingMs: Long, fadeWindowMs: Long = FADE_OUT_WINDOW_MS): Float {
            if (remainingMs <= 0L) return 0f
            if (fadeWindowMs <= 0L || remainingMs >= fadeWindowMs) return 1f
            return (remainingMs.toFloat() / fadeWindowMs.toFloat()).coerceIn(0f, 1f)
        }
    }
}
