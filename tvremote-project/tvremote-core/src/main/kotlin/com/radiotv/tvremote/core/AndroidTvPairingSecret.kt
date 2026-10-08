package com.radiotv.tvremote.core
import java.math.BigInteger
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
object AndroidTvPairingSecret {
    fun compute(client: X509Certificate, server: X509Certificate, code: String): ByteArray {
        val normalized = code.trim().uppercase()
        if (normalized.length != 6 || normalized.any { it !in "0123456789ABCDEF" }) {
            throw PairingException("Введите 6-символьный HEX-код с телевизора")
        }
        val codeBytes = ByteArray(3)
        for (i in 0 until 3) {
            codeBytes[i] = ((Character.digit(normalized[i * 2], 16) shl 4) or
                Character.digit(normalized[i * 2 + 1], 16)).toByte()
        }
        val c = client.publicKey as? RSAPublicKey ?: throw PairingException("Клиентский сертификат не RSA")
        val s = server.publicKey as? RSAPublicKey ?: throw PairingException("Сертификат TV не RSA")
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(unsigned(c.modulus))
            update(unsigned(c.publicExponent))
            update(unsigned(s.modulus))
            update(unsigned(s.publicExponent))
            update(codeBytes, 1, 2)
        }.digest()
        if (digest[0] != codeBytes[0]) {
            throw PairingException("Код TV не прошёл проверку контрольного байта")
        }
        return digest
    }
    private fun unsigned(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        return if (raw.size > 1 && raw[0] == 0.toByte()) raw.copyOfRange(1, raw.size) else raw
    }
}
