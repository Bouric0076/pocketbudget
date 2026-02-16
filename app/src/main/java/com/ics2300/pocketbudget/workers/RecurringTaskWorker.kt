package com.ics2300.pocketbudget.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log
import com.ics2300.pocketbudget.MainApplication

class RecurringTaskWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("WorkManager", "Checking recurring transactions...")
            
            val repository = (applicationContext as MainApplication).repository
            repository.processDueRecurringTransactions()
            
            Result.success()
        } catch (e: Exception) {
            Log.e("WorkManager", "Error processing recurring transactions", e)
            Result.failure()
        }
    }
}