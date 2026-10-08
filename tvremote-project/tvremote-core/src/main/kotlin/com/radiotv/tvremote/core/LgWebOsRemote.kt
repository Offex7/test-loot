package com.radiotv.tvremote.core

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import okhttp3.WebSocket
import org.json.JSONArray
import org.json.JSONObject

class LgWebOsRemote(
    context: Context,
    host: String,
    name: String = "LG webOS TV",
    port: Int = 3000
) : JsonWebSocketRemote(
    insecureClient(),
    RemoteDeviceInfo("lg:" + host + ":" + port, name, host, port, DeviceProtocol.LG_WEBOS)
) {
    override val capabilities = setOf(
        Capability.NAVIGATION, Capability.VOLUME, Capability.CHANNEL, Capability.POWER,
        Capability.MUTE, Capability.INPUT, Capability.MEDIA, Capability.TEXT, Capability.POINTER
    )

    private val prefs = context.getSharedPreferences("lg-webos", Context.MODE_PRIVATE)
    private var pointerWs: WebSocket? = null

    private val keyMap = mapOf(
        TvKey.UP to "UP", TvKey.DOWN to "DOWN", TvKey.LEFT to "LEFT", TvKey.RIGHT to "RIGHT",
        TvKey.OK to "ENTER", TvKey.BACK to "BACK", TvKey.HOME to "HOME", TvKey.MENU to "MENU",
        TvKey.INFO to "INFO", TvKey.PLAY_PAUSE to "PLAY", TvKey.STOP to "STOP",
        TvKey.REWIND to "REWIND", TvKey.FAST_FORWARD to "FASTFORWARD",
        TvKey.VOLUME_UP to "VOLUMEUP", TvKey.VOLUME_DOWN to "VOLUMEDOWN", TvKey.MUTE to "MUTE",
        TvKey.CHANNEL_UP to "CHANNELUP", TvKey.CHANNEL_DOWN to "CHANNELDOWN"
    )

    override fun onJsonMessage(json: JSONObject) {
        if (json.optString("type") == "registered") {
            val key = json.optJSONObject("payload")?.optString("client-key", "")
            if (!key.isNullOrBlank()) prefs.edit().putString("key:" + info.host, key).apply()
        }
    }

    override suspend fun connect() {
        val manifest = JSONObject().apply {
            put("manifestVersion", 1)
            put("appVersion", "1.0")
            put("signed", JSONObject().apply {
                put("created", "20261008")
                put("appId", "com.radiotv.tvremote")
                put("vendorId", "com.radiotv")
                put("localizedAppNames", JSONObject().put("", "Radio.TV Remote"))
                put("permissions", JSONArray(listOf(
                    "LAUNCH", "CONTROL_AUDIO", "CONTROL_INPUT_TEXT",
                    "CONTROL_MOUSE_AND_KEYBOARD", "CONTROL_POWER"
                )))
            })
        }
        val registerId = "register_0"
        val payload = JSONObject().apply {
            put("forcePairing", false)
            put("pairingType", "PROMPT")
            prefs.getString("key:" + info.host, null)?.let { put("client-key", it) }
            put("manifest", manifest)
        }
        stateFlow.value = ConnectionState.PAIRING
        open("ws://" + info.host + ":" + info.port, JSONObject().apply {
            put("type", "register"); put("id", registerId); put("payload", payload)
        })
        stateFlow.value = ConnectionState.READY
    }

    override suspend fun send(command: RemoteCommand) {
        when (command) {
            is RemoteCommand.Key -> {
                val name = keyMap[command.code] ?: throw RemoteException("LG: " + command.code)
                pointerButton(name)
            }
            is RemoteCommand.Text -> request(
                "ssap://com.webos.service.ime/insertText",
                JSONObject().put("text", command.value).put("replace", 0)
            )
            is RemoteCommand.PointerMove -> ensurePointer().send(
                "type:move\ndx:" + command.dx + "\ndy:" + command.dy + "\ndown:0\n\n"
            )
            RemoteCommand.PointerClick -> ensurePointer().send("type:click\n\n")
            RemoteCommand.PointerRightClick -> ensurePointer().send("type:button\nname:RIGHT\n\n")
        }
    }

    private suspend fun pointerButton(name: String) {
        ensurePointer().send("type:button\nname:" + name + "\n\n")
    }

    private suspend fun ensurePointer(): WebSocket {
        pointerWs?.let { return it }
        val response = request("ssap://com.webos.service.networkinput/getPointerInputSocket")
        val socketPath = response.optJSONObject("payload")?.optString("socketPath")
            ?: throw RemoteException("LG не вернул socketPath мыши")
        val normalized = if (socketPath.startsWith("wss:")) {
            socketPath.replaceFirst("wss:", "ws:").replace(":3001/", ":3000/")
        } else socketPath
        val done = CompletableDeferred<WebSocket>()
        pointerWs = contextClient.newWebSocket(
            okhttp3.Request.Builder().url(normalized).build(),
            object : okhttp3.WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) { done.complete(webSocket) }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    done.completeExceptionally(t)
                }
            }
        )
        return withTimeout(5000) { done.await() }
    }

    override suspend fun disconnect() {
        pointerWs?.close(1000, "bye")
        pointerWs = null
        super.disconnect()
    }
}
