package com.ics2300.pocketbudget.utils

import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.regex.Pattern

class MpesaParserTest {

    @Test
    fun testParseExample() {
        val body = "UBG6U6NN6A Confirmed. Ksh50.00 sent to SAFARICOM POSTPAID BUNDLES for account EasyTalk on 16/2/26 at 9:18 AM New M-PESA balance is Ksh370.44. Transaction cost, Ksh0.00.Amount you can transact within the day is 499,850.00. Save frequent paybills for quick payment on M-PESA app https://bit.ly/mpesalnk"
        
        // Use case insensitive flag or manual adjustment
        val PAYBILL_PATTERN = Pattern.compile("([A-Z0-9]+)\\s+Confirmed\\.\\s+Ksh([\\d,]+\\.\\d{2})\\s+sent\\s+to\\s+(.+?)\\s+for\\s+account\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)", Pattern.CASE_INSENSITIVE)
        
        val matcher = PAYBILL_PATTERN.matcher(body)
        if (matcher.find()) {
            println("Matched!")
            println("ID: ${matcher.group(1)}")
            println("Amount: ${matcher.group(2)}")
            println("Party: ${matcher.group(3)}")
            println("Account: ${matcher.group(4)}")
            println("Date: ${matcher.group(5)}")
            println("Time: ${matcher.group(6)}")
        } else {
            println("Not Matched")
        }
    @Test
    fun testReversalNewFormat() {
        val body = "UA26UGIL70 confirmed. Reversal of transaction UA26U2IPRD has been completed successfully on 2/1/26 at 11:11 AM and Ksh50.00 has been credited to your M-Pesa Account."
        val transaction = MpesaParser.parse(body)
        assertNotNull(transaction)
        assert(transaction?.amount == 50.0)
        assert(transaction?.type == "Reversal")
        assert(transaction?.partyName == "Reversal of UA26U2IPRD")
    }

    @Test
    fun testFulizaRepaymentNewFormat() {
        val body = "UCJOY9JZVM Confirmed. Ksh 1792.45 from your M-PESA has been used to fully pay your outstanding Fuliza M-PESA. Available Fuliza M-PESA limit is Ksh 1200.00. Your M-PESA balance is 8447.55."
        val transaction = MpesaParser.parse(body)
        assertNotNull(transaction)
        assert(transaction?.amount == 1792.45)
        assert(transaction?.type == "Fuliza Repayment")
    }

    @Test
    fun testFulizaDisbursementNewFormat() {
        val body = "UCJOY9KJBO Confirmed. Fuliza M-PESA amount is Ksh 1165.45. Access Fee charged Ksh 11.66. Total Fuliza M-PESA outstanding amount is Ksh1177.11 due on 18/04/26. To check daily charges, Dial *334#OK Select Query Charges"
        val transaction = MpesaParser.parse(body)
        assertNotNull(transaction)
        assert(transaction?.amount == 1165.45)
        assert(transaction?.type == "Fuliza Loan")
    }
}
