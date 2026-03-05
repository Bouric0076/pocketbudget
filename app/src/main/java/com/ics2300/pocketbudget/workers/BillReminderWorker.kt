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
            // Get last 6 months of transactions for better pattern matching
            // For now, just getting all is fine or use date range query
            val transactions = repository.allTransactions.first()
            
            val predictions = AnalyticsUtils.predictUpcomingBills(transactions)
            
            for (bill in predictions) {
                // Determine days until
                val diff = bill.dueDate - System.currentTimeMillis()
                val daysUntil = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt()
                
                // Show notification
                NotificationHelper.showBillReminder(applicationContext, bill.name, bill.amount, daysUntil)
            }
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
