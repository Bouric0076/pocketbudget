package com.ics2300.pocketbudget.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // Sent, Received, etc.
    val partyName: String,
    val timestamp: Long,
    val categoryId: Int?
)
