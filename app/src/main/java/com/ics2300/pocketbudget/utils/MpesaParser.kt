package com.ics2300.pocketbudget.utils

import com.ics2300.pocketbudget.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

object MpesaParser {

    private const val DATE_FORMAT = "d/M/yy 'at' h:mm a"
    
    // Regex Patterns for M-Pesa Messages (Updated 2024)
    
    // 1. PAYBILL
    // Example: UBG6U6NN6A Confirmed. Ksh50.00 sent to SAFARICOM POSTPAID BUNDLES for account EasyTalk on 16/2/26 at 9:18 AM
    private val PAYBILL_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh([\\d,]+\\.\\d{2})\\s+sent\\s+to\\s+(.+?)\\s+for\\s+account\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    // 2. BUY GOODS
    // Example: PDS345 CONFIRMED. Ksh300.00 paid to QUICKMART. on 15/5/23 at 8:00 PM.
    private val BUY_GOODS_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh([\\d,]+\\.\\d{2})\\s+paid\\s+to\\s+(.+?)\\.?\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    // 3. WITHDRAW (Agent/ATM)
    // Example: SBI123 Confirmed. Ksh5,000.00 withdrawn from 123456 - AGENT NAME on...
    private val WITHDRAW_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh([\\d,]+\\.\\d{2})\\s+withdrawn\\s+from\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    // 4. SENT (Standard P2P)
    // Example: PDS345 CONFIRMED. Ksh1,500.00 sent to JOHN DOE 0712345678 on 15/5/23 at 5:30 PM.
    private val SENT_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh([\\d,]+\\.\\d{2})\\s+sent\\s+to\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    // 5. RECEIVED (Standard & Bank Deposits)
    // Example: PDS345 CONFIRMED. You have received Ksh500.00 from JANE DOE 0723456789 on 15/5/23 at 6:00 PM.
    private val RECEIVED_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*You\\s+have\\s+received\\s+Ksh([\\d,]+\\.\\d{2})\\s+from\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    // 6. M-SHWARI / KCB (Transferred from - Withdrawal from Savings)
    // Example: UBE6U6I0FD Confirmed.Ksh350.00 transferred from M-Shwari account on 14/2/26 at 2:42 PM.
    private val MSHWARI_WITHDRAW_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh([\\d,]+\\.\\d{2})\\s+transferred\\s+from\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    // 7. M-SHWARI / KCB (Transferred to - Deposit to Savings)
    // Example: UBE6U6I0FD Confirmed. Ksh500.00 transferred to M-Shwari account on 14/2/26 at 2:42 PM.
    private val MSHWARI_DEPOSIT_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh([\\d,]+\\.\\d{2})\\s+transferred\\s+to\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    // 8. AIRTIME PURCHASE
    // Example: QWE12345 Confirmed. Ksh100.00 bought for 0712345678 on 15/5/23 at 5:30 PM.
    private val AIRTIME_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh([\\d,]+\\.\\d{2})\\s+bought\\s+for\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    // 9. FULIZA REPAYMENT
    // Example: QWE12345 Confirmed. Ksh50.00 has been paid from your M-PESA account to your Fuliza M-PESA on 15/5/23 at 5:30 PM.
    private val FULIZA_REPAYMENT_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh([\\d,]+\\.\\d{2})\\s+has\\s+been\\s+paid\\s+from\\s+your\\s+M-PESA\\s+account\\s+to\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    // 10. FULIZA DISBURSEMENT (Loan)
    // Example: QWE12345 Confirmed. Ksh100.00 has been paid from your Fuliza M-PESA for PAYBILL on 15/5/23 at 5:30 PM.
    private val FULIZA_DISBURSEMENT_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh([\\d,]+\\.\\d{2})\\s+has\\s+been\\s+paid\\s+from\\s+your\\s+Fuliza\\s+M-PESA\\s+for\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    // 11. REVERSAL
    // Example: QWE12345 Confirmed. Reversal of transaction ABC12345 for Ksh1,000.00 has been successfully processed on 15/5/23 at 5:30 PM.
    // Groups: 1=ID, 2=OriginalTx, 3=Amount, 4=Date, 5=Time
    private val REVERSAL_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Reversal\\s+of\\s+transaction\\s+(.+?)\\s+for\\s+Ksh([\\d,]+\\.\\d{2})\\s+has\\s+been\\s+successfully\\s+processed\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

    fun parse(body: String): TransactionEntity? {
        // Normalize space and fix common OCR/SMS issues
        var normalizedBody = body.replace("\\s+".toRegex(), " ")
        normalizedBody = normalizedBody.replace("Confirmed\\.(\\S)".toRegex(RegexOption.IGNORE_CASE), "Confirmed. $1")

        // Try matching patterns in order of specificity
        
        var matcher = PAYBILL_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Paybill", hasAccount = true)

        matcher = BUY_GOODS_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Buy Goods")

        matcher = WITHDRAW_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Withdraw")

        matcher = SENT_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Sent")

        matcher = RECEIVED_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Received")

        matcher = MSHWARI_WITHDRAW_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Deposit") // Money IN from Savings

        matcher = MSHWARI_DEPOSIT_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Savings") // Money OUT to Savings

        matcher = AIRTIME_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Airtime")

        matcher = FULIZA_REPAYMENT_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Fuliza Repayment")

        matcher = FULIZA_DISBURSEMENT_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Fuliza Loan")

        matcher = REVERSAL_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createReversalTransaction(matcher)

        return null
    }

    private fun createReversalTransaction(matcher: java.util.regex.Matcher): TransactionEntity {
        val id = matcher.group(1) ?: ""
        val originalTx = matcher.group(2) ?: "Unknown"
        val amountStr = matcher.group(3)?.replace(",", "") ?: "0.0"
        val date = matcher.group(4) ?: ""
        val time = matcher.group(5) ?: ""

        return TransactionEntity(
            transactionId = id,
            amount = amountStr.toDouble(),
            type = "Reversal",
            partyName = "Reversal of $originalTx",
            timestamp = parseDate(date, time),
            categoryId = null
        )
    }

    private fun createTransaction(matcher: java.util.regex.Matcher, type: String, hasAccount: Boolean = false): TransactionEntity {
        val id = matcher.group(1) ?: ""
        val amountStr = matcher.group(2)?.replace(",", "") ?: "0.0"
        
        var party = matcher.group(3) ?: "Unknown"
        party = party.trim().removeSuffix(".")

        val date: String
        val time: String
        
        if (hasAccount) {
            // Groups: 1=ID, 2=Amount, 3=Party, 4=Account, 5=Date, 6=Time
            date = matcher.group(5) ?: ""
            time = matcher.group(6) ?: ""
        } else {
            // Groups: 1=ID, 2=Amount, 3=Party, 4=Date, 5=Time
            date = matcher.group(4) ?: ""
            time = matcher.group(5) ?: ""
        }

        return TransactionEntity(
            transactionId = id,
            amount = amountStr.toDouble(),
            type = type,
            partyName = party,
            timestamp = parseDate(date, time),
            categoryId = null
        )
    }

    private fun parseDate(date: String, time: String): Long {
        return try {
            // Flexible date format to handle d/M/yy and dd/MM/yy
            val format = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
            val dateObj = format.parse("$date at $time")
            dateObj?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
            System.currentTimeMillis()
        }
    }
}
