package com.ics2300.pocketbudget.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {

    private const val TAG = "SecurityUtils"

    private const val PREF_NAME = "secure_prefs"
    private const val STANDARD_PREF_NAME = "pocketbudget_prefs"

    private const val KEY_IS_BIOMETRIC_ENABLED = "is_biometric_enabled"
    private const val KEY_PIN_CODE = "app_pin_code" // legacy/plaintext key
    private const val KEY_PIN_HASH = "app_pin_hash"
    private const val KEY_PRIVACY_MODE = "privacy_mode_enabled"

    const val LOCK_TIMEOUT_MS = 30 * 1000L

    private fun getSecurePrefsOrNull(context: Context): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences unavailable. Falling back to hashed PIN storage only.", e)
            null
        }
    }

    private fun getStandardPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(STANDARD_PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun prefsForSecurityWrite(context: Context): SharedPreferences {
        return getSecurePrefsOrNull(context) ?: getStandardPrefs(context)
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))

        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        return "v2:$saltBase64:$hashBase64"
    }

    private fun verifyPin(inputPin: String, storedValue: String): Boolean {
        val parts = storedValue.split(":")
        if (parts.size != 3 || parts[0] != "v2") return false

        return try {
            val salt = Base64.decode(parts[1], Base64.NO_WRAP)
            val expected = storedValue
            val actual = hashPin(inputPin, salt)
            MessageDigest.isEqual(
                expected.toByteArray(Charsets.UTF_8),
                actual.toByteArray(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify PIN hash.", e)
            false
        }
    }

    private fun readPinHash(context: Context): String? {
        val securePrefs = getSecurePrefsOrNull(context)
        val standardPrefs = getStandardPrefs(context)

        return securePrefs?.getString(KEY_PIN_HASH, null)
            ?: standardPrefs.getString(KEY_PIN_HASH, null)
            ?: standardPrefs.getString("${KEY_PIN_CODE}_fallback", null)
    }

    fun isSecurityEnabled(context: Context): Boolean {
        return hasPin(context)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getStandardPrefs(context)
            .edit()
            .putBoolean(KEY_IS_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getStandardPrefs(context).getBoolean(KEY_IS_BIOMETRIC_ENABLED, false)
    }

    fun setPin(context: Context, pin: String) {
        require(pin.isNotBlank()) { "PIN cannot be blank." }

        val hashedPin = hashPin(pin, generateSalt())

        prefsForSecurityWrite(context)
            .edit()
            .putString(KEY_PIN_HASH, hashedPin)
            .remove(KEY_PIN_CODE)
            .remove("${KEY_PIN_CODE}_fallback")
            .apply()

        getStandardPrefs(context)
            .edit()
            .remove(KEY_PIN_CODE)
            .remove("${KEY_PIN_CODE}_fallback")
            .apply()
    }

    /**
     * PINs are no longer recoverable by design.
     * Use checkPin() instead.
     */
    fun getPin(context: Context): String? {
        return null
    }

    fun hasPin(context: Context): Boolean {
        val securePrefs = getSecurePrefsOrNull(context)
        val standardPrefs = getStandardPrefs(context)

        return securePrefs?.contains(KEY_PIN_HASH) == true ||
            standardPrefs.contains(KEY_PIN_HASH) ||
            standardPrefs.contains("${KEY_PIN_CODE}_fallback") ||
            securePrefs?.contains(KEY_PIN_CODE) == true
    }

    fun checkPin(context: Context, inputPin: String): Boolean {
        val storedHash = readPinHash(context)

        if (storedHash != null && storedHash.startsWith("v2:")) {
            return verifyPin(inputPin, storedHash)
        }

        // Legacy migration path: old app may have stored plaintext fallback.
        // If it matches once, immediately replace it with a secure hash.
        val standardPrefs = getStandardPrefs(context)
        val legacyPlaintext = standardPrefs.getString("${KEY_PIN_CODE}_fallback", null)

        if (legacyPlaintext != null && legacyPlaintext == inputPin) {
            setPin(context, inputPin)
            return true
        }

        val securePrefs = getSecurePrefsOrNull(context)
        val legacySecurePlaintext = securePrefs?.getString(KEY_PIN_CODE, null)

        if (legacySecurePlaintext != null && legacySecurePlaintext == inputPin) {
            setPin(context, inputPin)
            return true
        }

        return false
    }

    fun setPrivacyModeEnabled(context: Context, enabled: Boolean) {
        getStandardPrefs(context)
            .edit()
            .putBoolean(KEY_PRIVACY_MODE, enabled)
            .apply()
    }

    fun isPrivacyModeEnabled(context: Context): Boolean {
        return getStandardPrefs(context).getBoolean(KEY_PRIVACY_MODE, false)
    }

    fun clearSecurity(context: Context) {
        getStandardPrefs(context)
            .edit()
            .remove(KEY_IS_BIOMETRIC_ENABLED)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_CODE)
            .remove("${KEY_PIN_CODE}_fallback")
            .apply()

        getSecurePrefsOrNull(context)
            ?.edit()
            ?.remove(KEY_PIN_HASH)
            ?.remove(KEY_PIN_CODE)
            ?.apply()
    }
}