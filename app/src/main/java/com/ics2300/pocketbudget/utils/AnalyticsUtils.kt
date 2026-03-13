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
    /* DEPRECATED: Caused false positives with old transactions. Use explicit recurring transactions instead.
    fun predictUpcomingBills(transactions: List<TransactionEntity>): List<BillPrediction> {
        // ... (removed implementation)
        return emptyList()
    }
    */
    
    data class BillPrediction(val name: String, val amount: Double, val dueDate: Long)
}
