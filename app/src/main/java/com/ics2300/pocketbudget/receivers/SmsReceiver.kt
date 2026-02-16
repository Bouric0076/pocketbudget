package com.ics2300.pocketbudget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import com.ics2300.pocketbudget.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = (context.applicationContext as MainApplication).repository
                    
                    for (msg in messages) {
                        val sender = msg.originatingAddress
                        val body = msg.messageBody
                        
                        Log.d("SmsReceiver", "SMS Received from: $sender")

                        // M-Pesa sender varies: "MPESA", "M-PESA", or shortcodes
                        // Also check common M-Pesa shortcodes or names if the name varies
                        if (sender != null && (
                            sender.contains("MPESA", ignoreCase = true) || 
                            sender.contains("M-PESA", ignoreCase = true) ||
                            sender.equals("MPESA", ignoreCase = true)
                        )) {
                            Log.d("SmsReceiver", "Processing M-Pesa SMS: $body")
                            val transaction = repository.processNewSms(body)
                            
                            if (transaction != null) {
                                Log.d("SmsReceiver", "Transaction parsed: ${transaction.transactionId}")
                                NotificationHelper.showTransactionNotification(
                                    context,
                                    "New Transaction Added",
                                    "${transaction.type}: ${CurrencyFormatter.formatKsh(transaction.amount)} - ${transaction.partyName}",
                                    transaction.timestamp.toInt()
                                )
                            } else {
                                Log.e("SmsReceiver", "Failed to parse body: $body")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error processing SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
