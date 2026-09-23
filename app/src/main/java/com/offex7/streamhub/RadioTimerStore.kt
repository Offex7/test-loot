package com.offex7.streamhub

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

data class RadioTimerState(val elapsedMs: Long, val startedAtMs: Long)

private val Context.radioTimerDataStore by preferencesDataStore("radio_timer_state")

class RadioTimerStore(private val context: Context) {
    private val elapsedKey = longPreferencesKey("elapsed_ms")
    private val startedAtKey = longPreferencesKey("started_at_ms")

    suspend fun state(): RadioTimerState {
        val prefs = context.radioTimerDataStore.data.first()
        return RadioTimerState(
            elapsedMs = (prefs[elapsedKey] ?: 0L).coerceAtLeast(0L),
            startedAtMs = (prefs[startedAtKey] ?: 0L).coerceAtLeast(0L)
        )
    }

    suspend fun start(now: Long): RadioTimerState {
        val state = RadioTimerState(0L, now.coerceAtLeast(0L))
        context.radioTimerDataStore.edit {
            it[elapsedKey] = state.elapsedMs
            it[startedAtKey] = state.startedAtMs
        }
        return state
    }

    suspend fun pause(now: Long): RadioTimerState {
        var result = RadioTimerState(0L, 0L)
        context.radioTimerDataStore.edit { prefs ->
            val currentElapsed = (prefs[elapsedKey] ?: 0L).coerceAtLeast(0L)
            val started = (prefs[startedAtKey] ?: 0L).coerceAtLeast(0L)
            val extra = if (started > 0L) (now - started).coerceAtLeast(0L) else 0L
            result = RadioTimerState(currentElapsed + extra, 0L)
            prefs[elapsedKey] = result.elapsedMs
            prefs.remove(startedAtKey)
        }
        return result
    }

    suspend fun resume(now: Long, cutoffTime: Long = 0L): RadioTimerState {
        var result = RadioTimerState(0L, now.coerceAtLeast(0L))
        context.radioTimerDataStore.edit { prefs ->
            var elapsed = (prefs[elapsedKey] ?: 0L).coerceAtLeast(0L)
            val started = (prefs[startedAtKey] ?: 0L).coerceAtLeast(0L)
            if (started > 0L) {
                val cutoff = if (cutoffTime > started) cutoffTime else now
                elapsed += (cutoff - started).coerceAtLeast(0L)
            }
            result = RadioTimerState(elapsed, now.coerceAtLeast(0L))
            prefs[elapsedKey] = result.elapsedMs
            prefs[startedAtKey] = result.startedAtMs
        }
        return result
    }

    suspend fun reset() {
        context.radioTimerDataStore.edit {
            it.remove(elapsedKey)
            it.remove(startedAtKey)
        }
    }
}
