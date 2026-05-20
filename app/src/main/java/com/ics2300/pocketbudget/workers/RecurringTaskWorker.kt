package com.ics2300.pocketbudget.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ics2300.pocketbudget.MainApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecurringTaskWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? MainApplication
                ?: return Result.failure()

            withContext(Dispatchers.IO) {
                app.repository.processDueRecurringTransactions()
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Recurring transaction worker failed.", e)

            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "RecurringTaskWorker"
        private const val MAX_RETRIES = 3
    }
}