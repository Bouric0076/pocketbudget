package com.ics2300.pocketbudget.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {

    private const val PREF_NAME = "secure_prefs"
    private const val KEY_IS_BIOMETRIC_ENABLED = "is_biometric_enabled"
    private const val KEY_PIN_CODE = "app_pin_code"
    private const val KEY_PRIVACY_MODE = "privacy_mode_enabled"
    
    // Time in milliseconds before app locks when in background (e.g. 1 minute)
    // For testing/UX, maybe immediate or 30s is better. Let's say 30 seconds.
    const val LOCK_TIMEOUT_MS = 30 * 1000L 

    private fun getSecurePrefs(context: Context): SharedPreferences {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to standard prefs if crypto fails (e.g. key store issue)
            // In a real app, we should handle this more gracefully, perhaps by resetting security
            e.printStackTrace()
            return getStandardPrefs(context)
        }
    }
    
    private fun getStandardPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("pocketbudget_prefs", Context.MODE_PRIVATE)
    }

    fun isSecurityEnabled(context: Context): Boolean {
        // Biometric is just one method. PIN is the fallback/primary method.
        // Security is "enabled" if a PIN is set.
        return hasPin(context)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getStandardPrefs(context).edit().putBoolean(KEY_IS_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getStandardPrefs(context).getBoolean(KEY_IS_BIOMETRIC_ENABLED, false)
    }

    fun setPin(context: Context, pin: String) {
        try {
            getSecurePrefs(context).edit().putString(KEY_PIN_CODE, pin).apply()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to standard prefs
            getStandardPrefs(context).edit().putString(KEY_PIN_CODE + "_fallback", pin).apply()
        }
    }

    fun getPin(context: Context): String? {
        try {
            val secure = getSecurePrefs(context).getString(KEY_PIN_CODE, null)
            if (secure != null) return secure
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getStandardPrefs(context).getString(KEY_PIN_CODE + "_fallback", null)
    }

    fun hasPin(context: Context): Boolean {
        try {
            if (getSecurePrefs(context).contains(KEY_PIN_CODE)) return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getStandardPrefs(context).contains(KEY_PIN_CODE + "_fallback")
    }

    fun checkPin(context: Context, inputPin: String): Boolean {
        val storedPin = getPin(context)
        return storedPin == inputPin
    }
    
    fun setPrivacyModeEnabled(context: Context, enabled: Boolean) {
        getStandardPrefs(context).edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
    }

    fun isPrivacyModeEnabled(context: Context): Boolean {
        return getStandardPrefs(context).getBoolean(KEY_PRIVACY_MODE, false)
    }

    fun clearSecurity(context: Context) {
        getStandardPrefs(context).edit().remove(KEY_IS_BIOMETRIC_ENABLED).apply()
        try {
            getSecurePrefs(context).edit().remove(KEY_PIN_CODE).apply()
        } catch (e: Exception) {
            getStandardPrefs(context).edit().remove(KEY_PIN_CODE + "_fallback").apply()
        }
    }
}
