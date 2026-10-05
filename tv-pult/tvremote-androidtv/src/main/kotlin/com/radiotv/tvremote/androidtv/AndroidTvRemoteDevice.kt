package com.radiotv.tvremote.androidtv

import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteKeyCode
import com.radiotv.tvremote.androidtv.protocol.ConnectionState
import com.radiotv.tvremote.androidtv.protocol.RemoteClient
import com.radiotv.tvremote.core.RemoteConnectionState
import com.radiotv.tvremote.core.RemoteDevice
import com.radiotv.tvremote.core.RemoteKey
import com.radiotv.tvremote.core.RemoteVolume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.security.KeyStore

class AndroidTvRemoteDevice(
    host: String,
    keyStore: KeyStore,
    password: CharArray,
) : RemoteDevice {
    private val client = RemoteClient(
        host = host,
        keyStore = keyStore,
        password = password,
        deviceModel = "TV pult",
        deviceVendor = "radiotv",
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val state: StateFlow<RemoteConnectionState> =
        client.state.map { state ->
            when (state) {
                ConnectionState.Disconnected -> RemoteConnectionState.Disconnected
                ConnectionState.Connecting -> RemoteConnectionState.Connecting
                ConnectionState.Ready -> RemoteConnectionState.Ready
                is ConnectionState.Failed -> RemoteConnectionState.Failed(state.cause.message ?: "Ошибка соединения")
            }
        }.stateIn(scope, SharingStarted.Eagerly, RemoteConnectionState.Disconnected)

    override val volume: StateFlow<RemoteVolume?> =
        client.volume.map { value ->
            value?.let { RemoteVolume(it.level, it.max, it.muted) }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun connect() = client.connect()
    override fun disconnect() = client.disconnect()
    override fun send(key: RemoteKey) = client.sendKey(key.toProto())
    override fun sendText(text: String) = client.sendText(text)
    override suspend fun startVoice(): Int = client.startVoice()
    override fun sendVoiceChunk(samples: ByteArray, sessionId: Int) = client.sendVoiceChunk(samples, sessionId)
    override fun endVoice(sessionId: Int) = client.endVoice(sessionId)

    private fun RemoteKey.toProto(): RemoteKeyCode = when (this) {
        RemoteKey.POWER -> RemoteKeyCode.KEYCODE_POWER
        RemoteKey.MUTE -> RemoteKeyCode.KEYCODE_MUTE
        RemoteKey.INPUT -> RemoteKeyCode.KEYCODE_TV
        RemoteKey.VOLUME_UP -> RemoteKeyCode.KEYCODE_VOLUME_UP
        RemoteKey.VOLUME_DOWN -> RemoteKeyCode.KEYCODE_VOLUME_DOWN
        RemoteKey.CHANNEL_UP -> RemoteKeyCode.KEYCODE_CHANNEL_UP
        RemoteKey.CHANNEL_DOWN -> RemoteKeyCode.KEYCODE_CHANNEL_DOWN
        RemoteKey.UP -> RemoteKeyCode.KEYCODE_DPAD_UP
        RemoteKey.DOWN -> RemoteKeyCode.KEYCODE_DPAD_DOWN
        RemoteKey.LEFT -> RemoteKeyCode.KEYCODE_DPAD_LEFT
        RemoteKey.RIGHT -> RemoteKeyCode.KEYCODE_DPAD_RIGHT
        RemoteKey.OK -> RemoteKeyCode.KEYCODE_DPAD_CENTER
        RemoteKey.BACK -> RemoteKeyCode.KEYCODE_BACK
        RemoteKey.HOME -> RemoteKeyCode.KEYCODE_HOME
        RemoteKey.MENU -> RemoteKeyCode.KEYCODE_MENU
        RemoteKey.PLAY -> RemoteKeyCode.KEYCODE_MEDIA_PLAY
        RemoteKey.PAUSE -> RemoteKeyCode.KEYCODE_MEDIA_PAUSE
        RemoteKey.STOP -> RemoteKeyCode.KEYCODE_MEDIA_STOP
        RemoteKey.REWIND -> RemoteKeyCode.KEYCODE_MEDIA_REWIND
        RemoteKey.FAST_FORWARD -> RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD
        RemoteKey.SEARCH -> RemoteKeyCode.KEYCODE_SEARCH
        RemoteKey.DIGIT_0 -> RemoteKeyCode.KEYCODE_0
        RemoteKey.DIGIT_1 -> RemoteKeyCode.KEYCODE_1
        RemoteKey.DIGIT_2 -> RemoteKeyCode.KEYCODE_2
        RemoteKey.DIGIT_3 -> RemoteKeyCode.KEYCODE_3
        RemoteKey.DIGIT_4 -> RemoteKeyCode.KEYCODE_4
        RemoteKey.DIGIT_5 -> RemoteKeyCode.KEYCODE_5
        RemoteKey.DIGIT_6 -> RemoteKeyCode.KEYCODE_6
        RemoteKey.DIGIT_7 -> RemoteKeyCode.KEYCODE_7
        RemoteKey.DIGIT_8 -> RemoteKeyCode.KEYCODE_8
        RemoteKey.DIGIT_9 -> RemoteKeyCode.KEYCODE_9
    }
}