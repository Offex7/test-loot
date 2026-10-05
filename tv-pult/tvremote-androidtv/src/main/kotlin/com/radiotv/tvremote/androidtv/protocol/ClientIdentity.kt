package com.radiotv.tvremote.androidtv.protocol

import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date

/**
 * The self-signed RSA client certificate this app presents to every TV.
 *
 * One identity is reused across all TVs, matching the reference implementations: pairing
 * is the TV deciding to trust *this* certificate, so regenerating it would invalidate
 * every existing pairing.
 *
 * The key is a plain software key rather than an AndroidKeyStore one on purpose —
 * hardware-backed RSA keys drag in TLS 1.3 RSA-PSS signing edge cases across vendors, and
 * app-private storage is already the security boundary for a LAN pairing credential.
 */
object ClientIdentity {

    const val KEY_ALIAS = "atvremote-client"

    /** Loads the PKCS12 identity at [file], generating and persisting one if absent. */
    fun loadOrCreate(
        file: File,
        password: CharArray,
        commonName: String = "atvremote",
    ): KeyStore {
        val keyStore = KeyStore.getInstance("PKCS12")
        if (file.exists()) {
            file.inputStream().use { keyStore.load(it, password) }
            if (keyStore.containsAlias(KEY_ALIAS)) return keyStore
        } else {
            keyStore.load(null, password)
        }

        val (keyPair, certificate) = generateSelfSigned(commonName)
        keyStore.setKeyEntry(KEY_ALIAS, keyPair.private, password, arrayOf(certificate))

        file.parentFile?.mkdirs()
        file.outputStream().use { keyStore.store(it, password) }
        return keyStore
    }

    fun certificateOf(keyStore: KeyStore): X509Certificate =
        keyStore.getCertificate(KEY_ALIAS) as? X509Certificate
            ?: throw IllegalStateException("Key store has no certificate under '$KEY_ALIAS'")

    private fun generateSelfSigned(commonName: String): Pair<KeyPair, X509Certificate> {
        val keyPair = KeyPairGenerator.getInstance("RSA")
            .apply { initialize(2048, SecureRandom()) }
            .generateKeyPair()

        val subject = X500NameBuilder(BCStyle.INSTANCE)
            .addRDN(BCStyle.CN, commonName)
            .build()

        val now = System.currentTimeMillis()
        val builder = JcaX509v3CertificateBuilder(
            /* issuer = */ subject, // self-signed: issuer and subject are the same
            /* serial = */ BigInteger.valueOf(now),
            /* notBefore = */ Date(now - ONE_DAY_MS), // backdated against clock skew
            /* notAfter = */ Date(now + TEN_YEARS_MS),
            /* subject = */ subject,
            /* publicKey = */ keyPair.public,
        )

        // Deliberately no .setProvider("BC"): Android ships its own trimmed-down
        // BouncyCastle, and forcing the "BC" provider name collides with it. The platform
        // JCA provider signs this perfectly well.
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certificate = JcaX509CertificateConverter().getCertificate(builder.build(signer))
        return keyPair to certificate
    }

    private const val ONE_DAY_MS = 24L * 60 * 60 * 1000
    private const val TEN_YEARS_MS = 10L * 365 * 24 * 60 * 60 * 1000
}
