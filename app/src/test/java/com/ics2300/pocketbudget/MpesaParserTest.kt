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
    }
}
