package com.radiotv.tvremote.androidtv.protocol

import com.google.polo.wire.protobuf.PoloProto.Configuration
import com.google.polo.wire.protobuf.PoloProto.Options
import com.google.polo.wire.protobuf.PoloProto.OuterMessage
import com.google.polo.wire.protobuf.PoloProto.PairingRequest
import com.google.polo.wire.protobuf.PoloProto.Secret
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket

/**
 * The Polo pairing handshake on port 6467.
 *
 * Two-step by nature: [begin] runs the handshake up to the point where the TV puts a
 * six-character code on screen, then [complete] proves we can see it. The socket must stay
 * open between the two — the code is bound to this TLS session's certificates.
 *
 * ```
 * client                                    TV
 *   |-- pairing_request ------------------->|
 *   |<------------------ pairing_request_ack|
 *   |-- options --------------------------->|
 *   |<----------------------------- options |
 *   |-- configuration --------------------->|
 *   |<-------------------- configuration_ack|   TV displays the code
 *   |-- secret ---------------------------->|
 *   |<-------------------------- secret_ack |   paired
 * ```
 */
class PairingSession(
    private val host: String,
    private val keyStore: KeyStore,
    private val password: CharArray,
    private val clientName: String,
    private val port: Int = PAIRING_PORT,
) : Closeable {

    private var socket: SSLSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var serverCertificate: X509Certificate? = null

    /**
     * Connects and negotiates until the TV displays its pairing code.
     * Returns the TV's self-reported name when it offers one.
     */
    suspend fun begin(): String? = withContext(Dispatchers.IO) {
        val ssl = Tls.connect(keyStore, password, host, port, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)
        socket = ssl
        input = ssl.inputStream
        output = ssl.outputStream
        serverCertificate = Tls.peerCertificate(ssl)

        send(
            baseMessage().setPairingRequest(
                PairingRequest.newBuilder()
                    .setServiceName(SERVICE_NAME)
                    .setClientName(clientName),
            ),
        )

        var serverName: String? = null
        while (true) {
            val message = receive()
            when {
                message.hasPairingRequestAck() -> {
                    if (message.pairingRequestAck.hasServerName()) {
                        serverName = message.pairingRequestAck.serverName
                    }
                    send(
                        baseMessage().setOptions(
                            Options.newBuilder()
                                .setPreferredRole(Options.RoleType.ROLE_TYPE_INPUT)
                                .addInputEncodings(hexadecimalEncoding()),
                        ),
                    )
                }

                message.hasOptions() -> send(
                    baseMessage().setConfiguration(
                        Configuration.newBuilder()
                            .setClientRole(Options.RoleType.ROLE_TYPE_INPUT)
                            .setEncoding(hexadecimalEncoding()),
                    ),
                )

                // The TV is now showing the code; hand control back to the caller.
                message.hasConfigurationAck() -> break

                else -> throw PairingException("Unexpected message during pairing: ${message.describe()}")
            }
        }
        serverName
    }

    /**
     * Proves we can see [code] and finishes pairing. After this returns, the TV trusts our
     * certificate and [RemoteClient] can connect on port 6466.
     */
    suspend fun complete(code: String): Unit = withContext(Dispatchers.IO) {
        val server = serverCertificate ?: throw PairingException("complete() called before begin()")
        val secret = PairingSecret.compute(
            clientCertificate = ClientIdentity.certificateOf(keyStore),
            serverCertificate = server,
            code = code,
        )

        send(baseMessage().setSecret(Secret.newBuilder().setSecret(ByteString.copyFrom(secret))))

        val reply = receive()
        if (!reply.hasSecretAck()) {
            throw PairingException("TV rejected the pairing code (got ${reply.describe()})")
        }
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
        input = null
        output = null
    }

    private fun baseMessage(): OuterMessage.Builder = OuterMessage.newBuilder()
        .setProtocolVersion(PROTOCOL_VERSION)
        .setStatus(OuterMessage.Status.STATUS_OK)

    private fun hexadecimalEncoding(): Options.Encoding.Builder =
        Options.Encoding.newBuilder()
            .setType(Options.Encoding.EncodingType.ENCODING_TYPE_HEXADECIMAL)
            .setSymbolLength(SYMBOL_LENGTH)

    private fun send(builder: OuterMessage.Builder) {
        val stream = output ?: throw PairingException("Pairing socket is closed")
        Framing.writeMessage(stream, builder.build().toByteArray())
    }

    private fun receive(): OuterMessage {
        val stream = input ?: throw PairingException("Pairing socket is closed")
        val raw = Framing.readMessage(stream)
            ?: throw PairingException("TV closed the pairing connection unexpectedly")
        val message = try {
            OuterMessage.parseFrom(raw)
        } catch (e: Exception) {
            throw PairingException("Could not parse a pairing message from the TV", e)
        }
        if (message.status != OuterMessage.Status.STATUS_OK) {
            throw PairingException(
                when (message.status) {
                    OuterMessage.Status.STATUS_BAD_SECRET ->
                        "TV rejected the pairing code. Check the code on screen and try again."

                    OuterMessage.Status.STATUS_BAD_CONFIGURATION ->
                        "TV rejected the pairing configuration."

                    else -> "TV reported pairing status ${message.status}"
                },
            )
        }
        return message
    }

    private fun OuterMessage.describe(): String = when {
        hasPairingRequest() -> "pairing_request"
        hasPairingRequestAck() -> "pairing_request_ack"
        hasOptions() -> "options"
        hasConfiguration() -> "configuration"
        hasConfigurationAck() -> "configuration_ack"
        hasSecret() -> "secret"
        hasSecretAck() -> "secret_ack"
        else -> "unknown"
    }

    private companion object {
        const val PROTOCOL_VERSION = 2
        const val SERVICE_NAME = "atvremote"
        const val SYMBOL_LENGTH = 6
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 60_000 // the user has to read a code off the TV
    }
}
