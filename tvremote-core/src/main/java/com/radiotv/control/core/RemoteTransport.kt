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

/** Protocol-neutral remote device controlled with Android key codes and text commands. */
interface RemoteDevice {
    suspend fun connect(host: String, port: Int = 5555)
    suspend fun sendKey(keyCode: Int)
    suspend fun sendText(text: String)
    suspend fun launchApp(packageName: String)
    suspend fun disconnect()
}
