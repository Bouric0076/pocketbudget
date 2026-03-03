package com.ics2300.pocketbudget.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MpesaParserBehaviorTest {

    @Test
    fun parsePaybillMessageProducesPaybillTransaction() {
        val body =
            "UBE6U6JJTR Confirmed. Ksh85.00 sent to ONFON MOBILE LIMITED for account 42636407 on 14/2/26 at 9:51 PM New M-PESA balance is Ksh105.44."

        val transaction = MpesaParser.parse(body)

        assertNotNull(transaction)
        assertEquals("UBE6U6JJTR", transaction?.transactionId)
        assertEquals(85.0, transaction?.amount, 0.0)
        assertEquals("Paybill", transaction?.type)
        assertEquals("ONFON MOBILE LIMITED", transaction?.partyName)
    }

    @Test
    fun parseBuyGoodsMessageProducesBuyGoodsTransaction() {
        val body =
            "UBF6U6LHVI Confirmed. Ksh65.00 paid to BERACAH SUPERMARKET. on 15/2/26 at 3:20 PM.New M-PESA balance is Ksh36.44."

        val transaction = MpesaParser.parse(body)

        assertNotNull(transaction)
        assertEquals(65.0, transaction?.amount, 0.0)
        assertEquals("Buy Goods", transaction?.type)
        assertEquals("BERACAH SUPERMARKET", transaction?.partyName)
    }

    @Test
    fun parseSentToPersonMessageProducesSentTransaction() {
        val body =
            "UBE6U6I651 Confirmed. Ksh100.00 sent to Angela  Esabwa on 14/2/26 at 3:59 PM. New M-PESA balance is Ksh70.44."

        val transaction = MpesaParser.parse(body)

        assertNotNull(transaction)
        assertEquals(100.0, transaction?.amount, 0.0)
        assertEquals("Sent", transaction?.type)
        assertEquals("Angela Esabwa", transaction?.partyName)
    }

    @Test
    fun parseReceivedFromPersonMessageProducesReceivedTransaction() {
        val body =
            "UBGG96SKAZ Confirmed.You have received Ksh330.00 from OKWARO  THOMAS 0704832328 on 16/2/26 at 5:56 AM  New M-PESA balance is Ksh370.44."

        val transaction = MpesaParser.parse(body)

        assertNotNull(transaction)
        assertEquals(330.0, transaction?.amount, 0.0)
        assertEquals("Received", transaction?.type)
        assertEquals(true, transaction?.partyName?.contains("OKWARO THOMAS") == true)
    }

    @Test
    fun parseMshwariTransferHandlesMissingSpaceAfterConfirmed() {
        val body =
            "UBE6U6I0FD Confirmed.Ksh350.00 transferred from M-Shwari account on 14/2/26 at 2:42 PM. M-Shwari balance is Ksh0.73 ."

        val transaction = MpesaParser.parse(body)

        assertNotNull(transaction)
        assertEquals(350.0, transaction?.amount, 0.0)
        assertEquals("Deposit", transaction?.type)
        assertEquals(true, transaction?.partyName?.contains("M-Shwari", ignoreCase = true) == true)
    }

    @Test
    fun parseNonMpesaMessageReturnsNull() {
        val body = "This is not an M-Pesa transaction message."

        val transaction = MpesaParser.parse(body)

        assertNull(transaction)
    }
}

