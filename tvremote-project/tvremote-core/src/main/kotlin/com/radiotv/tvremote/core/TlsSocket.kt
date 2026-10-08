package com.radiotv.tvremote.core
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLSocket
object TlsSocket {
    fun connect(host: String, port: Int, keyStore: KeyStore, password: CharArray, timeoutMs: Int = 10_000): SSLSocket {
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, password)
        val trustAll = object : X509TrustManager {
            override fun getAcceptedIssuers() = emptyArray<java.security.cert.X509Certificate>()
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
        }
        val context = SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, arrayOf(trustAll), SecureRandom())
        }
        return (context.socketFactory.createSocket() as SSLSocket).apply {
            useClientMode = true
            soTimeout = 30_000
            connect(java.net.InetSocketAddress(host, port), timeoutMs)
            startHandshake()
        }
    }
}
