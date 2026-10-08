package com.radiotv.tvremote.core

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.runCatching
import java.nio.charset.StandardCharsets

class SamsungTizenRemote(
    host: String,
    name: String = "Samsung TV",
    port: Int = 8001
) : JsonWebSocketRemote(
    insecureClient(),
    RemoteDeviceInfo("samsung:" + host + ":" + port, name, host, port, DeviceProtocol.SAMSUNG_TIZEN)
) {
    override val capabilities = setOf(
        Capability.NAVIGATION, Capability.VOLUME, Capability.CHANNEL,
        Capability.POWER, Capability.MUTE, Capability.INPUT, Capability.MEDIA, Capability.NUMERIC
    )

    private val keyMap = mapOf(
        TvKey.POWER to "KEY_POWER", TvKey.MUTE to "KEY_MUTE",
        TvKey.VOLUME_UP to "KEY_VOLUP", TvKey.VOLUME_DOWN to "KEY_VOLDOWN",
        TvKey.CHANNEL_UP to "KEY_CHUP", TvKey.CHANNEL_DOWN to "KEY_CHDOWN",
        TvKey.UP to "KEY_UP", TvKey.DOWN to "KEY_DOWN", TvKey.LEFT to "KEY_LEFT",
        TvKey.RIGHT to "KEY_RIGHT", TvKey.OK to "KEY_ENTER", TvKey.BACK to "KEY_RETURN",
        TvKey.HOME to "KEY_HOME", TvKey.MENU to "KEY_MENU", TvKey.INPUT to "KEY_SOURCE",
        TvKey.PLAY_PAUSE to "KEY_PLAYPAUSE", TvKey.STOP to "KEY_STOP",
        TvKey.REWIND to "KEY_REWIND", TvKey.FAST_FORWARD to "KEY_FF",
        TvKey.DIGIT_0 to "KEY_0", TvKey.DIGIT_1 to "KEY_1", TvKey.DIGIT_2 to "KEY_2",
        TvKey.DIGIT_3 to "KEY_3", TvKey.DIGIT_4 to "KEY_4", TvKey.DIGIT_5 to "KEY_5",
        TvKey.DIGIT_6 to "KEY_6", TvKey.DIGIT_7 to "KEY_7", TvKey.DIGIT_8 to "KEY_8",
        TvKey.DIGIT_9 to "KEY_9"
    )

    override suspend fun connect() {
        val name = Base64.encodeToString(
            "Radio.TV Remote".toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP
        )
        val scheme = if (info.port == 8002) "wss" else "ws"
        val url = scheme + "://" + info.host + ":" + info.port +
            "/api/v2/channels/samsung.remote.control?name=" + Uri.encode(name)
        stateFlow.value = ConnectionState.PAIRING
        open(url, JSONObject())
        stateFlow.value = ConnectionState.READY
    }

    override suspend fun send(command: RemoteCommand) {
        when (command) {
            is RemoteCommand.Key -> {
                val key = keyMap[command.code] ?: throw RemoteException("Samsung: " + command.code)
                sendJson(JSONObject().apply {
                    put("method", "ms.remote.control")
                    put("params", JSONObject().apply {
                        put("Cmd", "Click")
                        put("DataOfCmd", key)
                        put("Option", "false")
                        put("TypeOfRemote", "SendRemoteKey")
                    })
                })
            }
            is RemoteCommand.Text -> throw RemoteException("Samsung text input: vendor-specific API varies by model")
            else -> throw RemoteException("Samsung pointer: model-specific input socket not enabled here")
        }
    }
}
