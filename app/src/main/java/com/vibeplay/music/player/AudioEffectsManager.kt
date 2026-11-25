package com.vibeplay.music.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import com.vibeplay.music.data.local.db.dao.EqualizerDao
import com.vibeplay.music.data.local.db.entity.EqualizerPresetEntity
import com.vibeplay.music.data.local.prefs.UserPreferencesDataStore
import com.vibeplay.music.di.ApplicationScope
import com.vibeplay.music.domain.model.EqualizerBand
import com.vibeplay.music.domain.model.EqualizerPreset
import com.vibeplay.music.domain.model.EqualizerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEffectsManager @Inject constructor(
    private val equalizerDao: EqualizerDao,
    private val prefs: UserPreferencesDataStore,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = 0

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    init {
        scope.launch {
            seedBuiltInPresetsIfNeeded()
            equalizerDao.observePresets().collectLatest { entities ->
                val presets = entities.map { entity ->
                    EqualizerPreset(
                        id = entity.id,
                        name = entity.name,
                        bandLevelsMb = entity.bandLevelsMb,
                        bassBoostStrength = entity.bassBoostStrength,
                        isBuiltIn = entity.isBuiltIn
                    )
                }
                _state.update { it.copy(presets = presets) }
            }
        }
    }

    fun attachToSession(sessionId: Int) {
        if (sessionId <= 0 || sessionId == currentSessionId) return
        releaseEffectsOnly()
        currentSessionId = sessionId

        scope.launch {
            val saved = prefs.getEqualizerPrefsOnce()
            val attached = runCatching {
                val eq = Equalizer(0, sessionId)
                val bb = BassBoost(0, sessionId)

                val bandCount = eq.numberOfBands.toInt().coerceAtLeast(1)
                val range = eq.bandLevelRange
                val minMb = range[0].toInt()
                val maxMb = range[1].toInt()

                val bands = (0 until bandCount).map { idx ->
                    val centerMilliHz = eq.getCenterFreq(idx.toShort())
                    val centerHz = (centerMilliHz / 1000).coerceAtLeast(20)
                    val savedLevel = saved.bandLevelsMb.getOrNull(idx) ?: 0
                    val clamped = clampBandLevel(savedLevel, minMb, maxMb)
                    eq.setBandLevel(idx.toShort(), clamped.toShort())
                    EqualizerBand(
                        index = idx,
                        centerFreqHz = centerHz,
                        label = formatFreqLabel(centerHz),
                        levelMb = clamped,
                        minLevelMb = minMb,
                        maxLevelMb = maxMb
                    )
                }

                eq.enabled = saved.enabled
                if (bb.strengthSupported) {
                    bb.setStrength(saved.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
                bb.enabled = saved.enabled && saved.bassBoostStrength > 0

                equalizer = eq
                bassBoost = bb

                _state.update { current ->
                    current.copy(
                        isAvailable = true,
                        isEnabled = saved.enabled,
                        bands = bands,
                        bassBoostStrength = saved.bassBoostStrength,
                        activePresetName = saved.presetName
                    )
                }
                true
            }.getOrDefault(false)

            if (!attached) {
                releaseEffectsOnly()
                _state.update { current ->
                    current.copy(
                        isAvailable = false,
                        isEnabled = saved.enabled,
                        bands = defaultFallbackBands(saved.bandLevelsMb),
                        bassBoostStrength = saved.bassBoostStrength,
                        activePresetName = saved.presetName
                    )
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        runCatching {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled && _state.value.bassBoostStrength > 0
        }
        _state.update { it.copy(isEnabled = enabled) }
        persistCurrentState()
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val currentBands = _state.value.bands
        if (bandIndex !in currentBands.indices) return
        val targetBand = currentBands[bandIndex]
        val clamped = clampBandLevel(levelMb, targetBand.minLevelMb, targetBand.maxLevelMb)

        runCatching {
            equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort())
        }

        val updatedBands = currentBands.mapIndexed { idx, band ->
            if (idx == bandIndex) band.copy(levelMb = clamped) else band
        }
        _state.update {
            it.copy(
                bands = updatedBands,
                activePresetName = "Custom"
            )
        }
        persistCurrentState()
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
            bassBoost?.enabled = _state.value.isEnabled && clamped > 0
        }
        _state.update {
            it.copy(
                bassBoostStrength = clamped,
                activePresetName = "Custom"
            )
        }
        persistCurrentState()
    }

    fun selectPreset(preset: EqualizerPreset) {
        val currentBands = _state.value.bands.ifEmpty { defaultFallbackBands(emptyList()) }
        val updatedBands = currentBands.mapIndexed { idx, band ->
            val raw = preset.bandLevelsMb.getOrNull(idx) ?: 0
            val clamped = clampBandLevel(raw, band.minLevelMb, band.maxLevelMb)
            runCatching {
                equalizer?.setBandLevel(idx.toShort(), clamped.toShort())
            }
            band.copy(levelMb = clamped)
        }

        val clampedBass = preset.bassBoostStrength.coerceIn(0, 1000)
        runCatching {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clampedBass.toShort())
            }
            bassBoost?.enabled = _state.value.isEnabled && clampedBass > 0
        }

        _state.update {
            it.copy(
                bands = updatedBands,
                bassBoostStrength = clampedBass,
                activePresetName = preset.name
            )
        }
        persistCurrentState()
    }

    fun saveCustomPreset(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val snapshot = _state.value
        val levels = snapshot.bands.map { it.levelMb }
        scope.launch {
            equalizerDao.insertPreset(
                EqualizerPresetEntity(
                    name = cleanName,
                    bandLevelsMb = levels,
                    bassBoostStrength = snapshot.bassBoostStrength,
                    isBuiltIn = false
                )
            )
            _state.update { it.copy(activePresetName = cleanName) }
            persistCurrentState()
        }
    }

    fun deleteCustomPreset(presetId: Long) {
        scope.launch {
            equalizerDao.deleteCustomPreset(presetId)
        }
    }

    fun release() {
        releaseEffectsOnly()
        currentSessionId = 0
    }

    private fun releaseEffectsOnly() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }

    private fun persistCurrentState() {
        val snapshot = _state.value
        scope.launch {
            prefs.saveEqualizerState(
                enabled = snapshot.isEnabled,
                presetName = snapshot.activePresetName,
                bandLevelsMb = snapshot.bands.map { it.levelMb },
                bassBoostStrength = snapshot.bassBoostStrength
            )
        }
    }

    private suspend fun seedBuiltInPresetsIfNeeded() {
        if (equalizerDao.countPresets() > 0) return
        equalizerDao.insertPresets(
            listOf(
                EqualizerPresetEntity(name = "Flat", bandLevelsMb = listOf(0, 0, 0, 0, 0), bassBoostStrength = 0, isBuiltIn = true),
                EqualizerPresetEntity(name = "Bass Boost", bandLevelsMb = listOf(550, 350, 0, 100, 200), bassBoostStrength = 650, isBuiltIn = true),
                EqualizerPresetEntity(name = "Rock", bandLevelsMb = listOf(450, 200, -150, 250, 450), bassBoostStrength = 300, isBuiltIn = true),
                EqualizerPresetEntity(name = "Pop", bandLevelsMb = listOf(-150, 250, 400, 200, -150), bassBoostStrength = 200, isBuiltIn = true),
                EqualizerPresetEntity(name = "Jazz", bandLevelsMb = listOf(350, 200, -150, 250, 400), bassBoostStrength = 250, isBuiltIn = true),
                EqualizerPresetEntity(name = "Classical", bandLevelsMb = listOf(400, 250, -100, 300, 450), bassBoostStrength = 150, isBuiltIn = true)
            )
        )
    }

    private fun defaultFallbackBands(savedLevels: List<Int>): List<EqualizerBand> {
        val defaultFreqs = listOf(60, 230, 910, 3600, 14000)
        return defaultFreqs.mapIndexed { idx, freq ->
            EqualizerBand(
                index = idx,
                centerFreqHz = freq,
                label = formatFreqLabel(freq),
                levelMb = clampBandLevel(savedLevels.getOrNull(idx) ?: 0, -1500, 1500),
                minLevelMb = -1500,
                maxLevelMb = 1500
            )
        }
    }

    companion object {
        fun clampBandLevel(levelMb: Int, minMb: Int, maxMb: Int): Int {
            return levelMb.coerceIn(minMb, maxMb)
        }

        fun formatFreqLabel(freqHz: Int): String {
            return if (freqHz >= 1000) {
                val khz = freqHz / 1000f
                if (khz % 1f == 0f) "${khz.toInt()} kHz" else String.format(Locale.US, "%.1f kHz", khz)
            } else {
                "$freqHz Hz"
            }
        }
    }
}
