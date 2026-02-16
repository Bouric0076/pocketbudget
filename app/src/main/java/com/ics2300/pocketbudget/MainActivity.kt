package com.ics2300.pocketbudget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

import androidx.appcompat.app.AlertDialog
import com.ics2300.pocketbudget.utils.AutoStartHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        bottomNavigationView.setupWithNavController(navController)
        
        // Prompt for Auto-Start on supported devices (Tecno, Xiaomi, etc.)
        if (AutoStartHelper.isAutoStartPermissionAvailable(this) && !AutoStartHelper.hasPrompted(this)) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("To ensure automatic tracking works on your device, please enable 'Auto-start' for PocketBudget in the next screen.")
                .setPositiveButton("Enable Now") { _, _ ->
                    AutoStartHelper.requestAutoStartPermission(this)
                    AutoStartHelper.setPrompted(this)
                }
                .setNegativeButton("Later") { _, _ -> 
                    AutoStartHelper.setPrompted(this) 
                }
                .show()
        }
    }
}
