package com.ics2300.pocketbudget.utils

import android.content.Context
import android.content.SharedPreferences

object PremiumManager {
    private const val PREF_NAME = "premium_prefs"
    private const val KEY_IS_PREMIUM = "is_premium"
    private const val KEY_EXPIRY_DATE = "premium_expiry_date"
    
    // Feature flags
    const val FEATURE_PDF_EXPORT = "pdf_export"
    const val FEATURE_CSV_EXPORT = "csv_export"
    const val FEATURE_IMPORT_DATA = "import_data"
    const val FEATURE_ADVANCED_ANALYTICS = "advanced_analytics"
    const val FEATURE_UNLIMITED_BUDGETS = "unlimited_budgets"
    const val FEATURE_APP_LOCK = "app_lock" // Maybe make this basic?
    const val FEATURE_DATA_BACKUP = "data_backup"

    fun isPremium(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false)
        if (isPremium) {
            val expiry = prefs.getLong(KEY_EXPIRY_DATE, 0)
            if (expiry > 0 && System.currentTimeMillis() > expiry) {
                // Expired
                setPremiumStatus(context, false)
                return false
            }
        }
        return isPremium
    }

    fun setPremiumStatus(context: Context, isPremium: Boolean, durationDays: Int = 30) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_PREMIUM, isPremium)
        
        if (isPremium) {
            val expiry = System.currentTimeMillis() + (durationDays.toLong() * 24 * 60 * 60 * 1000)
            editor.putLong(KEY_EXPIRY_DATE, expiry)
        } else {
            editor.putLong(KEY_EXPIRY_DATE, 0)
        }
        editor.apply()
    }
    
    fun getExpiryDate(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_EXPIRY_DATE, 0)
    }

    fun canAccessFeature(context: Context, feature: String): Boolean {
        // Define which features are Premium-only
        return when (feature) {
            FEATURE_PDF_EXPORT -> isPremium(context)
            FEATURE_CSV_EXPORT -> isPremium(context) // Maybe CSV is basic? Let's say Premium for now
            FEATURE_IMPORT_DATA -> isPremium(context)
            FEATURE_ADVANCED_ANALYTICS -> isPremium(context)
            FEATURE_DATA_BACKUP -> isPremium(context)
            FEATURE_APP_LOCK -> true // Basic feature
            FEATURE_UNLIMITED_BUDGETS -> true // Basic feature for now
            else -> true
        }
    }
    
    fun getFeatureName(feature: String): String {
        return when (feature) {
            FEATURE_PDF_EXPORT -> "PDF Export"
            FEATURE_CSV_EXPORT -> "CSV Export"
            FEATURE_IMPORT_DATA -> "Import Data"
            FEATURE_ADVANCED_ANALYTICS -> "Advanced Analytics"
            FEATURE_DATA_BACKUP -> "Cloud Backup"
            FEATURE_UNLIMITED_BUDGETS -> "Unlimited Budgets"
            else -> "Premium Feature"
        }
    }
}
