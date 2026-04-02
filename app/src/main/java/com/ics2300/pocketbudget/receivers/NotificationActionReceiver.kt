package com.ics2300.pocketbudget.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ics2300.pocketbudget.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra("notification_db_id", -1)
        val androidNotificationId = intent.getIntExtra("android_notification_id", -1)
        val transactionId = intent.getStringExtra("transaction_id")
        
        val app = context.applicationContext as? MainApplication ?: return
        val repository = app.notificationRepository

        when (action) {
            ACTION_MARK_AS_READ -> {
                if (notificationId != -1) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.markAsRead(notificationId)
                    }
                }
                
                // Dismiss the notification from tray
                dismissNotification(context, androidNotificationId)
            }
            ACTION_WRONG_CATEGORY -> {
                // Open app to transaction detail/edit screen
                val mainIntent = Intent(context, com.ics2300.pocketbudget.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "transaction_edit")
                    putExtra("transaction_id", transactionId)
                }
                context.startActivity(mainIntent)
                
                // Mark notification as read and dismiss
                if (notificationId != -1) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.markAsRead(notificationId)
                    }
                }
                dismissNotification(context, androidNotificationId)
            }
            ACTION_SNOOZE -> {
                val billName = intent.getStringExtra("bill_name") ?: "Bill"
                val amount = intent.getDoubleExtra("amount", 0.0)
                
                // Use WorkManager to re-trigger in 1 hour
                val snoozeRequest = androidx.work.OneTimeWorkRequestBuilder<com.ics2300.pocketbudget.workers.SnoozeWorker>()
                    .setInitialDelay(1, java.util.concurrent.TimeUnit.HOURS)
                    .setInputData(androidx.work.workDataOf(
                        "bill_name" to billName,
                        "amount" to amount
                    ))
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueue(snoozeRequest)
                
                dismissNotification(context, androidNotificationId)
            }
            ACTION_DISMISS -> {
                dismissNotification(context, androidNotificationId)
            }
        }
    }

    private fun dismissNotification(context: Context, androidNotificationId: Int) {
        if (androidNotificationId != -1) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(androidNotificationId)
        }
    }

    companion object {
        const val ACTION_MARK_AS_READ = "com.ics2300.pocketbudget.ACTION_MARK_AS_READ"
        const val ACTION_DISMISS = "com.ics2300.pocketbudget.ACTION_DISMISS"
        const val ACTION_WRONG_CATEGORY = "com.ics2300.pocketbudget.ACTION_WRONG_CATEGORY"
        const val ACTION_SNOOZE = "com.ics2300.pocketbudget.ACTION_SNOOZE"
    }
}
