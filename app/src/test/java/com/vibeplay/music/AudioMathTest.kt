package com.vibeplay.music

import com.vibeplay.music.player.AudioEffectsManager
import com.vibeplay.music.player.CrossfadeManager
import com.vibeplay.music.player.SleepTimerManager
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioMathTest {

    @Test
    fun crossfadeCurve_smoothlyTransitionsZeroToOne() {
        val (outStart, inStart) = CrossfadeManager.computeFadeCurve(0f)
        assertEquals(1f, outStart, 0.001f)
        assertEquals(0f, inStart, 0.001f)

        val (outMid, inMid) = CrossfadeManager.computeFadeCurve(0.5f)
        assertEquals(inMid, outMid, 0.01f)

        val (outEnd, inEnd) = CrossfadeManager.computeFadeCurve(1f)
        assertEquals(0f, outEnd, 0.001f)
        assertEquals(1f, inEnd, 0.001f)
    }

    @Test
    fun sleepTimerFadeMultiplier_fadesOverFinalFiveSeconds() {
        assertEquals(1f, SleepTimerManager.computeSleepFadeMultiplier(10_000L), 0.001f)
        assertEquals(1f, SleepTimerManager.computeSleepFadeMultiplier(5_000L), 0.001f)
        assertEquals(0.5f, SleepTimerManager.computeSleepFadeMultiplier(2_500L), 0.001f)
        assertEquals(0f, SleepTimerManager.computeSleepFadeMultiplier(0L), 0.001f)
    }

    @Test
    fun equalizerClamping_respectsDeviceBounds() {
        assertEquals(-1500, AudioEffectsManager.clampBandLevel(-3000, -1500, 1500))
        assertEquals(1500, AudioEffectsManager.clampBandLevel(2200, -1500, 1500))
        assertEquals(350, AudioEffectsManager.clampBandLevel(350, -1500, 1500))
    }
}
