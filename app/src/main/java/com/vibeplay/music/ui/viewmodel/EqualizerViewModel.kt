package com.vibeplay.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.vibeplay.music.domain.model.EqualizerPreset
import com.vibeplay.music.domain.model.EqualizerState
import com.vibeplay.music.player.AudioEffectsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val audioEffectsManager: AudioEffectsManager
) : ViewModel() {

    val uiState: StateFlow<EqualizerState> = audioEffectsManager.state

    fun setEnabled(enabled: Boolean) = audioEffectsManager.setEnabled(enabled)
    fun setBandLevel(bandIndex: Int, levelMb: Int) = audioEffectsManager.setBandLevel(bandIndex, levelMb)
    fun setBassBoost(strength: Int) = audioEffectsManager.setBassBoost(strength)
    fun selectPreset(preset: EqualizerPreset) = audioEffectsManager.selectPreset(preset)
    fun saveCustomPreset(name: String) = audioEffectsManager.saveCustomPreset(name)
    fun deleteCustomPreset(presetId: Long) = audioEffectsManager.deleteCustomPreset(presetId)
}
