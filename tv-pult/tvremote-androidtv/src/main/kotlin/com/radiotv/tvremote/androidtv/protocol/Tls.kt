package com.radiotv.tvremote.androidtv.protocol

import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

internal object Tls {

    /**
     * An [SSLSocketFactory] that presents our client certificate and accepts the TV's.
     *
     * Certificate validation is intentionally disabled. Android TVs present a self-signed
     * certificate that chains to no CA, so there is nothing to validate against — the
     * pairing handshake is the trust decision, and it binds to the peer certificate's RSA
     * parameters via [PairingSecret]. Substituting a different certificate produces a
     * different pairing digest, so a man-in-the-middle cannot complete pairing without the
     * code shown on the TV screen.
     */
    fun socketFactory(keyStore: KeyStore, password: CharArray): SSLSocketFactory {
        val keyManagers = KeyManagerFactory
            .getInstance(KeyManagerFactory.getDefaultAlgorithm())
            .apply { init(keyStore, password) }
            .keyManagers

        return SSLContext.getInstance("TLS")
            .apply { init(keyManagers, arrayOf(AcceptAnyServerCertificate), SecureRandom()) }
            .socketFactory
    }

    fun connect(
        keyStore: KeyStore,
        password: CharArray,
        host: String,
        port: Int,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
    ): SSLSocket = (socketFactory(keyStore, password).createSocket() as SSLSocket).apply {
        soTimeout = readTimeoutMs
        tcpNoDelay = true // key presses are tiny and latency-sensitive
        connect(java.net.InetSocketAddress(host, port), connectTimeoutMs)
        startHandshake()
    }

    fun peerCertificate(socket: SSLSocket): X509Certificate =
        socket.session.peerCertificates.firstOrNull() as? X509Certificate
            ?: throw RemoteException("TV did not present an X.509 certificate")

    private object AcceptAnyServerCertificate : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
