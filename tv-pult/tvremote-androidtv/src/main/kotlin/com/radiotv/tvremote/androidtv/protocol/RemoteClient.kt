package com.radiotv.tvremote.androidtv.protocol

import com.google.protobuf.ByteString
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteAppLinkLaunchRequest
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteConfigure
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteDeviceInfo
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteDirection
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteEditInfo
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteImeBatchEdit
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteImeObject
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteKeyCode
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteKeyInject
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteMessage
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemotePingResponse
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteSetActive
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteVoiceBegin
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteVoiceEnd
import com.radiotv.tvremote.androidtv.proto.RemoteProto.RemoteVoicePayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.io.IOException
import java.io.OutputStream
import java.security.KeyStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.net.ssl.SSLSocket
import kotlin.coroutines.coroutineContext

/**
 * A live remote-control session on port 6466.
 *
 * Requires that [PairingSession] has already run against this TV — the connection is
 * refused otherwise, since the TV only accepts certificates it has been told to trust.
 *
 * The TV drives the opening handshake: it sends `remote_configure` advertising what it
 * supports, then `remote_set_active`, then `remote_start`. It also pings roughly every
 * five seconds and hangs up after three unanswered pings, so the read loop must keep
 * running for the whole session — this is not a request/response protocol.
 */
class RemoteClient(
    private val host: String,
    private val keyStore: KeyStore,
    private val password: CharArray,
    private val deviceModel: String = "Android Remote",
    private val deviceVendor: String = "androidremote",
    private val port: Int = REMOTE_PORT,
    private val enableIme: Boolean = true,
    private val enableVoice: Boolean = true,
) : Closeable {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** False while the TV is in standby. Commands still reach it; POWER wakes it. */
    private val _powerOn = MutableStateFlow(false)
    val powerOn: StateFlow<Boolean> = _powerOn.asStateFlow()

    private val _volume = MutableStateFlow<VolumeInfo?>(null)
    val volume: StateFlow<VolumeInfo?> = _volume.asStateFlow()

    private val _currentApp = MutableStateFlow<String?>(null)
    val currentApp: StateFlow<String?> = _currentApp.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    /** Diagnostics sink; wired to Logcat on Android and stdout in the CLI. */
    var onLog: ((String) -> Unit)? = null

    /**
     * Every socket write goes through this single thread.
     *
     * Two reasons. Compose click handlers call [sendKey] directly on the main thread, and a
     * blocking TLS write there trips StrictMode's NetworkOnMainThreadException. And routing
     * all writers through one thread keeps outbound messages strictly ordered, which the
     * protocol's framing depends on — two interleaved writes would corrupt the stream.
     */
    private var writer: ExecutorService? = null
    private var socket: SSLSocket? = null
    private var output: OutputStream? = null
    private var scope: CoroutineScope? = null

    private var activeFeatures = 0
    private var imeCounter = 0
    private var imeFieldCounter = 0

    private var readySignal: CompletableDeferred<Unit>? = null
    private var voiceBeginSignal: CompletableDeferred<Int>? = null

    /** Connects and returns once the TV has completed its handshake and accepts commands. */
    suspend fun connect(readyTimeoutMs: Long = READY_TIMEOUT_MS) {
        disconnect()
        _state.value = ConnectionState.Connecting

        val ready = CompletableDeferred<Unit>()
        readySignal = ready
        try {
            val ssl = withContext(Dispatchers.IO) {
                Tls.connect(keyStore, password, host, port, CONNECT_TIMEOUT_MS, SOCKET_READ_TIMEOUT_MS)
            }
            socket = ssl
            output = ssl.outputStream
            writer = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "atv-remote-writer").apply { isDaemon = true }
            }

            val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope = sessionScope
            sessionScope.launch { readLoop(ssl) }

            withTimeout(readyTimeoutMs) { ready.await() }
            _state.value = ConnectionState.Ready
            log("Session ready; features ${Feature.describe(activeFeatures)}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            teardown()
            _state.value = ConnectionState.Failed(e)
            throw RemoteException("Could not start a remote session with $host:$port", e)
        }
    }

    fun disconnect() {
        teardown()
        if (_state.value != ConnectionState.Disconnected) {
            _state.value = ConnectionState.Disconnected
        }
    }

    override fun close() = disconnect()

    // ---- commands -----------------------------------------------------------------

    fun sendKey(keyCode: RemoteKeyCode, direction: RemoteDirection = RemoteDirection.SHORT) {
        send(
            RemoteMessage.newBuilder().setRemoteKeyInject(
                RemoteKeyInject.newBuilder().setKeyCode(keyCode).setDirection(direction),
            ),
        )
    }

    /** Convenience for raw Android keycodes, e.g. 19 for DPAD_UP. */
    fun sendKey(keyCode: Int, direction: RemoteDirection = RemoteDirection.SHORT) {
        val resolved = RemoteKeyCode.forNumber(keyCode)
            ?: throw IllegalArgumentException("Unknown key code $keyCode")
        sendKey(resolved, direction)
    }

    /** Launches an app by deep link, e.g. `https://www.netflix.com/title`. */
    fun launchApp(appLink: String) {
        send(
            RemoteMessage.newBuilder().setRemoteAppLinkLaunchRequest(
                RemoteAppLinkLaunchRequest.newBuilder().setAppLink(appLink),
            ),
        )
    }

    /**
     * Types [text] into whatever field the TV has focused, via the remote IME.
     *
     * The counters come from the TV's own `remote_ime_batch_edit` messages; sending stale
     * ones makes the edit silently do nothing, which is why they are tracked in the read
     * loop rather than guessed.
     */
    fun sendText(text: String) {
        require(text.isNotEmpty()) { "Text cannot be empty" }
        val caret = text.length - 1
        send(
            RemoteMessage.newBuilder().setRemoteImeBatchEdit(
                RemoteImeBatchEdit.newBuilder()
                    .setImeCounter(imeCounter)
                    .setFieldCounter(imeFieldCounter)
                    .addEditInfo(
                        RemoteEditInfo.newBuilder()
                            .setInsert(1)
                            .setTextFieldStatus(
                                RemoteImeObject.newBuilder()
                                    .setStart(caret)
                                    .setEnd(caret)
                                    .setValue(text),
                            ),
                    ),
            ),
        )
    }

    /**
     * Opens a voice search session and returns its id.
     *
     * Sends `KEYCODE_SEARCH` and waits for the TV to answer with `remote_voice_begin`,
     * which it only does when voice is among the negotiated features.
     */
    suspend fun startVoice(timeoutMs: Long = VOICE_BEGIN_TIMEOUT_MS): Int {
        if (activeFeatures and Feature.VOICE == 0) {
            throw RemoteException("This TV did not negotiate voice support")
        }
        val signal = CompletableDeferred<Int>()
        voiceBeginSignal = signal

        sendKey(RemoteKeyCode.KEYCODE_SEARCH)
        val sessionId = try {
            withTimeout(timeoutMs) { signal.await() }
        } finally {
            voiceBeginSignal = null
        }

        // The TV expects the begin message echoed back before it will accept audio.
        send(
            RemoteMessage.newBuilder().setRemoteVoiceBegin(
                RemoteVoiceBegin.newBuilder().setSessionId(sessionId),
            ),
        )
        return sessionId
    }

    /**
     * Streams one block of 16-bit PCM mono audio.
     *
     * The schema documents 8 kHz as the expected rate. Chunks below [VOICE_CHUNK_MIN_BYTES]
     * are padded and anything above [VOICE_CHUNK_MAX_BYTES] is split — TVs drop the
     * connection outright on oversized payloads.
     */
    fun sendVoiceChunk(chunk: ByteArray, sessionId: Int) {
        val padded = if (chunk.size < VOICE_CHUNK_MIN_BYTES) {
            chunk.copyOf(VOICE_CHUNK_MIN_BYTES)
        } else {
            chunk
        }
        var offset = 0
        while (offset < padded.size) {
            val end = minOf(offset + VOICE_CHUNK_MAX_BYTES, padded.size)
            send(
                RemoteMessage.newBuilder().setRemoteVoicePayload(
                    RemoteVoicePayload.newBuilder()
                        .setSessionId(sessionId)
                        .setSamples(ByteString.copyFrom(padded, offset, end - offset)),
                ),
                quiet = true,
            )
            offset = end
        }
    }

    fun endVoice(sessionId: Int) {
        send(
            RemoteMessage.newBuilder().setRemoteVoiceEnd(
                RemoteVoiceEnd.newBuilder().setSessionId(sessionId),
            ),
        )
    }

    // ---- session plumbing ---------------------------------------------------------

    private suspend fun readLoop(ssl: SSLSocket) {
        val input = ssl.inputStream
        try {
            while (coroutineContext.isActive) {
                val raw = Framing.readMessage(input)
                if (raw == null) {
                    onConnectionLost(RemoteException("TV closed the remote connection"))
                    return
                }
                handle(RemoteMessage.parseFrom(raw))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onConnectionLost(e)
        }
    }

    private fun handle(message: RemoteMessage) {
        when {
            message.hasRemoteConfigure() -> {
                val supported = message.remoteConfigure.code1
                activeFeatures = requestedFeatures() and supported
                val info = message.remoteConfigure.deviceInfo
                _deviceInfo.value = DeviceInfo(
                    vendor = info.vendor,
                    model = info.model,
                    appVersion = info.appVersion,
                )
                log("TV ${info.vendor} ${info.model} supports ${Feature.describe(supported)}")
                if (supported and Feature.KEY == 0) {
                    log("WARNING: TV reports no KEY support. $REMOTE_SERVICE_HINT")
                }
                send(
                    RemoteMessage.newBuilder().setRemoteConfigure(
                        RemoteConfigure.newBuilder()
                            .setCode1(activeFeatures)
                            .setDeviceInfo(
                                RemoteDeviceInfo.newBuilder()
                                    .setModel(deviceModel)
                                    .setVendor(deviceVendor)
                                    .setUnknown1(1)
                                    .setUnknown2("1")
                                    .setPackageName(PACKAGE_NAME)
                                    .setAppVersion(APP_VERSION),
                            ),
                    ),
                )
            }

            message.hasRemoteSetActive() -> send(
                RemoteMessage.newBuilder().setRemoteSetActive(
                    RemoteSetActive.newBuilder().setActive(activeFeatures),
                ),
            )

            message.hasRemotePingRequest() -> send(
                RemoteMessage.newBuilder().setRemotePingResponse(
                    RemotePingResponse.newBuilder().setVal1(message.remotePingRequest.val1),
                ),
                quiet = true,
            )

            message.hasRemoteStart() -> {
                _powerOn.value = message.remoteStart.started
                readySignal?.complete(Unit)
            }

            message.hasRemoteSetVolumeLevel() -> {
                val volumeMessage = message.remoteSetVolumeLevel
                _volume.value = VolumeInfo(
                    level = volumeMessage.volumeLevel,
                    max = volumeMessage.volumeMax,
                    muted = volumeMessage.volumeMuted,
                )
            }

            message.hasRemoteImeKeyInject() ->
                _currentApp.value = message.remoteImeKeyInject.appInfo.appPackage

            message.hasRemoteImeBatchEdit() -> {
                imeCounter = message.remoteImeBatchEdit.imeCounter
                imeFieldCounter = message.remoteImeBatchEdit.fieldCounter
            }

            message.hasRemoteVoiceBegin() ->
                voiceBeginSignal?.complete(message.remoteVoiceBegin.sessionId)

            message.hasRemoteError() -> log("TV reported an error: ${message.remoteError.value}")

            else -> Unit
        }
    }

    private fun requestedFeatures(): Int {
        var mask = Feature.PING or Feature.KEY or Feature.POWER or Feature.VOLUME or Feature.APP_LINK
        if (enableIme) mask = mask or Feature.IME
        if (enableVoice) mask = mask or Feature.VOICE
        return mask
    }

    /**
     * Queues a message for sending. Returns immediately and never throws: a key press is
     * fire-and-forget, and a write that fails means the session is gone, which surfaces
     * through [state] so the caller can reconnect.
     */
    private fun send(builder: RemoteMessage.Builder, quiet: Boolean = false) {
        val queue = writer ?: return
        val bytes = builder.build().toByteArray()
        val submitted = runCatching {
            queue.execute {
                val stream = output ?: return@execute
                try {
                    Framing.writeMessage(stream, bytes)
                    if (!quiet) log("sent ${bytes.size} bytes")
                } catch (e: Exception) {
                    // Not only IOException: a socket torn down underneath us surfaces as
                    // IllegalStateException from the TLS provider on some devices.
                    onConnectionLost(e)
                }
            }
        }
        // The executor rejects work once the session has been torn down.
        if (submitted.isFailure) log("dropped a message; the session is closing")
    }

    private fun onConnectionLost(cause: Throwable) {
        val signal = readySignal
        if (signal != null && !signal.isCompleted) {
            signal.completeExceptionally(cause)
        }
        if (_state.value != ConnectionState.Disconnected) {
            _state.value = ConnectionState.Failed(cause)
        }
        log("Connection lost: ${cause.message}")
        closeSocketOnly()
    }

    private fun teardown() {
        writer?.shutdownNow()
        writer = null
        scope?.cancel()
        scope = null
        readySignal = null
        voiceBeginSignal = null
        closeSocketOnly()
    }

    private fun closeSocketOnly() {
        runCatching { socket?.close() }
        socket = null
        output = null
    }

    private fun log(message: String) {
        onLog?.invoke(message)
    }

    companion object {
        const val VOICE_SAMPLE_RATE_HZ = 8_000
        const val VOICE_CHUNK_MAX_BYTES = 20 * 1024
        const val VOICE_CHUNK_MIN_BYTES = 8 * 1024

        const val REMOTE_SERVICE_HINT =
            "On the TV, go to Settings > Apps > See all apps > Show system apps > " +
                "Android TV Remote Service > Storage > Clear data, then pair again."

        private const val PACKAGE_NAME = "com.radiotv.tvremote"
        private const val APP_VERSION = "1.0.0"
        private const val CONNECT_TIMEOUT_MS = 10_000

        /**
         * Dead-peer detection. The TV pings every ~5 s while idle, so half a minute of
         * total silence means it is gone (powered off, off the network, or hung).
         */
        private const val SOCKET_READ_TIMEOUT_MS = 30_000
        private const val READY_TIMEOUT_MS = 15_000L
        private const val VOICE_BEGIN_TIMEOUT_MS = 3_000L
    }
}
