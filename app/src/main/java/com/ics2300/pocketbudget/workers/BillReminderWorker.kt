package com.ics2300.pocketbudget.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.utils.AnalyticsUtils
import com.ics2300.pocketbudget.utils.NotificationHelper
import kotlinx.coroutines.flow.first

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

            for (bill in explicitBills) {
                val diff = bill.nextDueDate - System.currentTimeMillis()
                val daysUntil = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt().coerceAtLeast(0)
                
                NotificationHelper.showBillReminder(applicationContext, bill.description, bill.amount, daysUntil)
            }
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
