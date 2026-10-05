package com.radiotv.tvremote.androidtv

import android.content.Context
import com.radiotv.tvremote.androidtv.protocol.ClientIdentity
import java.io.File
import java.security.KeyStore

object AndroidTvIdentity {
    private const val PASSWORD = "tv-pult-identity"
    val password: CharArray get() = PASSWORD.toCharArray()

    fun keyStore(context: Context): KeyStore =
        ClientIdentity.loadOrCreate(
            File(context.filesDir, "identity/client.p12"),
            password,
            commonName = "TV pult"
        )
}
