package com.pocketbudget.parser

/**
 * Data class representing a parsed M-Pesa transaction.
 * This object is produced by the SmsParser and passed to the Database Module.
 */
data class Transaction(
    val transactionId: String,       // Unique M-Pesa transaction code e.g. "QJK5XY12AB"
    val type: TransactionType,       // SENT, RECEIVED, PAYBILL, BUY_GOODS
    val amount: Double,              // Transaction amount in KES
    val partyName: String,           // Recipient / Sender / Business name
    val partyNumber: String,         // Phone number or Paybill number (if available)
    val dateTime: String,            // Formatted date/time string: "15/6/2025 10:30 AM"
    val balance: Double?,            // Account balance after transaction (nullable)
    val rawSms: String               // Original SMS for reference/debugging
)

/**
 * Enum representing the four main M-Pesa transaction types.
 */
enum class TransactionType {
    SENT,
    RECEIVED,
    PAYBILL,
    BUY_GOODS,
    UNKNOWN
}
