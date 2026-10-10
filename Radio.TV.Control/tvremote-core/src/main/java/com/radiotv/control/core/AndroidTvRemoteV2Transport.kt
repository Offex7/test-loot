package com.radiotv.control.core

import android.content.Context
import com.google.polo.wire.protobuf.PoloProto
import com.radiotv.control.core.proto.RemoteMessageProto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/** Android TV Remote v2 transport: pairing on TLS port 6467 and control channel on 6466. */
class AndroidTvRemoteV2Transport(context: Context) : RemoteTransport, Closeable {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("remote-v2", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val statusMutable = MutableStateFlow<RemoteStatus>(RemoteStatus.Disconnected)
    override val status: StateFlow<RemoteStatus> = statusMutable.asStateFlow()
    private var pendingPairing: PendingPairing? = null
    private var remoteSocket: SSLSocket? = null
    private var readerJob: Job? = null
    private var remoteOutput: DataOutputStream? = null

    override suspend fun beginPairing(host: String) = withContext(Dispatchers.IO) {
        val target = host.trim()
        require(target.isNotBlank()) { "Введите IP-адрес телевизора." }
        closePendingPairing()
        closeRemoteSocket()
        statusMutable.value = RemoteStatus.Connecting(target)
        try {
            val identity = loadOrCreateIdentity()
            val socket = newTlsSocket(identity, null)
            socket.connect(InetSocketAddress(target, PAIRING_PORT), CONNECT_TIMEOUT_MS)
            socket.soTimeout = IO_TIMEOUT_MS
            socket.startHandshake()
            val serverCertificate = socket.session.peerCertificates.firstOrNull() as? X509Certificate
                ?: error("Телевизор не предоставил TLS-сертификат.")
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            sendPolo(output, PoloProto.OuterMessage.newBuilder().setPairingRequest(
                PoloProto.PairingRequest.newBuilder().setServiceName("atvremote").setClientName("Radio.TV.Control")
            ))
            check(readPolo(input).hasPairingRequestAck()) { "Телевизор не подтвердил запрос на сопряжение." }

            val encoding = PoloProto.Options.Encoding.newBuilder()
                .setType(PoloProto.Options.Encoding.EncodingType.ENCODING_TYPE_HEXADECIMAL)
                .setSymbolLength(PIN_LENGTH).build()
            sendPolo(output, PoloProto.OuterMessage.newBuilder().setOptions(
                PoloProto.Options.newBuilder().addInputEncodings(encoding)
                    .setPreferredRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT)
            ))
            readPolo(input)

            sendPolo(output, PoloProto.OuterMessage.newBuilder().setConfiguration(
                PoloProto.Configuration.newBuilder().setEncoding(encoding)
                    .setClientRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT)
            ))
            check(readPolo(input).hasConfigurationAck()) { "Телевизор не принял параметры сопряжения." }
            pendingPairing = PendingPairing(target, socket, input, output, identity.certificate, serverCertificate)
            statusMutable.value = RemoteStatus.AwaitingCode(target)
        } catch (error: Exception) {
            statusMutable.value = RemoteStatus.Error(error.message ?: "Ошибка сопряжения.")
            closePendingPairing()
            throw error
        }
    }

    override suspend fun completePairing(code: String) = withContext(Dispatchers.IO) {
        val pending = checkNotNull(pendingPairing) { "Сначала нажмите «Начать сопряжение»." }
        val pin = code.trim().uppercase().filter { it in "0123456789ABCDEF" }
        require(pin.length == PIN_LENGTH) { "Введите шестизначный HEX-код с экрана телевизора." }
        try {
            val clientKey = pending.clientCertificate.publicKey as? RSAPublicKey ?: error("Ключ клиента не RSA.")
            val serverKey = pending.serverCertificate.publicKey as? RSAPublicKey ?: error("Ключ телевизора не RSA.")
            val pinBytes = pin.substring(2).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(clientKey.modulus.toUnsignedBytes())
            digest.update(clientKey.publicExponent.toUnsignedBytes())
            digest.update(serverKey.modulus.toUnsignedBytes())
            digest.update(serverKey.publicExponent.toUnsignedBytes())
            digest.update(pinBytes)
            val secret = digest.digest()
            require((secret[0].toInt() and 0xFF) == pin.substring(0, 2).toInt(16)) {
                "Код не совпадает. Проверьте PIN на экране телевизора."
            }
            sendPolo(pending.output, PoloProto.OuterMessage.newBuilder().setSecret(
                PoloProto.Secret.newBuilder().setSecret(com.google.protobuf.ByteString.copyFrom(secret))
            ))
            check(readPolo(pending.input).hasSecretAck()) { "Телевизор отклонил код сопряжения." }

            val fingerprint = sha256Hex(pending.serverCertificate.encoded)
            preferences.edit().putString(KEY_HOST, pending.host).putString(KEY_SERVER_FINGERPRINT, fingerprint).apply()
            val pairedHost = pending.host
            closePendingPairing()
            openRemoteConnection(pairedHost, fingerprint)
        } catch (error: Exception) {
            statusMutable.value = RemoteStatus.Error(error.message ?: "Не удалось завершить сопряжение.")
            closePendingPairing()
            throw error
        }
    }

    override suspend fun connect(host: String) = withContext(Dispatchers.IO) {
        val target = host.trim().ifEmpty { preferences.getString(KEY_HOST, "").orEmpty() }
        require(target.isNotBlank()) { "Введите IP-адрес телевизора." }
        val fingerprint = preferences.getString(KEY_SERVER_FINGERPRINT, null)
            ?: error("Сначала выполните сопряжение.")
        closeRemoteSocket()
        statusMutable.value = RemoteStatus.Connecting(target)
        try { openRemoteConnection(target, fingerprint) } catch (error: Exception) {
            statusMutable.value = RemoteStatus.Error(error.message ?: "Не удалось подключиться.")
            throw error
        }
    }

    override suspend fun sendKey(key: RemoteKey) = withContext(Dispatchers.IO) {
        val output = checkNotNull(remoteOutput) { "Сначала подключитесь к телевизору." }
        check(remoteSocket?.isConnected == true) { "Нет соединения с телевизором." }
        val message = RemoteMessageProto.RemoteMessage.newBuilder().setRemoteKeyInject(
            RemoteMessageProto.RemoteKeyInject.newBuilder()
                .setKeyCode(key.wireCode).setDirection(RemoteMessageProto.RemoteDirection.SHORT)
        ).build()
        writeRemote(output, message)
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        closePendingPairing()
        closeRemoteSocket()
        statusMutable.value = RemoteStatus.Disconnected
    }

    private suspend fun openRemoteConnection(host: String, fingerprint: String?) {
        val identity = loadOrCreateIdentity()
        val socket = newTlsSocket(identity, fingerprint)
        try {
            socket.connect(InetSocketAddress(host, REMOTE_PORT), CONNECT_TIMEOUT_MS)
            socket.soTimeout = IO_TIMEOUT_MS
            socket.startHandshake()
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            // The TV initiates the protocol negotiation with the supported-feature bit mask.
            val greeting = readRemote(input)
            check(greeting.hasRemoteConfigure()) { "Телевизор не прислал RemoteConfigure." }
            val supportedFeatures = greeting.remoteConfigure.code1
            val activeFeatures = supportedFeatures and (FEATURE_PING or FEATURE_KEY)
            check((activeFeatures and FEATURE_KEY) != 0) { "Телевизор не поддерживает управление клавишами." }

            writeRemote(output, RemoteMessageProto.RemoteMessage.newBuilder().setRemoteConfigure(
                RemoteMessageProto.RemoteConfigure.newBuilder().setCode1(activeFeatures).setDeviceInfo(
                    RemoteMessageProto.RemoteDeviceInfo.newBuilder()
                        .setModel("Radio.TV.Control")
                        .setVendor("Offex7")
                        .setUnknown1(1)
                        .setUnknown2("1")
                        .setPackageName(appContext.packageName)
                        .setAppVersion("0.1.0")
                )
            ).build())

            var activationResponded = false
            while (!activationResponded) {
                val message = readRemote(input)
                when {
                    message.hasRemoteSetActive() -> {
                        writeRemote(output, RemoteMessageProto.RemoteMessage.newBuilder().setRemoteSetActive(
                            RemoteMessageProto.RemoteSetActive.newBuilder().setActive(activeFeatures)
                        ).build())
                        activationResponded = true
                    }
                    message.hasRemotePingRequest() -> {
                        writeRemote(output, RemoteMessageProto.RemoteMessage.newBuilder().setRemotePingResponse(
                            RemoteMessageProto.RemotePingResponse.newBuilder().setVal1(message.remotePingRequest.val1)
                        ).build())
                    }
                    message.hasRemoteError() -> error("Телевизор отклонил протокол Remote v2.")
                }
            }

            socket.soTimeout = 0
            remoteSocket = socket
            remoteOutput = output
            statusMutable.value = RemoteStatus.Connected(host)
            readerJob?.cancel()
            readerJob = scope.launch {
                try {
                    while (true) {
                        val message = readRemote(input)
                        if (message.hasRemotePingRequest()) {
                            writeRemote(output, RemoteMessageProto.RemoteMessage.newBuilder().setRemotePingResponse(
                                RemoteMessageProto.RemotePingResponse.newBuilder().setVal1(message.remotePingRequest.val1)
                            ).build())
                        } else if (message.hasRemoteError()) {
                            error("Телевизор отправил RemoteError.")
                        }
                    }
                } catch (_: Exception) {
                    if (remoteSocket === socket) {
                        statusMutable.value = RemoteStatus.Error("Соединение с телевизором потеряно.")
                        closeRemoteSocket()
                    }
                }
            }
        } catch (error: Exception) {
            runCatching { socket.close() }
            throw error
        }
    }

    private fun newTlsSocket(identity: ClientIdentity, fingerprint: String?): SSLSocket {
        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(identity.keyStore, KEYSTORE_PASSWORD)
        }.keyManagers
        val context = SSLContext.getInstance("TLS")
        context.init(keyManagers, arrayOf<TrustManager>(PairingOrPinnedTrustManager(fingerprint)), SecureRandom())
        return context.socketFactory.createSocket() as SSLSocket
    }

    private fun loadOrCreateIdentity(): ClientIdentity {
        val file = File(appContext.filesDir, KEYSTORE_FILE)
        val keyStore = KeyStore.getInstance("PKCS12")
        if (file.exists()) {
            FileInputStream(file).use { keyStore.load(it, KEYSTORE_PASSWORD) }
            val certificate = keyStore.getCertificate(CLIENT_ALIAS) as X509Certificate
            return ClientIdentity(keyStore, certificate)
        }
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom())
        val pair = generator.generateKeyPair()
        val now = System.currentTimeMillis()
        val subject = X500Name("CN=Radio.TV.Control")
        val builder = JcaX509v3CertificateBuilder(
            subject, BigInteger.valueOf(now), Date(now - 60_000), Date(now + CERT_VALIDITY_MS), subject, pair.public
        )
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
        builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment))
        val certificate = JcaX509CertificateConverter().getCertificate(
            builder.build(JcaContentSignerBuilder("SHA256withRSA").build(pair.private))
        )
        keyStore.load(null, KEYSTORE_PASSWORD)
        keyStore.setKeyEntry(CLIENT_ALIAS, pair.private, KEYSTORE_PASSWORD, arrayOf(certificate))
        FileOutputStream(file).use { keyStore.store(it, KEYSTORE_PASSWORD) }
        return ClientIdentity(keyStore, certificate)
    }

    private fun closePendingPairing() {
        runCatching { pendingPairing?.socket?.close() }
        pendingPairing = null
    }

    private fun closeRemoteSocket() {
        readerJob?.cancel()
        readerJob = null
        runCatching { remoteSocket?.close() }
        remoteSocket = null
        remoteOutput = null
    }

    override fun close() {
        closePendingPairing()
        closeRemoteSocket()
        scope.cancel()
    }

    private data class ClientIdentity(val keyStore: KeyStore, val certificate: X509Certificate)
    private data class PendingPairing(
        val host: String, val socket: SSLSocket, val input: DataInputStream, val output: DataOutputStream,
        val clientCertificate: X509Certificate, val serverCertificate: X509Certificate
    )
    private class PairingOrPinnedTrustManager(private val fingerprint: String?) : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            if (fingerprint == null) return
            val encoded = chain.firstOrNull()?.encoded ?: throw CertificateException("Пустая цепочка TLS-сертификатов.")
            if (!sha256Hex(encoded).equals(fingerprint, true)) throw CertificateException("Сертификат ТВ изменился; выполните сопряжение повторно.")
        }
    }

    companion object {
        private const val PAIRING_PORT = 6467
        private const val REMOTE_PORT = 6466
        private const val PIN_LENGTH = 6
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val IO_TIMEOUT_MS = 8_000
        private const val CERT_VALIDITY_MS = 315_360_000_000L
        private const val KEYSTORE_FILE = "remote-client.p12"
        private const val CLIENT_ALIAS = "radiotv-control"
        private val KEYSTORE_PASSWORD = "radiotv-control".toCharArray()
        private const val KEY_HOST = "paired_host"
        private const val KEY_SERVER_FINGERPRINT = "server_fingerprint"
        private const val MAX_FRAME_SIZE = 1_048_576
        private const val FEATURE_PING = 1
        private const val FEATURE_KEY = 2
        private val outputLock = Any()

        private fun sendPolo(out: DataOutputStream, message: PoloProto.OuterMessage.Builder) {
            writeFrame(out, message.setProtocolVersion(2).setStatus(PoloProto.OuterMessage.Status.STATUS_OK).build().toByteArray())
        }
        private fun readPolo(input: DataInputStream) = PoloProto.OuterMessage.parseFrom(readFrame(input))
        private fun writeRemote(out: DataOutputStream, message: RemoteMessageProto.RemoteMessage) {
            synchronized(outputLock) { writeFrame(out, message.toByteArray()) }
        }
        private fun readRemote(input: DataInputStream) = RemoteMessageProto.RemoteMessage.parseFrom(readFrame(input))
        private fun writeFrame(out: DataOutputStream, bytes: ByteArray) {
            var remaining = bytes.size
            while (remaining >= 0x80) {
                out.writeByte((remaining and 0x7F) or 0x80)
                remaining = remaining ushr 7
            }
            out.writeByte(remaining)
            out.write(bytes)
            out.flush()
        }
        private fun readFrame(input: DataInputStream): ByteArray {
            var length = 0
            var shift = 0
            while (true) {
                val next = input.readUnsignedByte()
                length = length or ((next and 0x7F) shl shift)
                if ((next and 0x80) == 0) break
                shift += 7
                require(shift < 32) { "Слишком длинный varint protobuf frame." }
            }
            require(length in 1..MAX_FRAME_SIZE) { "Некорректный размер protobuf frame: $length" }
            return ByteArray(length).also { input.readFully(it) }
        }
        private fun BigInteger.toUnsignedBytes(): ByteArray {
            val bytes = toByteArray()
            return if (bytes.size > 1 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }
        private fun sha256Hex(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
