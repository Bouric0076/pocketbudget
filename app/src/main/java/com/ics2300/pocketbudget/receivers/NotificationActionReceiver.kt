package com.ics2300.pocketbudget.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ics2300.pocketbudget.MainActivity
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.workers.SnoozeWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleAction(context, intent, action)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle notification action: $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAction(
        context: Context,
        intent: Intent,
        action: String
    ) {
        val notificationDbId = intent.getIntExtra(EXTRA_NOTIFICATION_DB_ID, -1)
        val androidNotificationId = intent.getIntExtra(EXTRA_ANDROID_NOTIFICATION_ID, -1)
        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)

        val app = context.applicationContext as? MainApplication

        when (action) {
            ACTION_MARK_AS_READ -> {
                if (notificationDbId != -1 && app != null) {
                    app.notificationRepository.markAsRead(notificationDbId)
                }
                dismissNotification(context, androidNotificationId)
            }

            ACTION_WRONG_CATEGORY,
            ACTION_CHANGE_CATEGORY -> {
                openTransactionEdit(context, transactionId)

                if (notificationDbId != -1 && app != null) {
                    app.notificationRepository.markAsRead(notificationDbId)
                }

                dismissNotification(context, androidNotificationId)
            }

            ACTION_OPEN_NOTIFICATION -> {
                openNotificationsScreen(context)

                if (notificationDbId != -1 && app != null) {
                    app.notificationRepository.markAsRead(notificationDbId)
                }

                dismissNotification(context, androidNotificationId)
            }

            ACTION_SNOOZE -> {
                val billName = intent.getStringExtra(EXTRA_BILL_NAME) ?: "Bill"
                val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)

                scheduleSnooze(context, billName, amount)
                dismissNotification(context, androidNotificationId)
            }

            ACTION_DISMISS -> {
                dismissNotification(context, androidNotificationId)
            }

            else -> {
                Log.w(TAG, "Unknown notification action received: $action")
            }
        }
    }

    private fun openTransactionEdit(
        context: Context,
        transactionId: String?
    ) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "transaction_edit")
            putExtra("transaction_id", transactionId)
        }

        context.startActivity(mainIntent)
    }

    private fun openNotificationsScreen(context: Context) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "notifications")
        }

        context.startActivity(mainIntent)
    }

    private fun scheduleSnooze(
        context: Context,
        billName: String,
        amount: Double
    ) {
        val snoozeRequest = OneTimeWorkRequestBuilder<SnoozeWorker>()
            .setInitialDelay(1, TimeUnit.HOURS)
            .setInputData(
                workDataOf(
                    EXTRA_BILL_NAME to billName,
                    EXTRA_AMOUNT to amount
                )
            )
            .addTag("snooze_bill_$billName")
            .build()

        WorkManager.getInstance(context).enqueue(snoozeRequest)
    }

    private fun dismissNotification(
        context: Context,
        androidNotificationId: Int
    ) {
        if (androidNotificationId == -1) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.cancel(androidNotificationId)
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"

        const val ACTION_MARK_AS_READ =
            "com.ics2300.pocketbudget.ACTION_MARK_AS_READ"

        const val ACTION_DISMISS =
            "com.ics2300.pocketbudget.ACTION_DISMISS"

        const val ACTION_WRONG_CATEGORY =
            "com.ics2300.pocketbudget.ACTION_WRONG_CATEGORY"

        const val ACTION_CHANGE_CATEGORY =
            "com.ics2300.pocketbudget.ACTION_CHANGE_CATEGORY"

        const val ACTION_OPEN_NOTIFICATION =
            "com.ics2300.pocketbudget.ACTION_OPEN_NOTIFICATION"

        const val ACTION_SNOOZE =
            "com.ics2300.pocketbudget.ACTION_SNOOZE"

        const val EXTRA_NOTIFICATION_DB_ID = "notification_db_id"
        const val EXTRA_ANDROID_NOTIFICATION_ID = "android_notification_id"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_BILL_NAME = "bill_name"
        const val EXTRA_AMOUNT = "amount"
    }
}