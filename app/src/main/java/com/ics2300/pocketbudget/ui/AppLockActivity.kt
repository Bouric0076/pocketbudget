package com.ics2300.pocketbudget.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.utils.AppLockManager
import com.ics2300.pocketbudget.utils.SecurityUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executor

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
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )

            setContentView(R.layout.activity_app_lock)

            editPin = findViewById(R.id.edit_pin)
            btnUnlock = findViewById(R.id.btn_unlock)
            btnBiometric = findViewById(R.id.btn_biometric)

            securePinInput()
            setupBackPressHandling()

            btnUnlock.setOnClickListener {
                validatePin()
            }

            setupBiometric()
            configureBiometricButton()

        } catch (e: Exception) {
            e.printStackTrace()
            finishAffinity()
        }
    }

    private fun securePinInput() {
        editPin.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        editPin.importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO
        editPin.isSaveEnabled = false
        editPin.setTextIsSelectable(false)
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveTaskToBack(true)
                }
            }
        )
    }

    private fun configureBiometricButton() {
        if (SecurityUtils.isBiometricEnabled(this) && isBiometricAvailable()) {
            btnBiometric.visibility = android.view.View.VISIBLE
            btnBiometric.setOnClickListener {
                biometricPrompt.authenticate(promptInfo)
            }
        } else {
            btnBiometric.visibility = android.view.View.GONE
        }
    }

    private fun validatePin() {
        val inputPin = editPin.text.toString().trim()

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
        suppressOpenCloseTransition()
    }

    private fun suppressOpenCloseTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                0,
                0
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    unlockApp()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        applicationContext,
                        "Authentication failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("PocketBudget Locked")
            .setSubtitle("Unlock using your biometric credential")
            .setNegativeButtonText("Use PIN")
            .build()
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
}