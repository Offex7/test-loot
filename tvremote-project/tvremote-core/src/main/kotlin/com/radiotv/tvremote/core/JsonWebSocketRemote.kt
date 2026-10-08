package com.radiotv.tvremote.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

abstract class JsonWebSocketRemote(
    protected val contextClient: OkHttpClient,
    override val info: RemoteDeviceInfo
) : RemoteDevice {
    protected var ws: WebSocket? = null
    protected val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(ConnectionState.DISCONNECTED)
    override val state = stateFlow
    private val pending = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()

    protected suspend fun open(url: String, firstMessage: JSONObject) {
        stateFlow.value = ConnectionState.CONNECTING
        val firstId = firstMessage.optString("id", "")
        val firstSignal = if (firstId.isNotBlank()) CompletableDeferred<JSONObject>().also { pending[firstId] = it } else null
        suspendCancellableCoroutine<Unit> { continuation ->
            val socket = contextClient.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    ws = webSocket
                    if (firstMessage.length() > 0) webSocket.send(firstMessage.toString())
                    if (continuation.isActive) continuation.resume(Unit)
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        val json = JSONObject(text)
                        val id = json.optString("id", "")
                        if (id.isNotBlank()) pending.remove(id)?.complete(json)
                        onJsonMessage(json)
                    }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (continuation.isActive) continuation.resumeWithException(t)
                    pending.values.forEach { it.completeExceptionally(t) }
                    pending.clear()
                    stateFlow.value = ConnectionState.FAILED
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (ws === webSocket) ws = null
                    pending.values.forEach { it.completeExceptionally(RemoteException(reason)) }
                    pending.clear()
                    stateFlow.value = ConnectionState.DISCONNECTED
                }
            })
            continuation.invokeOnCancellation { socket.cancel() }
        }
        if (firstSignal != null) withTimeout(15_000) { firstSignal.await() }
    }

    protected open fun onJsonMessage(json: JSONObject) = Unit

    protected fun sendJson(json: JSONObject) {
        val socket = ws ?: throw RemoteException("WebSocket не подключён")
        if (!socket.send(json.toString())) throw RemoteException("WebSocket не принял сообщение")
    }

    protected suspend fun request(uri: String, payload: JSONObject = JSONObject()): JSONObject {
        val id = "r_" + System.nanoTime()
        val signal = CompletableDeferred<JSONObject>()
        pending[id] = signal
        sendJson(JSONObject().apply {
            put("type", "request")
            put("id", id)
            put("uri", uri)
            put("payload", payload)
        })
        return try {
            withTimeout(7000) { signal.await() }
        } finally {
            pending.remove(id)
        }
    }

    override suspend fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
        pending.values.forEach { it.cancel() }
        pending.clear()
        stateFlow.value = ConnectionState.DISCONNECTED
    }

    protected companion object {
        fun insecureClient(): OkHttpClient {
            val trustAll = object : X509TrustManager {
                override fun getAcceptedIssuers() = emptyArray<java.security.cert.X509Certificate>()
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
            }
            val ssl = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustAll), SecureRandom())
            }
            return OkHttpClient.Builder()
                .sslSocketFactory(ssl.socketFactory, trustAll)
                .hostnameVerifier { _, _ -> true }
                .build()
        }
    }
}
