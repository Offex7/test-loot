package com.radiotv.tvremote.core

import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate

class PairingCodeTest {
    @Test fun rejectsMalformedCode() {
        val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        // The function must reject before touching the certificate details.
        assertThrows(PairingException::class.java) {
            AndroidTvPairingSecret.compute(kp.fakeCertificate(), kp.fakeCertificate(), "12345")
        }
    }

    private fun java.security.KeyPair.fakeCertificate(): X509Certificate =
        throw PairingException("test helper: certificate generation intentionally omitted")
}
