package com.radiotv.tvremote.androidtv.protocol

/** TLS port that runs the Polo pairing handshake. */
const val PAIRING_PORT = 6467

/** TLS port that runs the live remote session. */
const val REMOTE_PORT = 6466

/** mDNS service type advertised by the Android TV Remote Service. */
const val MDNS_SERVICE_TYPE = "_androidtvremote2._tcp"

data class DeviceInfo(
    val vendor: String,
    val model: String,
    val appVersion: String,
)

data class VolumeInfo(
    val level: Int,
    val max: Int,
    val muted: Boolean,
)

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState

    /** TLS is up and the configure handshake finished; the TV accepts commands. */
    data object Ready : ConnectionState

    data class Failed(val cause: Throwable) : ConnectionState
}

/**
 * Capability bitmask exchanged in `remote_configure` / `remote_set_active`.
 *
 * The TV advertises what it supports; we intersect that with what we want and echo the
 * result back. Asking for a feature the TV did not offer gets the connection dropped.
 */
object Feature {
    const val PING = 1 shl 0
    const val KEY = 1 shl 1
    const val IME = 1 shl 2
    const val VOICE = 1 shl 3
    const val UNKNOWN_1 = 1 shl 4
    const val POWER = 1 shl 5
    const val VOLUME = 1 shl 6
    const val APP_LINK = 1 shl 9

    fun describe(mask: Int): String = buildList {
        if (mask and PING != 0) add("PING")
        if (mask and KEY != 0) add("KEY")
        if (mask and IME != 0) add("IME")
        if (mask and VOICE != 0) add("VOICE")
        if (mask and UNKNOWN_1 != 0) add("UNKNOWN_1")
        if (mask and POWER != 0) add("POWER")
        if (mask and VOLUME != 0) add("VOLUME")
        if (mask and APP_LINK != 0) add("APP_LINK")
    }.joinToString("|").ifEmpty { "none" }
}

/** Pairing failed in a way that retrying with the same code will not fix. */
class PairingException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The remote session ended or could not be established. */
class RemoteException(message: String, cause: Throwable? = null) : Exception(message, cause)
