package com.offex7.streamhub

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

data class RadioTimerState(val elapsedMs: Long, val startedAtMs: Long)

private val Context.radioTimerDataStore by preferencesDataStore("radio_timer_state")

class RadioTimerStore(private val context: Context) {
    private val elapsedKey = longPreferencesKey("elapsed_ms")
    private val startedAtKey = longPreferencesKey("started_at_ms")
    private val stationKey = stringPreferencesKey("station_key")
    private val checkpointKey = longPreferencesKey("last_playback_checkpoint_ms")

    suspend fun state(): RadioTimerState {
        val prefs = context.radioTimerDataStore.data.first()
        return RadioTimerState(
            elapsedMs = (prefs[elapsedKey] ?: 0L).coerceAtLeast(0L),
            startedAtMs = (prefs[startedAtKey] ?: 0L).coerceAtLeast(0L)
        )
    }

    /**
     * Starts (or resumes) only when the active station matches the persisted station.
     * An existing start timestamp is kept intact across Activity/process recreation.
     */
    suspend fun resumeIfNeeded(now: Long, stationId: String): RadioTimerState {
        val safeNow = now.coerceAtLeast(0L)
        var result = RadioTimerState(0L, safeNow)
        context.radioTimerDataStore.edit { prefs ->
            val savedStation = prefs[stationKey]
            val currentStation = stationId.trim()
            val sameStation = savedStation.isNullOrBlank() || savedStation == currentStation
            val elapsed = if (sameStation) (prefs[elapsedKey] ?: 0L).coerceAtLeast(0L) else 0L
            val started = (prefs[startedAtKey] ?: 0L).coerceAtLeast(0L)
            result = if (sameStation && started > 0L) {
                RadioTimerState(elapsed, started)
            } else {
                RadioTimerState(elapsed, safeNow)
            }
            prefs[elapsedKey] = result.elapsedMs
            prefs[startedAtKey] = result.startedAtMs
            prefs[checkpointKey] = safeNow
            if (currentStation.isNotBlank()) prefs[stationKey] = currentStation
        }
        return result
    }

    /** Atomic/idempotent counterpart of pause() for the foreground service. */
    suspend fun pauseIfRunning(now: Long): RadioTimerState {
        val safeNow = now.coerceAtLeast(0L)
        var result = RadioTimerState(0L, 0L)
        context.radioTimerDataStore.edit { prefs ->
            val currentElapsed = (prefs[elapsedKey] ?: 0L).coerceAtLeast(0L)
            val started = (prefs[startedAtKey] ?: 0L).coerceAtLeast(0L)
            val extra = if (started > 0L) (safeNow - started).coerceAtLeast(0L) else 0L
            result = RadioTimerState(currentElapsed + extra, 0L)
            prefs[elapsedKey] = result.elapsedMs
            prefs.remove(startedAtKey)
            prefs.remove(checkpointKey)
        }
        return result
    }

    suspend fun checkpoint(now: Long) {
        val safeNow = now.coerceAtLeast(0L)
        context.radioTimerDataStore.edit { prefs ->
            val started = (prefs[startedAtKey] ?: 0L).coerceAtLeast(0L)
            if (started > 0L) prefs[checkpointKey] = safeNow.coerceAtLeast(started)
        }
    }

    /** Closes a segment left open by an OS process kill using its last persisted service heartbeat. */
    suspend fun recoverAfterProcessRestart(): RadioTimerState {
        var result = RadioTimerState(0L, 0L)
        context.radioTimerDataStore.edit { prefs ->
            val elapsed = (prefs[elapsedKey] ?: 0L).coerceAtLeast(0L)
            val started = (prefs[startedAtKey] ?: 0L).coerceAtLeast(0L)
            val checkpoint = (prefs[checkpointKey] ?: started).coerceAtLeast(started)
            val played = if (started > 0L) (checkpoint - started).coerceAtLeast(0L) else 0L
            result = RadioTimerState(elapsed + played, 0L)
            prefs[elapsedKey] = result.elapsedMs
            prefs.remove(startedAtKey)
            prefs.remove(checkpointKey)
        }
        return result
    }

    suspend fun start(now: Long): RadioTimerState {
        val state = RadioTimerState(0L, now.coerceAtLeast(0L))
        context.radioTimerDataStore.edit {
            it[elapsedKey] = state.elapsedMs
            it[startedAtKey] = state.startedAtMs
            it[checkpointKey] = state.startedAtMs
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
            prefs.remove(checkpointKey)
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
            prefs[checkpointKey] = result.startedAtMs
        }
        return result
    }

    suspend fun reset() {
        context.radioTimerDataStore.edit {
            it.remove(elapsedKey)
            it.remove(startedAtKey)
            it.remove(stationKey)
            it.remove(checkpointKey)
        }
    }
}
