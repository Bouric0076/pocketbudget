package com.ics2300.pocketbudget.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.utils.SecurityUtils
import java.util.concurrent.Executor

import com.ics2300.pocketbudget.utils.AppLockManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppLockActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var editPin: EditText
    private lateinit var btnUnlock: Button
    private lateinit var btnBiometric: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_app_lock)

            editPin = findViewById(R.id.edit_pin)
            btnUnlock = findViewById(R.id.btn_unlock)
            btnBiometric = findViewById(R.id.btn_biometric)

            btnUnlock.setOnClickListener {
                validatePin()
            }

            setupBiometric()

            // Check if biometric is available and enabled
            if (SecurityUtils.isBiometricEnabled(this) && isBiometricAvailable()) {
                btnBiometric.visibility = android.view.View.VISIBLE
                btnBiometric.setOnClickListener {
                    biometricPrompt.authenticate(promptInfo)
                }
                // Do not auto-start, let user choose
            } else {
                btnBiometric.visibility = android.view.View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If something goes wrong during initialization (layout, etc.), close the app to prevent bypassing security
            finishAffinity()
        }
    }

    private fun validatePin() {
        val inputPin = editPin.text.toString()
        if (inputPin.length < 4) {
            editPin.error = "Enter 4 digits"
            return
        }

        if (SecurityUtils.checkPin(this, inputPin)) {
            unlockApp()
        } else {
            editPin.setText("")
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unlockApp() {
        AppLockManager.unlockSession()
        finish()
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If user cancels or too many attempts, just stay on PIN screen
                    // Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlockApp()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("PocketBudget Locked")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use PIN")
            .build()
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun onStop() {
        super.onStop()
        // We do not call finishAffinity() here anymore because it was causing the app to close 
        // when biometric prompt (or other system dialogs) paused the activity.
        // The lock screen will simply remain on top if the app is backgrounded.
    }

    // Prevent back button from bypassing lock
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing or minimize app
        moveTaskToBack(true)
    }
}
