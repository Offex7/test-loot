package com.offex7.streamhub

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

class PinSecurityStore(context: Context) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "radio_pin_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun enable(pin: String, hint: String): Boolean {
        if (!pin.matches(Regex("\\d{4}"))) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, hash(pin, salt))
            .putString(KEY_HINT, hint.take(15))
            .apply()
        return true
    }

    fun disable() {
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
    }

    fun verify(pin: String): Boolean {
        if (!pin.matches(Regex("\\d{4}"))) return false
        val salt = prefs.getString(KEY_SALT, null)?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
            ?: return false
        val expected = prefs.getString(KEY_HASH, null) ?: return false
        return MessageDigest.isEqual(expected.toByteArray(), hash(pin, salt).toByteArray())
    }

    fun hint(): String = prefs.getString(KEY_HINT, "").orEmpty()

    private fun hash(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val value = digest.digest(salt + pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(value, Base64.NO_WRAP)
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val KEY_HINT = "hint"
    }
}
