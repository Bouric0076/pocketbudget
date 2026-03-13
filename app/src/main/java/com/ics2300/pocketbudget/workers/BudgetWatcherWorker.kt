package com.ics2300.pocketbudget.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import com.ics2300.pocketbudget.utils.NotificationHelper
import com.ics2300.pocketbudget.utils.SecurityUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar

class BudgetWatcherWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val application = applicationContext as MainApplication
        val repository = application.repository
        val prefs = applicationContext.getSharedPreferences("budget_alerts", Context.MODE_PRIVATE)
        
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        
        try {
            // Get budget progress
            // Note: getBudgetProgress returns Flow, we take first emission
            val budgetProgressList = repository.getBudgetProgress(month, year).first()
            
            val isPrivacyMode = SecurityUtils.isPrivacyModeEnabled(applicationContext)

            for (item in budgetProgressList) {
                if (item.budgetAmount <= 0) continue // Skip if no budget set

                val percent = item.totalSpent / item.budgetAmount
                val keyBase = "${item.categoryId}_${month}_${year}"
                
                // Check 100% Threshold
                if (percent >= 1.0) {
                    val key100 = "alert_100_$keyBase"
                    if (!prefs.getBoolean(key100, false)) {
                        // Send Alert
                        val overAmount = item.totalSpent - item.budgetAmount
                        val overText = CurrencyFormatter.formatKsh(overAmount, isPrivacyMode)
                        
                        NotificationHelper.showBudgetAlert(
                            applicationContext,
                            "Budget Exceeded: ${item.categoryName}",
                            "You have exceeded your budget by $overText.",
                            item.categoryId + 10000 // Unique ID
                        )
                        
                        prefs.edit().putBoolean(key100, true).apply()
                    }
                } 
                // Check 80% Threshold
                else if (percent >= 0.8) {
                    val key80 = "alert_80_$keyBase"
                    // Only alert if we haven't already alerted for 80% OR 100% (though 100% logic handles itself)
                    // If we hit 100%, we don't need to re-alert 80% if we missed it, but usually we hit 80 first.
                    if (!prefs.getBoolean(key80, false)) {
                        val remaining = item.budgetAmount - item.totalSpent
                        val remainingText = CurrencyFormatter.formatKsh(remaining, isPrivacyMode)
                        
                        NotificationHelper.showBudgetAlert(
                            applicationContext,
                            "Approaching Budget Limit: ${item.categoryName}",
                            "You have used ${(percent * 100).toInt()}% of your budget. $remainingText remaining.",
                            item.categoryId + 20000
                        )
                        
                        prefs.edit().putBoolean(key80, true).apply()
                    }
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
