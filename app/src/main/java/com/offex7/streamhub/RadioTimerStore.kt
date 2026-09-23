package com.offex7.streamhub

import android.content.Context

data class RadioTimerState(val elapsedMs: Long, val startedAtMs: Long)

class RadioTimerStore(context: Context) {
    private val prefs = context.getSharedPreferences("radio_timer_state", Context.MODE_PRIVATE)

    fun state(): RadioTimerState = RadioTimerState(
        elapsedMs = prefs.getLong("elapsed_ms", 0L).coerceAtLeast(0L),
        startedAtMs = prefs.getLong("started_at_ms", 0L).coerceAtLeast(0L)
    )

    fun start(now: Long): RadioTimerState {
        val state = RadioTimerState(0L, now.coerceAtLeast(0L))
        prefs.edit().putLong("elapsed_ms", 0L).putLong("started_at_ms", state.startedAtMs).apply()
        return state
    }

    fun pause(now: Long): RadioTimerState {
        val current = state()
        val extra = if (current.startedAtMs > 0L) (now - current.startedAtMs).coerceAtLeast(0L) else 0L
        val state = RadioTimerState(current.elapsedMs + extra, 0L)
        prefs.edit().putLong("elapsed_ms", state.elapsedMs).remove("started_at_ms").apply()
        return state
    }

    fun resume(now: Long, cutoffTime: Long = 0L): RadioTimerState {
        val current = state()
        val extra = if (current.startedAtMs > 0L) {
            val cutoff = if (cutoffTime > current.startedAtMs) cutoffTime else now
            (cutoff - current.startedAtMs).coerceAtLeast(0L)
        } else 0L
        val state = RadioTimerState(current.elapsedMs + extra, now.coerceAtLeast(0L))
        prefs.edit().putLong("elapsed_ms", state.elapsedMs).putLong("started_at_ms", state.startedAtMs).apply()
        return state
    }

    fun reset() {
        prefs.edit().clear().apply()
    }
}
