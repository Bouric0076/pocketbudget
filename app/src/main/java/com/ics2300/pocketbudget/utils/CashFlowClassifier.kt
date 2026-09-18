package com.ics2300.pocketbudget.utils

import com.ics2300.pocketbudget.data.TransactionEntity

/**
 * Separates accounting meaning from the raw M-Pesa message type.
 * A savings movement is visible to the user but is not ordinary income or
 * spending; it is an internal movement between pockets of money. Transfer &
 * Cash is deliberately an expense category: money sent, withdrawn, or moved
 * out through that category is spending from the user's tracked wallet.
 */
enum class CashFlowBucket {
    INCOME,
    EXPENSE,
    SAVINGS,
    TRANSFER,
    BORROWING
}

object CashFlowClassifier {

    private const val SAVINGS = "Savings"
    private const val TRANSFER = "Transfer & Cash"

    private fun isTransferAndCashCategory(categoryName: String?): Boolean {
        return categoryName.equals(TRANSFER, ignoreCase = true) ||
            categoryName.equals("Transfer", ignoreCase = true)
    }

    fun bucket(transaction: TransactionEntity, categoryName: String?): CashFlowBucket {
        return when {
            transaction.type.equals("Fuliza Loan", ignoreCase = true) -> CashFlowBucket.BORROWING
            categoryName.equals(SAVINGS, ignoreCase = true) -> CashFlowBucket.SAVINGS
            isTransferAndCashCategory(categoryName) -> CashFlowBucket.EXPENSE
            transaction.type.equals("Received", ignoreCase = true) ||
                transaction.type.equals("Deposit", ignoreCase = true) -> CashFlowBucket.INCOME
            transaction.type.equals("Reversal", ignoreCase = true) &&
                categoryName.equals("Income", ignoreCase = true) -> CashFlowBucket.INCOME
            else -> CashFlowBucket.EXPENSE
        }
    }

    fun persistedBucket(transaction: TransactionEntity, categoryName: String?): CashFlowBucket {
        // Category semantics must win for Savings and Transfer & Cash. This
        // also repairs legacy rows whose persisted field predates these rules.
        when {
            transaction.type.equals("Fuliza Loan", ignoreCase = true) -> return CashFlowBucket.BORROWING
            categoryName.equals(SAVINGS, ignoreCase = true) -> return CashFlowBucket.SAVINGS
            isTransferAndCashCategory(categoryName) -> return CashFlowBucket.EXPENSE
            transaction.type.equals("Reversal", ignoreCase = true) &&
                categoryName.equals("Income", ignoreCase = true) -> return CashFlowBucket.INCOME
        }

        return runCatching { CashFlowBucket.valueOf(transaction.cashFlowBucket) }
            .getOrNull()
            ?.let { if (it == CashFlowBucket.TRANSFER) CashFlowBucket.EXPENSE else it }
            ?: bucket(transaction, categoryName)
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
