package com.offex7.streamhub

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object InteractionFeedback {
    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun vibrate(
        context: Context,
        enabled: Boolean,
        durationMs: Long = 55L,
        amplitude: Int = 180
    ): Boolean {
        if (!enabled) return false
        val v = vibrator(context) ?: return false
        if (!v.hasVibrator()) return false
        return runCatching {
            v.vibrate(
                VibrationEffect.createOneShot(
                    durationMs.coerceIn(20L, 300L),
                    amplitude.coerceIn(1, 255)
                )
            )
            true
        }.getOrDefault(false)
    }

    fun threshold(context: Context, enabled: Boolean): Boolean =
        vibrate(context, enabled, 48L, 165)

    fun success(context: Context, enabled: Boolean): Boolean {
        if (!enabled) return false
        val v = vibrator(context) ?: return false
        if (!v.hasVibrator()) return false
        return runCatching {
            v.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 42L, 48L, 65L),
                    intArrayOf(0, 170, 0, 200),
                    -1
                )
            )
            true
        }.getOrDefault(false)
    }

    fun error(context: Context, enabled: Boolean): Boolean {
        if (!enabled) return false
        val v = vibrator(context) ?: return false
        if (!v.hasVibrator()) return false
        return runCatching {
            v.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 70L, 60L, 70L),
                    intArrayOf(0, 210, 0, 210),
                    -1
                )
            )
            true
        }.getOrDefault(false)
    }

    fun beep(context: Context, enabled: Boolean): Boolean =
        if (!enabled) false else runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 62)
            try {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
            } finally {
                tone.release()
            }
            true
        }.getOrDefault(false)

    fun click(
        context: Context,
        hapticsEnabled: Boolean,
        soundEnabled: Boolean,
        allowSound: Boolean = true
    ) {
        if (vibrate(context, hapticsEnabled, 55L, 180)) return
        if (allowSound) beep(context, soundEnabled)
    }
}
