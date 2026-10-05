package com.radiotv.tvremote.core

import kotlinx.coroutines.flow.StateFlow

sealed interface RemoteConnectionState {
    data object Disconnected : RemoteConnectionState
    data object Connecting : RemoteConnectionState
    data object Ready : RemoteConnectionState
    data class Failed(val message: String) : RemoteConnectionState
}

data class RemoteVolume(val level: Int, val max: Int, val muted: Boolean)

interface RemoteDevice {
    val state: StateFlow<RemoteConnectionState>
    val volume: StateFlow<RemoteVolume?>
    suspend fun connect()
    fun disconnect()
    fun send(key: RemoteKey)
    fun sendText(text: String)
    suspend fun startVoice(): Int
    fun sendVoiceChunk(samples: ByteArray, sessionId: Int)
    fun endVoice(sessionId: Int)
}
