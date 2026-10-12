package com.radiotv.control.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Xml
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

data class InstalledTvApp(
    val id: String,
    val name: String,
    val iconUrl: String? = null
)

sealed interface OtherTvRemoteStatus {
    data object Disconnected : OtherTvRemoteStatus
    data class Connecting(val ip: String, val type: RemoteDeviceType) : OtherTvRemoteStatus
    data class Pairing(val ip: String, val type: RemoteDeviceType, val message: String) : OtherTvRemoteStatus
    data class Connected(val ip: String, val type: RemoteDeviceType) : OtherTvRemoteStatus
    data class Error(val message: String) : OtherTvRemoteStatus
}

/** Local-network adapters for Samsung Tizen, LG webOS and Roku ECP. */
class OtherTvRemoteController(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("other-tv-remote", Context.MODE_PRIVATE)
    private val stateMutable = MutableStateFlow<OtherTvRemoteStatus>(OtherTvRemoteStatus.Disconnected)
    val status: StateFlow<OtherTvRemoteStatus> = stateMutable.asStateFlow()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var protocol: RemoteDeviceType? = null
    @Volatile private var currentHost: String? = null
    @Volatile private var lgClientKey: String? = null
    private var nextMessageId = 1
    private val pendingLgResponses = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()

    private val plainClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()
    private val localTlsClient: OkHttpClient get() = lazyLocalTlsClient

    suspend fun connect(device: DiscoveredRemoteDevice) = withContext(Dispatchers.IO) {
        disconnectCurrent()
        currentHost = device.ip
        protocol = device.type
        stateMutable.value = OtherTvRemoteStatus.Connecting(device.ip, device.type)
        when (device.type) {
            RemoteDeviceType.SAMSUNG -> connectSamsung(device.ip)
            RemoteDeviceType.LG -> connectLg(device.ip)
            RemoteDeviceType.ROKU -> connectRoku(device.ip)
            else -> error("Для типа ${device.type.label} отдельный управляющий протокол ещё не настроен.")
        }
    }

    suspend fun sendKey(key: RemoteKey) = withContext(Dispatchers.IO) {
        val host = checkNotNull(currentHost) { "Сначала выберите найденный телевизор." }
        when (protocol) {
            RemoteDeviceType.SAMSUNG -> {
                val socket = checkNotNull(webSocket) { "WebSocket Samsung не подключён." }
                val json = JSONObject().put("method", "ms.remote.control").put("params", JSONObject()
                    .put("Cmd", "Click").put("DataOfCmd", samsungKey(key))
                    .put("Option", "false").put("TypeOfRemote", "SendRemoteKey"))
                check(socket.send(json.toString())) { "Не удалось отправить команду Samsung." }
            }
            RemoteDeviceType.LG -> {
                requireLgReady()
                sendLgRequest("ssap://com.webos.service.networkinput/sendButton", JSONObject().put("button", lgKey(key)))
            }
            RemoteDeviceType.ROKU -> rokuRequest(host, "POST", "/keypress/${rokuKey(key)}")
            else -> error("Текущий протокол не поддерживается.")
        }
    }

    suspend fun sendText(text: String) = withContext(Dispatchers.IO) {
        require(text.isNotEmpty()) { "Введите текст для отправки." }
        val host = checkNotNull(currentHost) { "Сначала выберите найденный телевизор." }
        when (protocol) {
            RemoteDeviceType.SAMSUNG -> {
                val socket = checkNotNull(webSocket) { "WebSocket Samsung не подключён." }
                val json = JSONObject().put("method", "ms.remote.control").put("params", JSONObject()
                    .put("Cmd", "Text").put("DataOfCmd", text).put("Option", "false")
                    .put("TypeOfRemote", "SendInputString"))
                check(socket.send(json.toString())) { "Не удалось отправить текст Samsung." }
            }
            RemoteDeviceType.LG -> {
                requireLgReady()
                sendLgRequest("ssap://com.webos.service.ime/insertText", JSONObject().put("text", text).put("replace", 0))
            }
            RemoteDeviceType.ROKU -> text.forEach { char ->
                val pathKey = when (char) {
                    '\n', '\r' -> "Enter"
                    else -> "Lit_" + Uri.encode(char.toString())
                }
                rokuRequest(host, "POST", "/keypress/$pathKey")
            }
            else -> error("Ввод текста не поддерживается текущим протоколом.")
        }
    }

    /**
     * Returns only apps reported by the currently connected local-TV protocol.
     * Unsupported protocols deliberately return an empty list.
     */
    suspend fun listInstalledApps(): List<InstalledTvApp> = withContext(Dispatchers.IO) {
        val host = currentHost ?: return@withContext emptyList()
        when (protocol) {
            RemoteDeviceType.SAMSUNG -> listSamsungApps(host)
            RemoteDeviceType.LG -> listLgApps()
            RemoteDeviceType.ROKU -> listRokuApps(host)
            else -> emptyList()
        }
    }

    /** Launch by exact ID returned by listInstalledApps; never guess an app ID. */
    suspend fun launchInstalledApp(app: InstalledTvApp) = withContext(Dispatchers.IO) {
        val host = checkNotNull(currentHost) { "Сначала подключитесь к телевизору." }
        when (protocol) {
            RemoteDeviceType.SAMSUNG -> {
                val socket = checkNotNull(webSocket) { "WebSocket Samsung не подключён." }
                val message = JSONObject()
                    .put("method", "ms.channel.emit")
                    .put("params", JSONObject()
                        .put("event", "ed.apps.launch")
                        .put("to", "host")
                        .put("data", JSONObject().put("appId", app.id).put("action_type", "NATIVE_LAUNCH")))
                check(socket.send(message.toString())) { "Не удалось отправить команду запуска Samsung." }
            }
            RemoteDeviceType.LG -> {
                requireLgReady()
                sendLgRequest("ssap://system.launcher/launch", JSONObject().put("id", app.id))
            }
            RemoteDeviceType.ROKU -> {
                val (code, _) = rokuRequest(host, "POST", "/launch/${Uri.encode(app.id)}")
                check(code in 200..299) { "Roku не принял запуск приложения (HTTP $code)." }
            }
            else -> error("Запуск приложений не поддерживается текущим протоколом.")
        }
    }

    private fun listSamsungApps(host: String): List<InstalledTvApp> {
        val endpoints = listOf(
            "http://$host:8001/api/v2/applications",
            "https://$host:8002/api/v2/applications"
        )
        for (endpoint in endpoints) {
            try {
                val client = (if (endpoint.startsWith("https://")) localTlsClient else plainClient)
                    .newBuilder()
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(4, TimeUnit.SECONDS)
                    .callTimeout(6, TimeUnit.SECONDS)
                    .build()
                client.newCall(Request.Builder().url(endpoint).get().build()).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val root = JSONObject(response.body?.string().orEmpty())
                    val items = root.optJSONArray("applications") ?: root.optJSONArray("apps") ?: JSONArray()
                    val apps = buildList {
                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            val id = item.optString("appId").takeIf { it.isNotBlank() && it != "null" }
                                ?: item.optString("id").takeIf { it.isNotBlank() && it != "null" } ?: continue
                            val name = item.optString("name").takeIf { it.isNotBlank() && it != "null" }
                                ?: item.optString("title").takeIf { it.isNotBlank() && it != "null" } ?: continue
                            val icon = item.optString("icon").takeIf { it.isNotBlank() && it != "null" }
                            add(InstalledTvApp(id, name, icon))
                        }
                    }.distinctBy { it.id }
                    if (apps.isNotEmpty()) return apps
                }
            } catch (_: Exception) {
                // Try the alternate Samsung local endpoint; expose no guessed apps.
            }
        }
        return emptyList()
    }

    private suspend fun listLgApps(): List<InstalledTvApp> {
        val payload = sendLgRequestAwait("ssap://com.webos.applicationManager/listLaunchPoints")
        val items = payload.optJSONArray("launchPoints")
            ?: payload.optJSONArray("launchpoints")
            ?: payload.optJSONArray("apps")
            ?: payload.optJSONObject("results")?.optJSONArray("launchPoints")
            ?: JSONArray()
        return buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() && it != "null" }
                    ?: item.optString("appId").takeIf { it.isNotBlank() && it != "null" } ?: continue
                val name = item.optString("title").takeIf { it.isNotBlank() && it != "null" }
                    ?: item.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: continue
                val icon = item.optString("icon").takeIf { it.isNotBlank() && it != "null" }
                add(InstalledTvApp(id, name, icon))
            }
        }.distinctBy { it.id }
    }

    private fun listRokuApps(host: String): List<InstalledTvApp> {
        val (code, xml) = rokuRequest(host, "GET", "/query/apps")
        if (code !in 200..299 || xml.isBlank()) return emptyList()
        return try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            val apps = mutableListOf<InstalledTvApp>()
            var event = parser.eventType
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (event == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name.equals("app", ignoreCase = true)) {
                    val id = parser.getAttributeValue(null, "id")
                    val name = parser.nextText().trim()
                    if (!id.isNullOrBlank() && name.isNotBlank()) apps += InstalledTvApp(id, name)
                }
                event = parser.next()
            }
            apps.distinctBy { it.id }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun sendLgRequestAwait(uri: String, payload: JSONObject = JSONObject()): JSONObject {
        requireLgReady()
        val socket = checkNotNull(webSocket) { "WebSocket LG не подключён." }
        val id = "rtv-${nextMessageId++}"
        val pending = CompletableDeferred<JSONObject>()
        pendingLgResponses[id] = pending
        val request = JSONObject().put("type", "request").put("id", id).put("uri", uri)
        if (payload.length() > 0) request.put("payload", payload)
        try {
            check(socket.send(request.toString())) { "Не удалось отправить запрос приложения LG." }
            return withTimeout(7_000L) { pending.await() }
        } finally {
            pendingLgResponses.remove(id)
        }
    }
    suspend fun disconnect() = withContext(Dispatchers.IO) { disconnectCurrent() }

    private suspend fun connectSamsung(host: String) {
        val token = preferences.getString("samsung-token:$host", null)
        try {
            openWebSocket(samsungUrl(host, 8002, true, token), RemoteDeviceType.SAMSUNG)
        } catch (secureError: Exception) {
            webSocket?.cancel()
            webSocket = null
            try {
                openWebSocket(samsungUrl(host, 8001, false, null), RemoteDeviceType.SAMSUNG)
            } catch (plainError: Exception) {
                throw IllegalStateException(
                    "Не удалось подключиться к Samsung через WSS: ${secureError.message}; WS: ${plainError.message}",
                    plainError
                )
            }
        }
        stateMutable.value = OtherTvRemoteStatus.Connected(host, RemoteDeviceType.SAMSUNG)
    }

    private suspend fun connectLg(host: String) {
        lgClientKey = preferences.getString("lg-client-key:$host", null)
        // onOpen requests pairing and onMessage("registered") owns the final state transition.
        openWebSocket("ws://$host:3000", RemoteDeviceType.LG)
    }

    private fun connectRoku(host: String) {
        val (code, _) = rokuRequest(host, "GET", "/query/device-info")
        check(code in 200..299) { "Roku ECP вернул HTTP $code." }
        stateMutable.value = OtherTvRemoteStatus.Connected(host, RemoteDeviceType.ROKU)
    }

    private suspend fun openWebSocket(url: String, type: RemoteDeviceType) {
        val gate = CompletableDeferred<Unit>()
        val host = currentHost ?: error("Не задан IP телевизора.")
        val client = if (url.startsWith("wss://", true)) localTlsClient else plainClient
        val socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                this@OtherTvRemoteController.webSocket = webSocket
                if (type == RemoteDeviceType.LG) {
                    stateMutable.value = OtherTvRemoteStatus.Pairing(
                        host, type, "Подтвердите запрос Radio.TV.Control на экране телевизора LG."
                    )
                    webSocket.send(buildLgRegistration())
                } else {
                    stateMutable.value = OtherTvRemoteStatus.Connected(host, type)
                }
                gate.complete(Unit)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching { handleSocketMessage(type, host, text) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!gate.isCompleted) gate.completeExceptionally(t)
                if (this@OtherTvRemoteController.webSocket === webSocket) {
                    this@OtherTvRemoteController.webSocket = null
                    stateMutable.value = OtherTvRemoteStatus.Error(
                        "WebSocket ${type.label}: ${t.message ?: "соединение закрыто"}"
                    )
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (this@OtherTvRemoteController.webSocket === webSocket) {
                    this@OtherTvRemoteController.webSocket = null
                    stateMutable.value = OtherTvRemoteStatus.Disconnected
                }
            }
        })
        webSocket = socket
        try {
            withTimeout(WEBSOCKET_CONNECT_TIMEOUT_MS) { gate.await() }
        } catch (error: Exception) {
            socket.cancel()
            if (webSocket === socket) webSocket = null
            throw error
        }
    }

    private fun handleSocketMessage(type: RemoteDeviceType, host: String, text: String) {
        val message = JSONObject(text)
        when (type) {
            RemoteDeviceType.SAMSUNG -> {
                val payload = message.optJSONObject("data") ?: message.optJSONObject("params")
                val token = payload?.optString("token")?.takeIf { it.isNotBlank() && it != "null" }
                if (token != null) preferences.edit().putString("samsung-token:$host", token).apply()
                if (message.optString("event") in setOf("ms.channel.connect", "ms.channel.ready")) {
                    stateMutable.value = OtherTvRemoteStatus.Connected(host, type)
                }
            }
            RemoteDeviceType.LG -> when (message.optString("type")) {
                "response" -> {
                    val id = message.optString("id")
                    val payload = message.optJSONObject("payload") ?: JSONObject()
                    pendingLgResponses.remove(id)?.complete(payload)
                }
                "registered" -> {
                    val key = message.optJSONObject("payload")?.optString("client-key")
                        ?.takeIf { it.isNotBlank() && it != "null" }
                    if (key != null) {
                        lgClientKey = key
                        preferences.edit().putString("lg-client-key:$host", key).apply()
                    }
                    stateMutable.value = OtherTvRemoteStatus.Connected(host, type)
                }
                "error" -> {
                    val description = message.optJSONObject("payload")?.optString("error")
                    stateMutable.value = OtherTvRemoteStatus.Pairing(
                        host, type, description?.takeIf { it.isNotBlank() }
                            ?: "Ожидается подтверждение сопряжения на телевизоре LG."
                    )
                }
            }
            else -> Unit
        }
    }

    private fun buildLgRegistration(): String {
        val permissions = JSONArray().apply {
            put("CONTROL_AUDIO"); put("CONTROL_INPUT_JOYSTICK"); put("CONTROL_INPUT_MEDIA_PLAYBACK")
            put("CONTROL_INPUT_TEXT"); put("CONTROL_POWER"); put("READ_CURRENT_CHANNEL")
            put("READ_RUNNING_APPS"); put("READ_TV_CURRENT_TIME")
        }
        val manifest = JSONObject()
            .put("manifestVersion", 1)
            .put("appVersion", "1.0")
            .put("signed", JSONObject().put("created", "2026-10-10").put("appId", "com.radiotv.control").put("vendorId", "Offex7"))
            .put("localizedAppNames", JSONObject().put("", "Radio.TV.Control"))
            .put("permissions", permissions)
            .put("serial", "radio-tv-control")
        val payload = JSONObject().put("forcePairing", false).put("pairingType", "PROMPT").put("manifest", manifest)
        lgClientKey?.let { payload.put("client-key", it) }
        return JSONObject().put("type", "register").put("id", "register_0").put("payload", payload).toString()
    }

    private fun sendLgRequest(uri: String, payload: JSONObject = JSONObject()) {
        val socket = checkNotNull(webSocket) { "WebSocket LG не подключён." }
        check(stateMutable.value is OtherTvRemoteStatus.Connected && lgClientKey != null) {
            "Сначала подтвердите сопряжение на телевизоре LG."
        }
        val request = JSONObject().put("type", "request").put("id", "rtv-${nextMessageId++}").put("uri", uri)
        if (payload.length() > 0) request.put("payload", payload)
        check(socket.send(request.toString())) { "Не удалось отправить команду LG." }
    }

    private fun requireLgReady() {
        check(stateMutable.value is OtherTvRemoteStatus.Connected && lgClientKey != null) {
            "Подтвердите сопряжение Radio.TV.Control на телевизоре LG."
        }
    }

    private fun samsungUrl(host: String, port: Int, secure: Boolean, token: String?): String {
        val name = Base64.encodeToString("Radio.TV.Control".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val uri = Uri.parse("${if (secure) "wss" else "ws"}://$host:$port/api/v2/channels/samsung.remote.control")
            .buildUpon().appendQueryParameter("name", name)
        if (!token.isNullOrBlank() && secure) uri.appendQueryParameter("token", token)
        return uri.build().toString()
    }

    private fun samsungKey(key: RemoteKey): String = when (key) {
        RemoteKey.UP -> "KEY_UP"; RemoteKey.DOWN -> "KEY_DOWN"; RemoteKey.LEFT -> "KEY_LEFT"; RemoteKey.RIGHT -> "KEY_RIGHT"
        RemoteKey.OK -> "KEY_ENTER"; RemoteKey.BACK -> "KEY_RETURN"; RemoteKey.HOME -> "KEY_HOME"; RemoteKey.MENU -> "KEY_MENU"
        RemoteKey.POWER -> "KEY_POWER"; RemoteKey.MUTE -> "KEY_MUTE"; RemoteKey.VOLUME_UP -> "KEY_VOLUP"; RemoteKey.VOLUME_DOWN -> "KEY_VOLDOWN"
        RemoteKey.CHANNEL_UP -> "KEY_CHUP"; RemoteKey.CHANNEL_DOWN -> "KEY_CHDOWN"; RemoteKey.SOURCE -> "KEY_SOURCE"
        RemoteKey.PLAY_PAUSE -> "KEY_PLAY"; RemoteKey.PLAY -> "KEY_PLAY"; RemoteKey.PAUSE -> "KEY_PAUSE"; RemoteKey.STOP -> "KEY_STOP"; RemoteKey.REWIND -> "KEY_REWIND"; RemoteKey.FAST_FORWARD -> "KEY_FF"
        RemoteKey.NUMBER_0 -> "KEY_0"; RemoteKey.NUMBER_1 -> "KEY_1"; RemoteKey.NUMBER_2 -> "KEY_2"; RemoteKey.NUMBER_3 -> "KEY_3"
        RemoteKey.NUMBER_4 -> "KEY_4"; RemoteKey.NUMBER_5 -> "KEY_5"; RemoteKey.NUMBER_6 -> "KEY_6"; RemoteKey.NUMBER_7 -> "KEY_7"
        RemoteKey.NUMBER_8 -> "KEY_8"; RemoteKey.NUMBER_9 -> "KEY_9"
    }

    private fun lgKey(key: RemoteKey): String = when (key) {
        RemoteKey.UP -> "UP"; RemoteKey.DOWN -> "DOWN"; RemoteKey.LEFT -> "LEFT"; RemoteKey.RIGHT -> "RIGHT"
        RemoteKey.OK -> "ENTER"; RemoteKey.BACK -> "BACK"; RemoteKey.HOME -> "HOME"; RemoteKey.MENU -> "MENU"
        RemoteKey.POWER -> "POWER"; RemoteKey.MUTE -> "MUTE"; RemoteKey.VOLUME_UP -> "VOLUMEUP"; RemoteKey.VOLUME_DOWN -> "VOLUMEDOWN"
        RemoteKey.CHANNEL_UP -> "CHANNELUP"; RemoteKey.CHANNEL_DOWN -> "CHANNELDOWN"; RemoteKey.SOURCE -> "INPUT"
        RemoteKey.PLAY_PAUSE -> "PLAY"; RemoteKey.PLAY -> "PLAY"; RemoteKey.PAUSE -> "PAUSE"; RemoteKey.STOP -> "STOP"; RemoteKey.REWIND -> "REWIND"; RemoteKey.FAST_FORWARD -> "FASTFORWARD"
        RemoteKey.NUMBER_0 -> "0"; RemoteKey.NUMBER_1 -> "1"; RemoteKey.NUMBER_2 -> "2"; RemoteKey.NUMBER_3 -> "3"
        RemoteKey.NUMBER_4 -> "4"; RemoteKey.NUMBER_5 -> "5"; RemoteKey.NUMBER_6 -> "6"; RemoteKey.NUMBER_7 -> "7"
        RemoteKey.NUMBER_8 -> "8"; RemoteKey.NUMBER_9 -> "9"
    }

    private fun rokuKey(key: RemoteKey): String = when (key) {
        RemoteKey.UP -> "Up"; RemoteKey.DOWN -> "Down"; RemoteKey.LEFT -> "Left"; RemoteKey.RIGHT -> "Right"
        RemoteKey.OK -> "Select"; RemoteKey.BACK -> "Back"; RemoteKey.HOME -> "Home"; RemoteKey.MENU -> "Info"
        RemoteKey.POWER -> "Power"; RemoteKey.MUTE -> "VolumeMute"; RemoteKey.VOLUME_UP -> "VolumeUp"; RemoteKey.VOLUME_DOWN -> "VolumeDown"
        RemoteKey.CHANNEL_UP -> "ChannelUp"; RemoteKey.CHANNEL_DOWN -> "ChannelDown"; RemoteKey.SOURCE -> "InputTuner"
        RemoteKey.PLAY_PAUSE -> "Play"; RemoteKey.PLAY -> "Play"; RemoteKey.PAUSE -> "Play"; RemoteKey.STOP -> "Play"; RemoteKey.REWIND -> "Rev"; RemoteKey.FAST_FORWARD -> "Fwd"
        RemoteKey.NUMBER_0 -> "Lit_0"; RemoteKey.NUMBER_1 -> "Lit_1"; RemoteKey.NUMBER_2 -> "Lit_2"; RemoteKey.NUMBER_3 -> "Lit_3"
        RemoteKey.NUMBER_4 -> "Lit_4"; RemoteKey.NUMBER_5 -> "Lit_5"; RemoteKey.NUMBER_6 -> "Lit_6"; RemoteKey.NUMBER_7 -> "Lit_7"
        RemoteKey.NUMBER_8 -> "Lit_8"; RemoteKey.NUMBER_9 -> "Lit_9"
    }

    private fun rokuRequest(host: String, method: String, path: String): Pair<Int, String> {
        val connection = URL("http://$host:8060$path").openConnection() as HttpURLConnection
        connection.connectTimeout = HTTP_TIMEOUT_MS
        connection.readTimeout = HTTP_TIMEOUT_MS
        connection.requestMethod = method
        if (method == "POST") {
            connection.doOutput = true
            connection.setRequestProperty("Content-Length", "0")
            connection.outputStream.use { }
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            code to (stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private fun disconnectCurrent() {
        webSocket?.close(1000, "Radio.TV.Control disconnect")
        webSocket?.cancel()
        webSocket = null
        pendingLgResponses.values.forEach { it.cancel() }
        pendingLgResponses.clear()
        protocol = null
        currentHost = null
        lgClientKey = null
        stateMutable.value = OtherTvRemoteStatus.Disconnected
    }

    private fun createLocalTlsClient(): OkHttpClient {
        val trustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), java.security.SecureRandom())
        }
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .pingInterval(25, TimeUnit.SECONDS)
            .build()
    }

    override fun close() {
        disconnectCurrent()
        plainClient.connectionPool.evictAll()
        plainClient.dispatcher.executorService.shutdown()
        if (localTlsClientDelegate.isInitialized()) {
            lazyLocalTlsClient.connectionPool.evictAll()
            lazyLocalTlsClient.dispatcher.executorService.shutdown()
        }
    }

    private val localTlsClientDelegate = lazy { createLocalTlsClient() }
    private val lazyLocalTlsClient: OkHttpClient by localTlsClientDelegate

    private companion object {
        const val WEBSOCKET_CONNECT_TIMEOUT_MS = 8_000L
        const val HTTP_TIMEOUT_MS = 4_000
    }
}
