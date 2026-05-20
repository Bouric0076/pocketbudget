package com.ics2300.pocketbudget.utils

import android.util.Log
import com.ics2300.pocketbudget.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

object MpesaParser {

    private const val TAG = "MpesaParser"
    private const val DATE_FORMAT = "d/M/yy 'at' h:mm a"

    private data class ParserRule(
        val pattern: Pattern,
        val type: String,
        val hasAccount: Boolean = false,
        val creator: (Matcher, Long) -> TransactionEntity?
    )

    private val PAYBILL_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\s*([\\d,]+\\.\\d{2})\\s+sent\\s+to\\s+(.+?)\\s+for\\s+account\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val BUY_GOODS_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\s*([\\d,]+\\.\\d{2})\\s+paid\\s+to\\s+(.+?)\\.?\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val WITHDRAW_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\s*([\\d,]+\\.\\d{2})\\s+withdrawn\\s+from\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val SENT_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\s*([\\d,]+\\.\\d{2})\\s+sent\\s+to\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val RECEIVED_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*You\\s+have\\s+received\\s+Ksh\\s*([\\d,]+\\.\\d{2})\\s+from\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val MSHWARI_WITHDRAW_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\s*([\\d,]+\\.\\d{2})\\s+transferred\\s+from\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val MSHWARI_DEPOSIT_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\s*([\\d,]+\\.\\d{2})\\s+transferred\\s+to\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val AIRTIME_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\s*([\\d,]+\\.\\d{2})\\s+bought\\s+for\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val FULIZA_REPAYMENT_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\s*([\\d,]+\\.\\d{2})\\s+has\\s+been\\s+paid\\s+from\\s+your\\s+M-PESA\\s+account\\s+to\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val FULIZA_DISBURSEMENT_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\s*([\\d,]+\\.\\d{2})\\s+has\\s+been\\s+paid\\s+from\\s+your\\s+Fuliza\\s+M-PESA\\s+for\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val REVERSAL_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Reversal\\s+of\\s+transaction\\s+(.+?)\\s+for\\s+Ksh\\s*([\\d,]+\\.\\d{2})\\s+has\\s+been\\s+successfully\\s+processed\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)",
        Pattern.CASE_INSENSITIVE
    )

    private val REVERSAL_NEW_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+confirmed\\.\\s*Reversal\\s+of\\s+transaction\\s+([A-Z0-9]+)\\s+has\\s+been\\s+completed\\s+successfully\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)\\s+and\\s+Ksh\\s*([\\d,]+\\.\\d{2})\\s+has\\s+been\\s+credited",
        Pattern.CASE_INSENSITIVE
    )

    private val FULIZA_REPAYMENT_NEW_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh\\s*([\\d,]+\\.\\d{2})\\s+from\\s+your\\s+M-PESA\\s+has\\s+been\\s+used\\s+to\\s+fully\\s+pay\\s+your\\s+outstanding\\s+Fuliza\\s+M-PESA",
        Pattern.CASE_INSENSITIVE
    )

    private val FULIZA_DISBURSEMENT_NEW_PATTERN = Pattern.compile(
        "([A-Z0-9]+)\\s+Confirmed\\.\\s*Fuliza\\s+M-PESA\\s+amount\\s+is\\s+Ksh\\s*([\\d,]+\\.\\d{2})",
        Pattern.CASE_INSENSITIVE
    )

    private val BALANCE_PATTERN = Pattern.compile(
        "balance\\s+is\\s+Ksh\\s*([\\d,]+\\.\\d{2})",
        Pattern.CASE_INSENSITIVE
    )

    private val COST_PATTERN = Pattern.compile(
        "cost,\\s*Ksh\\s*([\\d,]+\\.\\d{2})",
        Pattern.CASE_INSENSITIVE
    )

    private val rules = listOf(
        ParserRule(PAYBILL_PATTERN, "Paybill", hasAccount = true) { matcher, _ ->
            createTransaction(matcher, "Paybill", hasAccount = true)
        },
        ParserRule(BUY_GOODS_PATTERN, "Buy Goods") { matcher, _ ->
            createTransaction(matcher, "Buy Goods")
        },
        ParserRule(WITHDRAW_PATTERN, "Withdraw") { matcher, _ ->
            createTransaction(matcher, "Withdraw")
        },
        ParserRule(SENT_PATTERN, "Sent") { matcher, _ ->
            createTransaction(matcher, "Sent")
        },
        ParserRule(RECEIVED_PATTERN, "Received") { matcher, _ ->
            createTransaction(matcher, "Received")
        },
        ParserRule(MSHWARI_WITHDRAW_PATTERN, "Deposit") { matcher, _ ->
            createTransaction(matcher, "Deposit")
        },
        ParserRule(MSHWARI_DEPOSIT_PATTERN, "Savings") { matcher, _ ->
            createTransaction(matcher, "Savings")
        },
        ParserRule(AIRTIME_PATTERN, "Airtime") { matcher, _ ->
            createTransaction(matcher, "Airtime")
        },
        ParserRule(FULIZA_REPAYMENT_PATTERN, "Fuliza Repayment") { matcher, _ ->
            createTransaction(matcher, "Fuliza Repayment")
        },
        ParserRule(FULIZA_REPAYMENT_NEW_PATTERN, "Fuliza Repayment") { matcher, smsTimestamp ->
            createFulizaTransaction(matcher, "Fuliza Repayment", smsTimestamp)
        },
        ParserRule(FULIZA_DISBURSEMENT_PATTERN, "Fuliza Loan") { matcher, _ ->
            createTransaction(matcher, "Fuliza Loan")
        },
        ParserRule(FULIZA_DISBURSEMENT_NEW_PATTERN, "Fuliza Loan") { matcher, smsTimestamp ->
            createFulizaTransaction(matcher, "Fuliza Loan", smsTimestamp)
        },
        ParserRule(REVERSAL_PATTERN, "Reversal") { matcher, _ ->
            createReversalTransaction(matcher)
        },
        ParserRule(REVERSAL_NEW_PATTERN, "Reversal") { matcher, _ ->
            createReversalNewTransaction(matcher)
        }
    )

    fun parse(
        body: String,
        smsTimestamp: Long = System.currentTimeMillis()
    ): TransactionEntity? {
        val normalizedBody = normalizeBody(body)

        for (rule in rules) {
            val matcher = rule.pattern.matcher(normalizedBody)

            if (matcher.find()) {
                val transaction = try {
                    rule.creator(matcher, smsTimestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse ${rule.type} transaction.", e)
                    null
                }

                if (transaction != null) {
                    return attachMetadata(transaction, body, normalizedBody)
                }
            }
        }

        Log.w(TAG, "Unsupported M-Pesa message format.")
        return null
    }

    private fun normalizeBody(body: String): String {
        return body
            .replace("\\s+".toRegex(), " ")
            .replace(
                "Confirmed\\.(\\S)".toRegex(RegexOption.IGNORE_CASE),
                "Confirmed. $1"
            )
            .trim()
    }

    private fun attachMetadata(
        transaction: TransactionEntity,
        originalBody: String,
        normalizedBody: String
    ): TransactionEntity {
        val balanceAfter = extractMoney(BALANCE_PATTERN, normalizedBody)
        val transactionCost = extractMoney(COST_PATTERN, normalizedBody)

        return transaction.copy(
            balanceAfter = balanceAfter,
            transactionCost = transactionCost,
            fullSmsBody = originalBody
        )
    }

    private fun extractMoney(pattern: Pattern, body: String): Double? {
        val matcher = pattern.matcher(body)

        return if (matcher.find()) {
            matcher.group(1)
                ?.replace(",", "")
                ?.toDoubleOrNull()
        } else {
            null
        }
    }

    private fun createFulizaTransaction(
        matcher: Matcher,
        type: String,
        smsTimestamp: Long
    ): TransactionEntity? {
        val id = matcher.groupOrNull(1)
        val amount = matcher.moneyOrNull(2)

        if (id.isNullOrBlank() || amount == null) return null

        return TransactionEntity(
            transactionId = id,
            amount = amount,
            type = type,
            partyName = "Fuliza M-PESA",
            timestamp = smsTimestamp,
            categoryId = null
        )
    }

    private fun createReversalNewTransaction(matcher: Matcher): TransactionEntity? {
        val id = matcher.groupOrNull(1)
        val originalTx = matcher.groupOrNull(2) ?: "Unknown"
        val date = matcher.groupOrNull(3)
        val time = matcher.groupOrNull(4)
        val amount = matcher.moneyOrNull(5)

        if (id.isNullOrBlank() || date.isNullOrBlank() || time.isNullOrBlank() || amount == null) {
            return null
        }

        return TransactionEntity(
            transactionId = id,
            amount = amount,
            type = "Reversal",
            partyName = "Reversal of $originalTx",
            timestamp = parseDate(date, time),
            categoryId = null
        )
    }

    private fun createReversalTransaction(matcher: Matcher): TransactionEntity? {
        val id = matcher.groupOrNull(1)
        val originalTx = matcher.groupOrNull(2) ?: "Unknown"
        val amount = matcher.moneyOrNull(3)
        val date = matcher.groupOrNull(4)
        val time = matcher.groupOrNull(5)

        if (id.isNullOrBlank() || amount == null || date.isNullOrBlank() || time.isNullOrBlank()) {
            return null
        }

        return TransactionEntity(
            transactionId = id,
            amount = amount,
            type = "Reversal",
            partyName = "Reversal of $originalTx",
            timestamp = parseDate(date, time),
            categoryId = null
        )
    }

    private fun createTransaction(
        matcher: Matcher,
        type: String,
        hasAccount: Boolean = false
    ): TransactionEntity? {
        val id = matcher.groupOrNull(1)
        val amount = matcher.moneyOrNull(2)
        val party = matcher.groupOrNull(3)
            ?.trim()
            ?.removeSuffix(".")
            ?: "Unknown"

        val date: String?
        val time: String?
        val accountName: String?

        if (hasAccount) {
            accountName = matcher.groupOrNull(4)?.trim()
            date = matcher.groupOrNull(5)
            time = matcher.groupOrNull(6)
        } else {
            accountName = null
            date = matcher.groupOrNull(4)
            time = matcher.groupOrNull(5)
        }

        if (id.isNullOrBlank() || amount == null || date.isNullOrBlank() || time.isNullOrBlank()) {
            return null
        }

        return TransactionEntity(
            transactionId = id,
            amount = amount,
            type = type,
            partyName = party,
            accountName = accountName,
            timestamp = parseDate(date, time),
            categoryId = null
        )
    }

    private fun parseDate(date: String, time: String): Long {
        return try {
            val format = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH).apply {
                isLenient = false
            }

            format.parse("$date at $time")?.time ?: System.currentTimeMillis()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse M-Pesa transaction date: $date $time", e)
            System.currentTimeMillis()
        }
    }

    private fun Matcher.groupOrNull(index: Int): String? {
        return try {
            group(index)
        } catch (e: Exception) {
            null
        }
    }

    private fun Matcher.moneyOrNull(index: Int): Double? {
        return groupOrNull(index)
            ?.replace(",", "")
            ?.trim()
            ?.toDoubleOrNull()
    }
}