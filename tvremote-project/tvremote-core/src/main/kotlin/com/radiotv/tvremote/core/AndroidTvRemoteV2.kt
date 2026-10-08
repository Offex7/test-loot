package com.radiotv.tvremote.core

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.io.OutputStream
import java.security.cert.X509Certificate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLSocket

class AndroidTvRemoteV2(
    context: Context,
    override val info: RemoteDeviceInfo
) : RemoteDevice {
    private val identity = AndroidTvIdentity(context.applicationContext)
    private val store by lazy { identity.loadOrCreate() }
    private val password get() = identity.keyStorePassword()

    private val stateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val state: StateFlow<ConnectionState> = stateFlow

    override val capabilities = setOf(
        Capability.NAVIGATION, Capability.VOLUME, Capability.CHANNEL, Capability.NUMERIC,
        Capability.POWER, Capability.MUTE, Capability.INPUT, Capability.MEDIA,
        Capability.TEXT, Capability.VOICE
    )

    private var socket: SSLSocket? = null
    private var output: OutputStream? = null
    private var writer: ExecutorService? = null
    private var readScope: CoroutineScope? = null
    private var readySignal: CompletableDeferred<Unit>? = null
    private var voiceSignal: CompletableDeferred<Int>? = null
    private val voiceActive = AtomicBoolean(false)
    private var imeCounter = 0
    private var fieldCounter = 0

    override suspend fun pair(code: String) {
        stateFlow.value = ConnectionState.PAIRING
        try {
            withContext(Dispatchers.IO) {
                TlsSocket.connect(info.host, 6467, store, password).use { ssl ->
                    val server = ssl.session.peerCertificates.first() as X509Certificate
                    val input = ssl.inputStream
                    val out = ssl.outputStream

                    sendPair(out, AndroidTvPairingWire.pairingRequest())

                    var configured = false
                    while (!configured) {
                        when (AndroidTvPairingWire.parse(readPairBytes(input)).kind) {
                            AndroidTvPairingFrame.Kind.PAIRING_REQUEST_ACK ->
                                sendPair(out, AndroidTvPairingWire.options())
                            AndroidTvPairingFrame.Kind.OPTIONS ->
                                sendPair(out, AndroidTvPairingWire.configuration())
                            AndroidTvPairingFrame.Kind.CONFIGURATION_ACK -> configured = true
                            else -> throw PairingException("Неожиданное pairing-сообщение от TV")
                        }
                    }

                    val secret = AndroidTvPairingSecret.compute(identity.clientCertificate(), server, code)
                    sendPair(out, AndroidTvPairingWire.secret(secret))
                    val ack = AndroidTvPairingWire.parse(readPairBytes(input))
                    if (ack.status != 200 || ack.kind != AndroidTvPairingFrame.Kind.SECRET_ACK) {
                        throw PairingException("Телевизор отклонил код сопряжения")
                    }
                }
            }
        } catch (e: Exception) {
            stateFlow.value = ConnectionState.FAILED
            throw e
        } finally {
            if (stateFlow.value == ConnectionState.PAIRING) stateFlow.value = ConnectionState.DISCONNECTED
        }
    }

    private fun readPairBytes(input: InputStream): ByteArray =
        Framing.readMessage(input) ?: throw PairingException("TV закрыл pairing-сессию")

    private fun sendPair(out: OutputStream, payload: ByteArray) = Framing.writeMessage(out, payload)

    override suspend fun connect() {
        disconnect()
        stateFlow.value = ConnectionState.CONNECTING
        val ready = CompletableDeferred<Unit>()
        readySignal = ready
        try {
            withContext(Dispatchers.IO) {
                val ssl = TlsSocket.connect(info.host, 6466, store, password)
                socket = ssl
                output = ssl.outputStream
                writer = Executors.newSingleThreadExecutor { r ->
                    Thread(r, "tvremote-writer").apply { isDaemon = true }
                }
                readScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scope ->
                    scope.launch { readLoop(ssl) }
                }
            }
            withTimeout(15_000) { ready.await() }
            stateFlow.value = ConnectionState.READY
        } catch (e: Exception) {
            disconnect()
            stateFlow.value = ConnectionState.FAILED
            throw RemoteException("Не удалось подключиться к Android TV: " + info.host, e)
        } finally {
            readySignal = null
        }
    }

    private suspend fun readLoop(ssl: SSLSocket) {
        try {
            while (true) {
                val raw = Framing.readMessage(ssl.inputStream) ?: break
                val incoming = AndroidTvRemoteWire.parse(raw)
                incoming.configureFeatures?.let { requested ->
                    val active = requested and Feature.ALL
                    sendMessage(AndroidTvRemoteWire.configure(active))
                }
                incoming.pingValue?.let { sendMessage(AndroidTvRemoteWire.pingResponse(it), quiet = true) }
                incoming.started?.let { if (it) readySignal?.complete(Unit) }
                if (incoming.imeCounter != null && incoming.fieldCounter != null) {
                    imeCounter = incoming.imeCounter
                    fieldCounter = incoming.fieldCounter
                }
                incoming.voiceSessionId?.let { voiceSignal?.complete(it) }
            }
            if (stateFlow.value != ConnectionState.DISCONNECTED) stateFlow.value = ConnectionState.DISCONNECTED
            readySignal?.completeExceptionally(RemoteException("Android TV закрыл соединение"))
        } catch (e: Throwable) {
            if (stateFlow.value != ConnectionState.DISCONNECTED) stateFlow.value = ConnectionState.FAILED
            readySignal?.completeExceptionally(e)
            voiceSignal?.completeExceptionally(e)
        }
    }

    override suspend fun send(command: RemoteCommand) {
        ensureReady()
        when (command) {
            is RemoteCommand.Key -> sendKey(command.code, command.longPress)
            is RemoteCommand.Text -> sendText(command.value)
            else -> throw RemoteException("Android TV Remote v2: pointer не входит в базовый протокол")
        }
    }

    private fun sendKey(key: TvKey, longPress: Boolean) {
        val direction = if (longPress) 1 else 3 // START_LONG / SHORT
        AndroidTvRemoteWire.keyInject(key.androidKeyCode, direction).also { sendMessageBytes(it) }
        if (longPress) sendMessageBytes(AndroidTvRemoteWire.keyInject(key.androidKeyCode, 2)) // END_LONG
    }

    private fun sendText(text: String) {
        if (text.isEmpty()) return
        sendMessageBytes(AndroidTvRemoteWire.imeBatchEdit(imeCounter, fieldCounter, text))
    }

    suspend fun startVoice(): VoiceSession {
        ensureReady()
        if (voiceActive.getAndSet(true)) throw RemoteException("Голосовой сеанс уже идёт")
        val signal = CompletableDeferred<Int>()
        voiceSignal = signal
        sendKey(TvKey.SEARCH, false)
        val sessionId = try {
            withTimeout(3000) { signal.await() }
        } catch (e: Exception) {
            voiceActive.set(false)
            throw RemoteException("Android TV не открыл voice session", e)
        } finally {
            voiceSignal = null
        }
        sendMessageBytes(AndroidTvRemoteWire.voiceBegin(sessionId))
        return object : VoiceSession {
            override suspend fun sendPcm(chunk: ByteArray) = withContext(Dispatchers.IO) {
                var offset = 0
                while (offset < chunk.size) {
                    val end = minOf(offset + 20 * 1024, chunk.size)
                    var part = chunk.copyOfRange(offset, end)
                    if (end == chunk.size && part.size < 8 * 1024) part = part.copyOf(8 * 1024)
                    sendMessageBytes(AndroidTvRemoteWire.voicePayload(sessionId, part), quiet = true)
                    offset = end
                }
            }

            override suspend fun finish() {
                runCatching { sendMessageBytes(AndroidTvRemoteWire.voiceEnd(sessionId)) }
                voiceActive.set(false)
            }
        }
    }

    private fun ensureReady() {
        if (stateFlow.value != ConnectionState.READY) throw RemoteException("Android TV не подключён")
    }

    private fun sendMessage(message: ByteArray, quiet: Boolean = false) = sendMessageBytes(message, quiet)

    private fun sendMessageBytes(message: ByteArray, quiet: Boolean = false) {
        val task = writer ?: throw RemoteException("Сессия TV закрыта")
        task.execute {
            runCatching { Framing.writeMessage(output ?: return@execute, message) }
                .onFailure { stateFlow.value = ConnectionState.FAILED }
        }
    }

    override suspend fun disconnect() {
        readScope?.cancel()
        readScope = null
        writer?.shutdownNow()
        writer = null
        runCatching { socket?.close() }
        socket = null
        output = null
        voiceActive.set(false)
        voiceSignal?.cancel()
        voiceSignal = null
        readySignal?.cancel()
        readySignal = null
        stateFlow.value = ConnectionState.DISCONNECTED
    }

    private object Feature {
        const val PING = 1
        const val KEY = 2
        const val IME = 4
        const val VOICE = 8
        const val POWER = 32
        const val VOLUME = 64
        const val APP_LINK = 512
        const val ALL = PING or KEY or IME or VOICE or POWER or VOLUME or APP_LINK
    }
}
