package com.ics2300.pocketbudget

import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.utils.CashFlowBucket
import com.ics2300.pocketbudget.utils.CashFlowClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class CashFlowClassifierTest {

    private fun transaction(type: String) = TransactionEntity(
        transactionId = "TX-$type",
        amount = 1_000.0,
        type = type,
        partyName = "DEDICATED SAVINGS TILL",
        timestamp = 0L,
        categoryId = 1
    )

    @Test
    fun receivedSavingsTillIsSavingsNotIncome() {
        val received = transaction("Received")

        assertEquals(
            CashFlowBucket.SAVINGS,
            CashFlowClassifier.bucket(received, "Savings")
        )
        assertEquals(1_000.0, CashFlowClassifier.savingsDelta(received, "Savings"), 0.0)
    }

    @Test
    fun withdrawalFromSavingsTillReducesSavingsButIsNotExpense() {
        val withdrawal = transaction("Withdraw")

        assertEquals(
            CashFlowBucket.SAVINGS,
            CashFlowClassifier.bucket(withdrawal, "Savings")
        )
        assertEquals(-1_000.0, CashFlowClassifier.savingsDelta(withdrawal, "Savings"), 0.0)
    }

    @Test
    fun ordinaryReceivedTransactionRemainsIncome() {
        val received = transaction("Received")

        assertEquals(
            CashFlowBucket.INCOME,
            CashFlowClassifier.bucket(received, "Income")
        )
    }

    @Test
    fun transferAndCashIsCountedAsExpense() {
        val sent = transaction("Sent")

        assertEquals(
            CashFlowBucket.EXPENSE,
            CashFlowClassifier.bucket(sent, "Transfer & Cash")
        )
    }

    @Test
    fun transferAndCashRemainsExpenseForIncomingCashRecords() {
        val received = transaction("Received")

        assertEquals(
            CashFlowBucket.EXPENSE,
            CashFlowClassifier.bucket(received, "Transfer & Cash")
        )
    }

    @Test
    fun reversalOfIncomeRemainsIncomeForNetFlowAccounting() {
        val reversal = transaction("Reversal")

        assertEquals(
            CashFlowBucket.INCOME,
            CashFlowClassifier.bucket(reversal, "Income")
        )
    }

    @Test
    fun fulizaLoanIsBorrowingNotIncomeOrExpense() {
        val loan = transaction("Fuliza Loan")

        assertEquals(
            CashFlowBucket.BORROWING,
            CashFlowClassifier.bucket(loan, "Debt & Credit")
        )
        assertEquals(
            CashFlowBucket.BORROWING,
            CashFlowClassifier.persistedBucket(loan, "Debt & Credit")
        )
    }
}
