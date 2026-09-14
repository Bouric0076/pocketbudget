package com.ics2300.pocketbudget.utils

import com.ics2300.pocketbudget.data.TransactionEntity

/**
 * Separates accounting meaning from the raw M-Pesa message type.
 * A savings movement is visible to the user but is not ordinary income or
 * spending; it is an internal movement between pockets of money.
 */
enum class CashFlowBucket {
    INCOME,
    EXPENSE,
    SAVINGS,
    TRANSFER
}

object CashFlowClassifier {

    private const val SAVINGS = "Savings"
    private const val TRANSFER = "Transfer & Cash"

    fun bucket(transaction: TransactionEntity, categoryName: String?): CashFlowBucket {
        return when {
            categoryName.equals(SAVINGS, ignoreCase = true) -> CashFlowBucket.SAVINGS
            categoryName.equals(TRANSFER, ignoreCase = true) -> CashFlowBucket.TRANSFER
            transaction.type.equals("Received", ignoreCase = true) ||
                transaction.type.equals("Deposit", ignoreCase = true) -> CashFlowBucket.INCOME
            else -> CashFlowBucket.EXPENSE
        }
    }

    fun persistedBucket(transaction: TransactionEntity, categoryName: String?): CashFlowBucket {
        // Category semantics must win for the two non-income/non-expense pockets.
        // This also repairs legacy rows whose new persisted field defaults to EXPENSE.
        when {
            categoryName.equals(SAVINGS, ignoreCase = true) -> return CashFlowBucket.SAVINGS
            categoryName.equals(TRANSFER, ignoreCase = true) -> return CashFlowBucket.TRANSFER
        }

        return runCatching { CashFlowBucket.valueOf(transaction.cashFlowBucket) }
            .getOrElse { bucket(transaction, categoryName) }
    }

    fun savingsDelta(transaction: TransactionEntity, categoryName: String?): Double {
        if (persistedBucket(transaction, categoryName) != CashFlowBucket.SAVINGS) return 0.0

        return if (
            transaction.type.equals("Received", ignoreCase = true) ||
            transaction.type.equals("Deposit", ignoreCase = true)
        ) {
            transaction.amount
        } else {
            -transaction.amount
        }
    }
}
