package com.ics2300.pocketbudget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.ics2300.pocketbudget.MainApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private val smsProcessingMutex = Mutex()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val mpesaMessages = messages.filter { msg ->
                val sender = msg.originatingAddress.orEmpty()
                sender.contains("MPESA", ignoreCase = true) ||
                    sender.contains("M-PESA", ignoreCase = true)
            }

            if (mpesaMessages.isEmpty()) {
                pendingResult.finish()
                return
            }

            val application = context.applicationContext as? MainApplication
            if (application == null) {
                Log.e(TAG, "Application context is not MainApplication.")
                pendingResult.finish()
                return
            }

            application.applicationScope.launch {
                try {
                    smsProcessingMutex.withLock {
                        for (msg in mpesaMessages) {
                            val body = msg.messageBody.orEmpty()
                            if (body.isBlank()) continue

                            application.repository.processNewSms(
                                body = body,
                                timestamp = msg.timestampMillis
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process incoming M-Pesa SMS.", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to receive SMS broadcast.", e)
            pendingResult.finish()
        }
    }
}