package com.ics2300.pocketbudget.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ics2300.pocketbudget.workers.SnoozeWorker
import java.util.concurrent.TimeUnit

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val billName = intent.getStringExtra("bill_name") ?: "Bill"
        val amount = intent.getDoubleExtra("amount", 0.0)
        
        // Dismiss current
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
        
        // Schedule Snooze via WorkManager
        val snoozeRequest = OneTimeWorkRequestBuilder<SnoozeWorker>()
            .setInitialDelay(1, TimeUnit.HOURS)
            .setInputData(workDataOf(
                "bill_name" to billName,
                "amount" to amount
            ))
            .build()
            
        WorkManager.getInstance(context).enqueue(snoozeRequest)
        
        android.widget.Toast.makeText(context, "Snoozed $billName for 1 hour", android.widget.Toast.LENGTH_SHORT).show()
    }
}
