package com.ics2300.pocketbudget.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.utils.AnalyticsUtils
import com.ics2300.pocketbudget.utils.NotificationHelper
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as MainApplication).repository

        try {
            // Get explicit recurring bills due soon (next 3 days)
            val calendar = java.util.Calendar.getInstance()
            val start = calendar.timeInMillis
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 3)
            val end = calendar.timeInMillis
            
            // We only notify for explicitly set recurring transactions
            // This avoids false positives from heuristic predictions
            val explicitBills = repository.getUpcomingRecurringTransactions(start, end)
            val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val dueDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            for (bill in explicitBills) {
                val diff = bill.nextDueDate - System.currentTimeMillis()
                val daysUntil = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt().coerceAtLeast(0)
                val dueDateKey = dueDateFormat.format(Date(bill.nextDueDate))
                val notificationKey = "bill_${bill.id}_$dueDateKey"

                if (prefs.getBoolean(notificationKey, false)) {
                    continue
                }

                val posted = NotificationHelper.showBillReminder(
                    applicationContext,
                    bill.description,
                    bill.amount,
                    daysUntil
                )

                if (posted) {
                    prefs.edit().putBoolean(notificationKey, true).apply()
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}

private const val PREF_NAME = "bill_reminder_alerts"
