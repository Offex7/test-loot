package com.offex7.streamhub

import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer

class RadioAudioEffects(private val audioSessionId: Int) {
    private val equalizer: Equalizer? = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
    private val virtualizer: Virtualizer? = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()

    fun apply(settings: SettingsStore.RadioEqualizerSettings) {
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = true
            val range = eq.bandLevelRange
            val bands = eq.numberOfBands.toInt().coerceAtLeast(1)
            fun level(value: Int): Short = value.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
            val bassBand = 0
            val midBand = bands / 2
            val trebleBand = bands - 1
            eq.setBandLevel(bassBand.toShort(), level(settings.bass))
            if (midBand != bassBand) eq.setBandLevel(midBand.toShort(), level(settings.mid))
            if (trebleBand != bassBand && trebleBand != midBand) eq.setBandLevel(trebleBand.toShort(), level(settings.treble))
            when (settings.preset.lowercase()) {
                "bass boost" -> eq.setBandLevel(bassBand.toShort(), level(maxOf(settings.bass, 1000)))
                "rock" -> {
                    eq.setBandLevel(bassBand.toShort(), level(900))
                    if (midBand != bassBand) eq.setBandLevel(midBand.toShort(), level(150))
                    if (trebleBand != midBand) eq.setBandLevel(trebleBand.toShort(), level(800))
                }
                "pop" -> {
                    eq.setBandLevel(bassBand.toShort(), level(500))
                    if (midBand != bassBand) eq.setBandLevel(midBand.toShort(), level(-100))
                    if (trebleBand != midBand) eq.setBandLevel(trebleBand.toShort(), level(500))
                }
                "jazz" -> {
                    eq.setBandLevel(bassBand.toShort(), level(400))
                    if (midBand != bassBand) eq.setBandLevel(midBand.toShort(), level(-150))
                    if (trebleBand != midBand) eq.setBandLevel(trebleBand.toShort(), level(350))
                }
            }
            virtualizer?.enabled = settings.preset.equals("Virtualizer", true)
            if (virtualizer?.enabled == true) {
                virtualizer.setStrength(700)
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { virtualizer?.release() }
    }
}
