package com.radiotv.tvremote.core

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.util.Date
import java.util.concurrent.TimeUnit
import java.security.cert.X509Certificate

class AndroidTvIdentity(private val context: Context) {
    private val file = File(context.filesDir, "androidtv-identity.p12")
    private val password = "tvremote-local".toCharArray()

    init {
        if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider())
    }

    fun loadOrCreate(): KeyStore {
        if (file.exists()) {
            return KeyStore.getInstance("PKCS12").apply {
                file.inputStream().use { load(it, password) }
            }
        }
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val cert = selfSigned(pair)
        val ks = KeyStore.getInstance("PKCS12").apply { load(null, password) }
        ks.setKeyEntry("client", pair.private, password, arrayOf(cert))
        file.outputStream().use { ks.store(it, password) }
        return ks
    }

    fun clientCertificate(): X509Certificate =
        loadOrCreate().getCertificate("client") as X509Certificate

    fun keyStorePassword(): CharArray = password.copyOf()

    private fun selfSigned(pair: KeyPair): X509Certificate {
        val now = Date()
        val end = Date(now.time + TimeUnit.DAYS.toMillis(3650))
        val subject = X500Name("CN=Radio.TV Remote")
        val builder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(System.currentTimeMillis()),
            now,
            end,
            subject,
            pair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(pair.private)
        return JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer))
    }
}
