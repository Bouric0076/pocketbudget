package com.ics2300.pocketbudget.utils

import com.ics2300.pocketbudget.data.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MpesaParserRealDataTest {

    private val samples = listOf(
        // 1. Received from Person
        "UBGG96SKAZ Confirmed.You have received Ksh330.00 from OKWARO  THOMAS 0704832328 on 16/2/26 at 5:56 AM  New M-PESA balance is Ksh370.44. Earn interest daily on Ziidi MMF,Dial *334#",
        // 2. Buy Goods
        "UBF6U6LHVI Confirmed. Ksh65.00 paid to BERACAH SUPERMARKET. on 15/2/26 at 3:20 PM.New M-PESA balance is Ksh36.44. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,661.00. Save frequent Tills for quick payment on M-PESA app `https://bit.ly/mpesalnk`",
        // 3. Received from Bank
        "UBF6U6LHUV Confirmed. You have received Ksh50.00 from NCBA BANK on 15/2/26 at 3:19 PM. New M-PESA balance is Ksh101.44. Separate personal and business funds through Pochi la Biashara on *334#.",
        // 4. Paybill
        "UBE6U6JJTR Confirmed. Ksh85.00 sent to ONFON MOBILE LIMITED for account 42636407 on 14/2/26 at 9:51 PM New M-PESA balance is Ksh105.44. Transaction cost, Ksh0.00.Amount you can transact within the day is 497,907.00. Save frequent paybills for quick payment on M-PESA app `https://bit.ly/mpesalnk`",
        // 5. Buy Goods
        "UBE6U6IH6W Confirmed. Ksh130.00 paid to HAIR WE GO AGAIN 4. on 14/2/26 at 5:41 PM.New M-PESA balance is Ksh500.44. Transaction cost, Ksh0.00. Amount you can transact within the day is 498,702.00. Save frequent Tills for quick payment on M-PESA app `https://bit.ly/mpesalnk`",
        // 6. Sent to Person
        "UBE6U6I651 Confirmed. Ksh100.00 sent to Angela  Esabwa on 14/2/26 at 3:59 PM. New M-PESA balance is Ksh70.44. Transaction cost, Ksh0.00. Amount you can transact within the day is 498,832.00. Sign up for Lipa Na M-PESA Till online `https://m-pesaforbusiness.co.ke`",
        // 7. Paybill (Airtel)
        "UBE6U6I6KV confirmed. Ksh18.00 sent to AIRTEL MONEY  for account 254750288942 on 14/2/26 at 3:19 PM New M-PESA balance is Ksh230.44. Transaction cost, Ksh0.00.",
        // 8. M-Shwari Transfer
        "UBE6U6I0FD Confirmed.Ksh350.00 transferred from M-Shwari account on 14/2/26 at 2:42 PM. M-Shwari balance is Ksh0.73 .M-PESA balance is Ksh861.44 .Transaction cost Ksh.0.00",
        // 9. Received from Bank
        "UBE6U6HZ4L Confirmed. You have received Ksh300.00 from NCBA BANK on 14/2/26 at 2:40 PM. New M-PESA balance is Ksh511.44. Separate personal and business funds through Pochi la Biashara on *334#."
    )

    @Test
    fun testParseRealSamples() {
        val parsedTransactions = mutableListOf<TransactionEntity?>()
        
        for (sample in samples) {
            val transaction = MpesaParser.parse(sample)
            if (transaction == null) {
                println("FAILED to parse: $sample")
            } else {
                println("Parsed: ${transaction.transactionId} | ${transaction.type} | ${transaction.amount} | ${transaction.partyName}")
            }
            parsedTransactions.add(transaction)
        }

        // Assertions for specific known values
        assertNotNull("Sample 1 failed", parsedTransactions[0]) // Received OKWARO
        assertEquals(330.0, parsedTransactions[0]?.amount)
        assertEquals("Received", parsedTransactions[0]?.type)

        assertNotNull("Sample 2 failed", parsedTransactions[1]) // Paid to BERACAH
        assertEquals(65.0, parsedTransactions[1]?.amount)
        assertEquals("Buy Goods", parsedTransactions[1]?.type)

        assertNotNull("Sample 4 failed", parsedTransactions[3]) // Sent to ONFON
        assertEquals(85.0, parsedTransactions[3]?.amount)
        assertEquals("Paybill", parsedTransactions[3]?.type)
        
        assertNotNull("Sample 8 failed", parsedTransactions[7]) // M-Shwari
        // This one might fail with current parser
    }
}
