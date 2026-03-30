package com.ics2300.pocketbudget.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.ics2300.pocketbudget.MainActivity
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.receivers.NotificationActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {
    const val CHANNEL_TRANSACTIONS = "transaction_alerts"
    const val CHANNEL_DAILY_SUMMARY = "daily_summary"
    const val CHANNEL_BILL_REMINDERS = "bill_reminders"
    const val CHANNEL_BUDGET_ALERTS = "budget_alerts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val transChannel = NotificationChannel(
                CHANNEL_TRANSACTIONS,
                "Transaction Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications for new transactions" }
            
            val summaryChannel = NotificationChannel(
                CHANNEL_DAILY_SUMMARY,
                "Daily Summary",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Daily spending summary" }
            
            val billsChannel = NotificationChannel(
                CHANNEL_BILL_REMINDERS,
                "Bill Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Reminders for upcoming bills" }
            
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET_ALERTS,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts when exceeding budget limits" }
            
            manager.createNotificationChannels(listOf(transChannel, summaryChannel, billsChannel, budgetChannel))
        }
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        type: String,
        androidId: Int,
        iconRes: Int = R.drawable.ic_money,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        actionData: String? = null,
        extraActions: List<NotificationCompat.Action> = emptyList()
    ) {
        val app = context.applicationContext as? MainApplication ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Save to DB first to get the ID
            val dbId = app.notificationRepository.addNotification(title, message, type, actionData)
            
            // 2. Prepare Intent
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            // 3. Mark as Read Action
            val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_MARK_AS_READ
                putExtra("notification_db_id", dbId)
                putExtra("android_notification_id", androidId)
            }
            val markReadPendingIntent = PendingIntent.getBroadcast(
                context, dbId, markReadIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // 4. Build Notification
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(priority)
                .addAction(android.R.drawable.ic_menu_edit, "Mark Read", markReadPendingIntent)

            // Add extra actions (like Snooze)
            extraActions.forEach { builder.addAction(it) }

            // 5. Show
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(androidId, builder.build())
        }
    }

    fun showBillReminder(context: Context, billName: String, amount: Double, daysUntil: Int, isSnoozed: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        // Privacy Check
        val isPrivacyMode = SecurityUtils.isPrivacyModeEnabled(context)
        val amountText = CurrencyFormatter.formatKsh(amount, isPrivacyMode)
        val timeText = when (daysUntil) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> "in $daysUntil days"
        }
        val titleText = if (isSnoozed) "Snoozed: $billName" else "Upcoming Bill: $billName"
        val message = "$amountText is likely due $timeText."
        
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra("notification_id", billName.hashCode())
            putExtra("bill_name", billName)
            putExtra("amount", amount)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, billName.hashCode(), snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val snoozeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_popup_reminder, 
            "Snooze 1h", 
            snoozePendingIntent
        ).build()
        
        showNotification(
            context = context,
            channelId = CHANNEL_BILL_REMINDERS,
            title = titleText,
            message = message,
            type = "Bill",
            androidId = billName.hashCode(),
            iconRes = R.drawable.ic_popup_reminder,
            priority = NotificationCompat.PRIORITY_HIGH,
            actionData = billName,
            extraActions = listOf(snoozeAction)
        )
    }

    fun showTransactionNotification(context: Context, title: String, message: String, id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        showNotification(
            context = context,
            channelId = CHANNEL_TRANSACTIONS,
            title = title,
            message = message,
            type = "Transaction",
            androidId = id,
            iconRes = R.drawable.ic_money,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            actionData = id.toString()
        )
    }

    fun showBudgetAlert(context: Context, title: String, message: String, id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        showNotification(
            context = context,
            channelId = CHANNEL_BUDGET_ALERTS,
            title = title,
            message = message,
            type = "Budget",
            androidId = id,
            iconRes = R.drawable.ic_popup_reminder,
            priority = NotificationCompat.PRIORITY_HIGH,
            actionData = id.toString()
        )
    }

    fun showDailySummaryNotification(context: Context, amount: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        val isPrivacyMode = SecurityUtils.isPrivacyModeEnabled(context)
        val formattedAmount = CurrencyFormatter.formatKsh(amount, isPrivacyMode)
        
        val title = "Daily Spending Summary"
        val text = "You spent $formattedAmount today."
        
        showNotification(
            context = context,
            channelId = CHANNEL_DAILY_SUMMARY,
            title = title,
            message = text,
            type = "Summary",
            androidId = 1001,
            iconRes = R.drawable.ic_money,
            priority = NotificationCompat.PRIORITY_LOW
        )
    }
}
