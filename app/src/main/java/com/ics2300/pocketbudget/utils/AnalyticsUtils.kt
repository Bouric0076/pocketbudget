package com.ics2300.pocketbudget.utils

import com.ics2300.pocketbudget.data.TransactionEntity
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

object AnalyticsUtils {

    data class ForecastResult(
        val predictedTotal: Double,
        val isOverBudget: Boolean,
        val daysUntilLimit: Int?, // Null if not predicted to exceed
        val velocityStatus: VelocityStatus
    )

    enum class VelocityStatus {
        SLOW, // Spending less than usual
        NORMAL, // On track
        FAST // Spending faster than usual
    }

    /**
     * Calculates the "Spending Velocity" for the current month.
     * Compares current daily average spend vs expected daily average based on budget or past data.
     */
    fun calculateVelocity(
        currentSpent: Double,
        totalBudget: Double,
        dayOfMonth: Int,
        totalDaysInMonth: Int
    ): VelocityStatus {
        if (totalBudget <= 0) return VelocityStatus.NORMAL

        // Ideal spending rate: spread budget evenly across the month
        val idealDailySpend = totalBudget / totalDaysInMonth
        val expectedSpendToDate = idealDailySpend * dayOfMonth
        
        // Allow a 10% buffer
        val threshold = expectedSpendToDate * 1.1
        val safeZone = expectedSpendToDate * 0.9

        return when {
            currentSpent > threshold -> VelocityStatus.FAST
            currentSpent < safeZone -> VelocityStatus.SLOW
            else -> VelocityStatus.NORMAL
        }
    }

    /**
     * Predicts end-of-month spending using Linear Regression (Simple projection for now)
     * Formula: (CurrentSpent / CurrentDay) * TotalDays
     * Can be enhanced with historical trends if available.
     */
    fun forecastEndOfMonth(
        currentSpent: Double,
        dayOfMonth: Int,
        totalDaysInMonth: Int,
        budgetLimit: Double
    ): ForecastResult {
        if (dayOfMonth == 0) return ForecastResult(0.0, false, null, VelocityStatus.NORMAL)

        // Simple linear projection
        val dailyRate = currentSpent / dayOfMonth
        val predictedTotal = dailyRate * totalDaysInMonth
        
        val isOverBudget = predictedTotal > budgetLimit && budgetLimit > 0
        
        var daysUntilLimit: Int? = null
        if (isOverBudget && dailyRate > 0) {
            val remainingBudget = budgetLimit - currentSpent
            if (remainingBudget > 0) {
                daysUntilLimit = (remainingBudget / dailyRate).roundToInt()
            } else {
                daysUntilLimit = 0 // Already exceeded
            }
        }
        
        val velocity = calculateVelocity(currentSpent, budgetLimit, dayOfMonth, totalDaysInMonth)

        return ForecastResult(predictedTotal, isOverBudget, daysUntilLimit, velocity)
    }

    /**
     * Identifies potential recurring bills due soon
     * Returns a list of predicted transactions (Name, Amount, Due Date)
     */
    fun predictUpcomingBills(transactions: List<TransactionEntity>): List<BillPrediction> {
        // Group by Party Name
        val groups = transactions
            .filter { it.amount > 0 && it.type != "Received" && it.type != "Deposit" } // Only expenses
            .groupBy { it.partyName }
        
        val predictions = mutableListOf<BillPrediction>()
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(Calendar.MONTH)
        
        // Look for patterns: Same amount, same day of month (+/- 2 days)
        for ((party, items) in groups) {
            if (items.size < 2) continue // Need at least 2 to form a pattern
            
            // Sort by date desc
            val sorted = items.sortedByDescending { it.timestamp }
            val lastTx = sorted.first()
            
            // Check if it happens monthly
            // Simple check: check day of month consistency
            val days = sorted.take(3).map { 
                val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                c.get(Calendar.DAY_OF_MONTH)
            }
            
            // Calculate variance in day of month
            val avgDay = days.average()
            val variance = days.map { kotlin.math.abs(it - avgDay) }.average()
            
            if (variance < 3.0) { // Consistent within 3 days
                // It's likely a monthly bill
                // Check if we already paid it this month
                val lastTxDate = Calendar.getInstance().apply { timeInMillis = lastTx.timestamp }
                val lastTxMonth = lastTxDate.get(Calendar.MONTH)
                
                if (lastTxMonth != currentMonth) {
                    // Not paid yet this month?
                    // Expected due date is roughly avgDay of this month
                    val dueDate = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, avgDay.roundToInt())
                        // If day passed, maybe it's next month? Or late?
                        // For reminder, we care if it's coming up soon (tomorrow/today)
                    }
                    
                    // Logic: If due date is within next 3 days
                    val diff = dueDate.timeInMillis - System.currentTimeMillis()
                    val daysUntil = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
                    
                    if (daysUntil in 0..3) {
                         predictions.add(BillPrediction(party, lastTx.amount, dueDate.timeInMillis))
                    }
                }
            }
        }
        return predictions
    }
    
    data class BillPrediction(val name: String, val amount: Double, val dueDate: Long)
}
