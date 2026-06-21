package com.ics2300.pocketbudget.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ics2300.pocketbudget.MainApplication

class SmsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? MainApplication
                ?: return Result.failure()

            Log.d(TAG, "Starting periodic SMS sync background task...")
            val syncedCount = app.repository.syncTransactions()
            Log.d(TAG, "Periodic SMS sync complete. Synced $syncedCount new transactions.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed periodic SMS sync.", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "SmsSyncWorker"
        private const val MAX_RETRIES = 3
    }
}
