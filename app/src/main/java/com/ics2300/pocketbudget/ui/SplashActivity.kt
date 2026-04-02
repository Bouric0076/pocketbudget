package com.ics2300.pocketbudget.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ics2300.pocketbudget.MainActivity
import com.ics2300.pocketbudget.R

import android.content.Context
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Check if onboarding is completed
        val prefs = getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        val isCompleted = prefs.getBoolean("is_onboarding_completed", false)

        val appName = findViewById<TextView>(R.id.text_app_name)
        val tagline = findViewById<TextView>(R.id.text_tagline)

        // Ensure views are ready for animation
        appName.alpha = 0f
        tagline.alpha = 0f
        
        appName.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(500)
            .start()
            
        tagline.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(1000)
            .start()

        // Increase total delay to ensure text is read
        Handler(Looper.getMainLooper()).postDelayed({
            val targetActivity = if (isCompleted) {
                MainActivity::class.java
            } else {
                OnboardingActivity::class.java
            }
            startActivity(Intent(this, targetActivity))
            
            // Use Android 14+ compatible transition or fallback
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    android.R.anim.fade_in, 
                    android.R.anim.fade_out
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            finish()
        }, 4000) // 4 seconds
    }
}
