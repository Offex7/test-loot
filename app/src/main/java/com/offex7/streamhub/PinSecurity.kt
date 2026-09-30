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
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        return true
    }

    fun disable() {
        prefs.edit().putBoolean(KEY_ENABLED, false).putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
    }

    fun verify(pin: String): Boolean {
        if (!pin.matches(Regex("\\d{4}"))) return false
        val salt = prefs.getString(KEY_SALT, null)?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
            ?: return false
        val expected = prefs.getString(KEY_HASH, null) ?: return false
        return MessageDigest.isEqual(expected.toByteArray(), hash(pin, salt).toByteArray())
    }

    fun hint(): String = prefs.getString(KEY_HINT, "").orEmpty()

    fun failedAttempts(): Int {
        normalizeExpiredLockout()
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0).coerceIn(0, MAX_FAILED_ATTEMPTS)
    }

    fun lockoutUntil(): Long {
        normalizeExpiredLockout()
        return prefs.getLong(KEY_LOCKOUT_UNTIL, 0L).coerceAtLeast(0L)
    }

    fun lockoutRemainingMs(now: Long = System.currentTimeMillis()): Long =
        (lockoutUntil() - now).coerceAtLeast(0L)

    fun registerSuccess() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun registerFailure(
        maxAttempts: Int = MAX_FAILED_ATTEMPTS,
        lockoutMs: Long = LOCKOUT_DURATION_MS,
        now: Long = System.currentTimeMillis()
    ): Int {
        normalizeExpiredLockout(now)
        val current = prefs.getInt(KEY_FAILED_ATTEMPTS, 0).coerceIn(0, maxAttempts)
        val next = (current + 1).coerceAtMost(maxAttempts)
        if (next >= maxAttempts) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, next)
                .putLong(KEY_LOCKOUT_UNTIL, now + lockoutMs)
                .apply()
        } else {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, next).apply()
        }
        return next
    }

    private fun normalizeExpiredLockout(now: Long = System.currentTimeMillis()) {
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (until > 0L && until <= now) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
        }
    }

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
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L
    }
}
