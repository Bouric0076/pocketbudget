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
}
