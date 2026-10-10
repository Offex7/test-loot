package com.radiotv.control.core

import kotlinx.coroutines.flow.StateFlow

interface RemoteTransport {
    val status: StateFlow<RemoteStatus>
    suspend fun beginPairing(host: String)
    suspend fun completePairing(code: String)
    suspend fun connect(host: String)
    suspend fun sendKey(key: RemoteKey)
    suspend fun disconnect()
}
