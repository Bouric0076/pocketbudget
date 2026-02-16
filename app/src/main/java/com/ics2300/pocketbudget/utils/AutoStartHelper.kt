package com.ics2300.pocketbudget.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

object AutoStartHelper {

    private const val PREF_NAME = "auto_start_prefs"
    private const val KEY_PROMPTED = "has_prompted_auto_start"

    // Check if device is one of the manufacturers known for aggressive background killing
    fun isAutoStartPermissionAvailable(context: Context): Boolean {
        val manufacturers = listOf(
            "tecno", "infinix", "itel", // Transsion Holdings
            "xiaomi", "redmi",
            "oppo", "vivo", "letv",
            "honor", "huawei", "asus", "oneplus"
        )
        return manufacturers.any { Build.MANUFACTURER.contains(it, ignoreCase = true) }
    }

    fun hasPrompted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PROMPTED, false)
    }

    fun setPrompted(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PROMPTED, true).apply()
    }

    fun requestAutoStartPermission(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = Intent()
        
        try {
            when {
                // Tecno, Infinix, Itel (HiOS / XOS)
                manufacturer.contains("tecno") || manufacturer.contains("infinix") || manufacturer.contains("itel") -> {
                    val transsionIntents = listOf(
                        ComponentName("com.transsion.phonemaster", "com.transsion.phonemaster.AutoStartManagementActivity"),
                        ComponentName("com.transsion.mobilemanager", "com.transsion.mobilemanager.autostart.AutoStartManagementActivity"),
                        ComponentName("com.transsion.mobilemanager", "com.transsion.mobilemanager.appmanagement.AutoStartActivity"),
                        ComponentName("com.transsion.phonemanager", "com.transsion.phonemanager.AutoStartManagementActivity")
                    )
                    
                    if (!tryIntents(context, transsionIntents)) {
                        // Fallback to app details if specific intents fail
                        openAppDetails(context)
                    }
                    return
                }
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                    intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
                manufacturer.contains("oppo") -> {
                    intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                }
                manufacturer.contains("vivo") -> {
                    intent.component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                }
                // Fallback for others
                else -> {
                    openAppDetails(context)
                    return
                }
            }
            
            if (context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                context.startActivity(intent)
            } else {
                openAppDetails(context)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            openAppDetails(context)
        }
    }

    private fun tryIntents(context: Context, components: List<ComponentName>): Boolean {
        for (component in components) {
            try {
                val intent = Intent().setComponent(component)
                if (context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                // Continue to next intent
            }
        }
        return false
    }

    private fun openAppDetails(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:" + context.packageName)
            context.startActivity(intent)
            Toast.makeText(context, "Please enable 'Auto-start' manually in App Info settings", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }
}