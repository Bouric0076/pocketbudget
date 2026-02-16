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

    // 6. M-SHWARI / DEPOSIT (Transferred from ...)
    // Example: UBE6U6I0FD Confirmed.Ksh350.00 transferred from M-Shwari account on 14/2/26 at 2:42 PM.
    private val MSHWARI_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s*Ksh([\\d,]+\\.\\d{2})\\s+transferred\\s+from\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)

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

        matcher = MSHWARI_PATTERN.matcher(normalizedBody)
        if (matcher.find()) return createTransaction(matcher, "Deposit")

        return null
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
