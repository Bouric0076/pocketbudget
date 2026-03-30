package com.ics2300.pocketbudget.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ics2300.pocketbudget.utils.NotificationHelper

class SnoozeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val billName = inputData.getString("bill_name") ?: "Bill"
        val amount = inputData.getDouble("amount", 0.0)
        
        NotificationHelper.showBillReminder(applicationContext, billName, amount, 0, isSnoozed = true)
        
        return Result.success()
    }
}
