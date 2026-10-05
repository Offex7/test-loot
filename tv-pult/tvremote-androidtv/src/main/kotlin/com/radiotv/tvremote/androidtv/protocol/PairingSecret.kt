package com.radiotv.tvremote.androidtv.protocol

import java.math.BigInteger
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey

/**
 * Derives the pairing secret proving we can see the code the TV is displaying.
 *
 * The TV shows six hex characters. The first two are a check byte over the digest; the
 * remaining four are hashed together with both certificates' RSA parameters:
 *
 * ```
 * SHA256(clientModulus ‖ clientExponent ‖ serverModulus ‖ serverExponent ‖ code[2..6])
 * ```
 */
object PairingSecret {

    /**
     * Big-endian magnitude bytes, with no sign byte.
     *
     * This is the single most failure-prone line in the whole protocol.
     * [BigInteger.toByteArray] prepends a `0x00` byte whenever the top bit of the
     * magnitude is set — which is *always* true for a freshly generated 2048-bit RSA
     * modulus. The reference implementation hashes Python's `f"{n:X}"` form, which has no
     * such byte, so leaving it in produces a digest that differs from the TV's in every
     * byte, surfacing only as an opaque "bad secret" rejection.
     */
    fun unsignedBytes(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        return if (raw.size > 1 && raw[0] == 0.toByte()) raw.copyOfRange(1, raw.size) else raw
    }

    /**
     * @param code the six hex characters shown on the TV.
     * @throws PairingException if [code] is malformed or fails its check byte.
     */
    fun compute(
        clientCertificate: X509Certificate,
        serverCertificate: X509Certificate,
        code: String,
    ): ByteArray {
        val normalized = code.trim().uppercase()
        if (normalized.length != CODE_LENGTH) {
            throw PairingException("Pairing code must be exactly $CODE_LENGTH characters, got ${normalized.length}")
        }
        val codeBytes = parseHex(normalized)

        val client = clientCertificate.publicKey as? RSAPublicKey
            ?: throw PairingException("Client certificate is not RSA")
        val server = serverCertificate.publicKey as? RSAPublicKey
            ?: throw PairingException("TV certificate is not RSA")

        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(unsignedBytes(client.modulus))
            update(unsignedBytes(client.publicExponent))
            update(unsignedBytes(server.modulus))
            update(unsignedBytes(server.publicExponent))
            // Skip the leading check byte; only the last two bytes are hashed.
            update(codeBytes, 1, codeBytes.size - 1)
        }.digest()

        if (digest[0] != codeBytes[0]) {
            throw PairingException("Pairing code $normalized failed its check byte — most likely a typo")
        }
        return digest
    }

    private fun parseHex(value: String): ByteArray {
        if (value.length % 2 != 0) throw PairingException("Pairing code must have an even number of characters")
        return ByteArray(value.length / 2) { index ->
            val high = Character.digit(value[index * 2], 16)
            val low = Character.digit(value[index * 2 + 1], 16)
            if (high < 0 || low < 0) {
                throw PairingException("Pairing code must be hexadecimal (0-9, A-F), got '$value'")
            }
            ((high shl 4) or low).toByte()
        }
    }

    private const val CODE_LENGTH = 6
}
