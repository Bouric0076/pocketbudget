package com.ics2300.pocketbudget.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["transactionId"], unique = true), Index(value = ["timestamp"]), Index(value = ["categoryId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionId: String, // Unique ID to avoid duplicates
    val amount: Double,
    val type: String, // Sent, Received, etc.
    val partyName: String,
    val timestamp: Long,
    val categoryId: Int?,
    val accountName: String? = null, // Extra metadata for Paybills (e.g. account forEasyTalk)
    val balanceAfter: Double? = null,
    val transactionCost: Double? = null,
    val fullSmsBody: String? = null
)
