package com.ics2300.pocketbudget.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import com.ics2300.pocketbudget.utils.NotificationHelper
import com.ics2300.pocketbudget.utils.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.util.Calendar

class BudgetWatcherWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? MainApplication
                ?: return Result.failure()

            val repository = app.repository
            val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)

            val budgetProgressList = withTimeout(FLOW_TIMEOUT_MS) {
                repository.getBudgetProgress(month, year).first()
            }

            val isPrivacyMode = SecurityUtils.isPrivacyModeEnabled(applicationContext)

            for (item in budgetProgressList) {
                if (item.budgetAmount <= 0) continue

                val percent = item.totalSpent / item.budgetAmount
                val keyBase = "${item.categoryId}_${month}_${year}"

                if (percent >= 1.0) {
                    val key100 = "alert_100_$keyBase"

                    if (!prefs.getBoolean(key100, false)) {
                        val overAmount = item.totalSpent - item.budgetAmount
                        val overText = CurrencyFormatter.formatKsh(overAmount, isPrivacyMode)

                        NotificationHelper.showBudgetAlert(
                            applicationContext,
                            "Budget Exceeded: ${item.categoryName}",
                            "You have exceeded your budget by $overText.",
                            item.categoryId + 10000
                        )

                        prefs.edit().putBoolean(key100, true).commit()
                    }

                } else if (percent >= 0.8) {
                    val key80 = "alert_80_$keyBase"

                    if (!prefs.getBoolean(key80, false)) {
                        val remaining = item.budgetAmount - item.totalSpent
                        val remainingText = CurrencyFormatter.formatKsh(remaining, isPrivacyMode)

                        NotificationHelper.showBudgetAlert(
                            applicationContext,
                            "Approaching Budget Limit: ${item.categoryName}",
                            "You have used ${(percent * 100).toInt()}% of your budget. $remainingText remaining.",
                            item.categoryId + 20000
                        )

                        prefs.edit().putBoolean(key80, true).commit()
                    }
                }
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Budget watcher worker failed.", e)

            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "BudgetWatcherWorker"
        private const val PREF_NAME = "budget_alerts"
        private const val FLOW_TIMEOUT_MS = 10_000L
        private const val MAX_RETRIES = 3
    }
}