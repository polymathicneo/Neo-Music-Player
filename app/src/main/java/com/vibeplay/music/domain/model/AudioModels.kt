package com.vibeplay.music.domain.model

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlaybackState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val audioSessionId: Int = 0
)

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val levelMb: Int,
    val minLevelMb: Int,
    val maxLevelMb: Int
)

data class EqualizerPreset(
    val id: Long = 0L,
    val name: String,
    val bandLevelsMb: List<Int>,
    val bassBoostStrength: Int,
    val isBuiltIn: Boolean = false
)

data class EqualizerState(
    val isAvailable: Boolean = true,
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoostStrength: Int = 0, // 0..1000
    val activePresetName: String = "Flat",
    val presets: List<EqualizerPreset> = emptyList()
)

enum class SleepTimerMode {
    OFF,
    DURATION,
    END_OF_TRACK
}

data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.OFF,
    val remainingMs: Long = 0L,
    val totalMs: Long = 0L,
    val fadeMultiplier: Float = 1f
) {
    val isActive: Boolean get() = mode != SleepTimerMode.OFF
}
