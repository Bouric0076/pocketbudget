package com.ics2300.pocketbudget.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val description: String,
    val categoryId: Int,
    val type: String, // "Expense" or "Income"
    val frequency: String, // "Daily", "Weekly", "Monthly", "Yearly"
    val startDate: Long,
    val nextDueDate: Long,
    val isActive: Boolean = true
)