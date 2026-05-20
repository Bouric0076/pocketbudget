package com.ics2300.pocketbudget.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /**
     * High-level grouping:
     * Transaction, Budget, Bill, System, Insight
     */
    val type: String,

    /**
     * Fine-grained semantic subtype:
     * Income, Expense, Withdrawal, Reversal,
     * Fuliza, Savings, Airtime, Warning, etc.
     */
    val subtype: String = "General",

    /**
     * UI severity ranking.
     * LOW, NORMAL, HIGH, CRITICAL
     */
    val severity: String = "NORMAL",

    /**
     * Main notification title.
     */
    val title: String,

    /**
     * Primary visible message.
     */
    val message: String,

    /**
     * Rich expandable content.
     */
    val expandedMessage: String? = null,

    /**
     * Transaction amount if applicable.
     */
    val amount: Double? = null,

    /**
     * Currency code.
     */
    val currency: String = "KES",

    /**
     * Category or semantic grouping.
     * e.g Food, Transport, Salary
     */
    val categoryLabel: String? = null,

    /**
     * Optional related transaction id.
     */
    val transactionId: String? = null,

    /**
     * Optional actor/merchant/person.
     */
    val actorName: String? = null,

    /**
     * Timestamp created.
     */
    val timestamp: Long,

    /**
     * Whether user has read it.
     */
    val isRead: Boolean = false,

    /**
     * Whether card can expand.
     */
    val isExpandable: Boolean = false,

    /**
     * Optional original SMS body.
     */
    val originalMessage: String? = null,

    /**
     * Optional balance after transaction.
     */
    val balanceAfter: Double? = null,

    /**
     * Optional transaction fee/cost.
     */
    val transactionCost: Double? = null,

    /**
     * Optional action payload.
     */
    val actionData: String? = null
)