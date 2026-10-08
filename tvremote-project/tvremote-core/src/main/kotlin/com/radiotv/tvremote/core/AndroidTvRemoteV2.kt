package com.radiotv.tvremote.core

import android.content.Context
import com.google.protobuf.ByteString
import com.radiotv.tvremote.core.proto.PoloProto
import com.radiotv.tvremote.core.proto.RemoteProto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private var voiceActive = AtomicBoolean(false)
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
                    sendPair(out, PoloProto.OuterMessage.newBuilder().setPairingRequest(
                        PoloProto.PairingRequest.newBuilder()
                            .setServiceName("atvremote")
                            .setClientName("Radio.TV Remote")
                    ))
                    while (true) {
                        val message = receivePair(input)
                        when {
                            message.hasPairingRequestAck() -> sendPair(out,
                                PoloProto.OuterMessage.newBuilder().setOptions(
                                    PoloProto.Options.newBuilder()
                                        .setPreferredRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT)
                                        .addInputEncodings(hexEncoding())
                                )
                            )
                            message.hasOptions() -> sendPair(out,
                                PoloProto.OuterMessage.newBuilder().setConfiguration(
                                    PoloProto.Configuration.newBuilder()
                                        .setClientRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT)
                                        .setEncoding(hexEncoding())
                                )
                            )
                            message.hasConfigurationAck() -> break
                            else -> throw PairingException("Неожиданное pairing-сообщение")
                        }
                    }
                    val secret = AndroidTvPairingSecret.compute(identity.clientCertificate(), server, code)
                    sendPair(out, PoloProto.OuterMessage.newBuilder().setSecret(
                        PoloProto.Secret.newBuilder().setSecret(ByteString.copyFrom(secret))
                    ))
                    if (!receivePair(input).hasSecretAck()) {
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

    private fun hexEncoding() = PoloProto.Options.Encoding.newBuilder()
        .setType(PoloProto.Options.Encoding.EncodingType.ENCODING_TYPE_HEXADECIMAL)
        .setSymbolLength(6)

    private fun sendPair(out: OutputStream, builder: PoloProto.OuterMessage.Builder) {
        Framing.writeMessage(
            out,
            builder.setProtocolVersion(2)
                .setStatus(PoloProto.OuterMessage.Status.STATUS_OK)
                .build().toByteArray()
        )
    }

    private fun receivePair(input: InputStream): PoloProto.OuterMessage {
        val raw = Framing.readMessage(input) ?: throw PairingException("TV закрыл pairing-сессию")
        val message = PoloProto.OuterMessage.parseFrom(raw)
        if (message.status != PoloProto.OuterMessage.Status.STATUS_OK) {
            throw PairingException("TV сообщил ошибку pairing: " + message.status)
        }
        return message
    }

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
        }
    }

    private suspend fun readLoop(ssl: SSLSocket) {
        try {
            while (true) {
                val raw = Framing.readMessage(ssl.inputStream) ?: break
                val message = RemoteProto.RemoteMessage.parseFrom(raw)
                when {
                    message.hasRemoteConfigure() -> {
                        val requested = Feature.PING or Feature.KEY or Feature.POWER or Feature.VOLUME or
                            Feature.IME or Feature.VOICE or Feature.APP_LINK
                        val active = requested and message.remoteConfigure.code1
                        sendMessage(RemoteProto.RemoteMessage.newBuilder().setRemoteConfigure(
                            RemoteProto.RemoteConfigure.newBuilder()
                                .setCode1(active)
                                .setDeviceInfo(
                                    RemoteProto.RemoteDeviceInfo.newBuilder()
                                        .setModel("Radio.TV Remote")
                                        .setVendor("Radio.TV")
                                        .setUnknown1(1)
                                        .setUnknown2("1")
                                        .setPackageName("com.radiotv.tvremote")
                                        .setAppVersion("1.0.0")
                                )
                        ))
                    }
                    message.hasRemoteSetActive() -> Unit
                    message.hasRemotePingRequest() -> sendMessage(
                        RemoteProto.RemoteMessage.newBuilder().setRemotePingResponse(
                            RemoteProto.RemotePingResponse.newBuilder().setVal1(message.remotePingRequest.val1)
                        ), quiet = true
                    )
                    message.hasRemoteStart() -> {
                        readySignal?.complete(Unit)
                    }
                    message.hasRemoteImeBatchEdit() -> {
                        imeCounter = message.remoteImeBatchEdit.imeCounter
                        fieldCounter = message.remoteImeBatchEdit.fieldCounter
                    }
                    message.hasRemoteVoiceBegin() -> voiceSignal?.complete(message.remoteVoiceBegin.sessionId)
                }
            }
            if (stateFlow.value != ConnectionState.DISCONNECTED) stateFlow.value = ConnectionState.DISCONNECTED
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
        val code = RemoteProto.RemoteKeyCode.forNumber(key.androidKeyCode)
            ?: throw RemoteException("Клавиша не поддерживается: " + key)
        sendMessage(RemoteProto.RemoteMessage.newBuilder().setRemoteKeyInject(
            RemoteProto.RemoteKeyInject.newBuilder()
                .setKeyCode(code)
                .setDirection(
                    if (longPress) RemoteProto.RemoteDirection.START_LONG
                    else RemoteProto.RemoteDirection.SHORT
                )
        ))
        if (longPress) {
            sendMessage(RemoteProto.RemoteMessage.newBuilder().setRemoteKeyInject(
                RemoteProto.RemoteKeyInject.newBuilder()
                    .setKeyCode(code)
                    .setDirection(RemoteProto.RemoteDirection.END_LONG)
            ))
        }
    }

    private fun sendText(text: String) {
        if (text.isEmpty()) return
        val caret = text.length - 1
        sendMessage(RemoteProto.RemoteMessage.newBuilder().setRemoteImeBatchEdit(
            RemoteProto.RemoteImeBatchEdit.newBuilder()
                .setImeCounter(imeCounter)
                .setFieldCounter(fieldCounter)
                .addEditInfo(RemoteProto.RemoteEditInfo.newBuilder()
                    .setInsert(1)
                    .setTextFieldStatus(
                        RemoteProto.RemoteImeObject.newBuilder()
                            .setStart(caret).setEnd(caret).setValue(text)
                    )
                )
        ))
    }

    suspend fun startVoice(): VoiceSession {
        ensureReady()
        if (voiceActive.getAndSet(true)) throw RemoteException("Голосовой сеанс уже идёт")
        val signal = CompletableDeferred<Int>()
        voiceSignal = signal
        sendKey(TvKey.SEARCH, false)
        val sessionId = try {
            withTimeout(3000) { signal.await() }
        } finally {
            voiceSignal = null
        }
        sendMessage(RemoteProto.RemoteMessage.newBuilder().setRemoteVoiceBegin(
            RemoteProto.RemoteVoiceBegin.newBuilder().setSessionId(sessionId)
        ))
        return object : VoiceSession {
            override suspend fun sendPcm(chunk: ByteArray) = withContext(Dispatchers.IO) {
                var offset = 0
                while (offset < chunk.size) {
                    val end = minOf(offset + 20 * 1024, chunk.size)
                    var part = chunk.copyOfRange(offset, end)
                    if (end == chunk.size && part.size < 8 * 1024) part = part.copyOf(8 * 1024)
                    sendMessage(RemoteProto.RemoteMessage.newBuilder().setRemoteVoicePayload(
                        RemoteProto.RemoteVoicePayload.newBuilder()
                            .setSessionId(sessionId).setSamples(ByteString.copyFrom(part))
                    ), quiet = true)
                    offset = end
                }
            }
            override suspend fun finish() {
                runCatching {
                    sendMessage(RemoteProto.RemoteMessage.newBuilder().setRemoteVoiceEnd(
                        RemoteProto.RemoteVoiceEnd.newBuilder().setSessionId(sessionId)
                    ))
                }
                voiceActive.set(false)
            }
        }
    }

    private fun ensureReady() {
        if (stateFlow.value != ConnectionState.READY) throw RemoteException("Android TV не подключён")
    }

    private fun sendMessage(message: RemoteProto.RemoteMessage.Builder, quiet: Boolean = false) {
        val task = writer ?: throw RemoteException("Сессия TV закрыта")
        task.execute {
            runCatching { Framing.writeMessage(output ?: return@execute, message.build().toByteArray()) }
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
        voiceSignal = null
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
    }
}
