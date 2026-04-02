package com.ics2300.pocketbudget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ics2300.pocketbudget.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                val body = msg.messageBody
                val sender = msg.originatingAddress ?: ""
                
                // Filter for M-Pesa messages
                if (sender.contains("MPESA", ignoreCase = true) || 
                    sender.contains("M-PESA", ignoreCase = true)) {
                    
                    val application = context.applicationContext as? MainApplication
                    application?.let { app ->
                        app.applicationScope.launch {
                            try {
                                app.repository.processNewSms(body, msg.timestampMillis)
                            } catch (e: Exception) {
                                // In a real app, you'd want to log this to a remote service
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}
