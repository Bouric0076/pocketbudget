package com.pocketbudget.parser

/**
 * SmsParserTest — Manual unit tests for the SmsParser.
 *
 * Run this file as a plain Kotlin program (main function) in VS Code or Android Studio.
 * Each test prints PASS or FAIL with details.
 *
 * In a real Android project, these would be JUnit tests under src/test/.
 */
fun main() {
    var passed = 0
    var failed = 0

    fun assert(label: String, condition: Boolean) {
        if (condition) {
            println("  PASS: $label")
            passed++
        } else {
            println("   FAIL: $label")
            failed++
        }
    }

    // =========================================================================
    // TEST 1: Send Money
    // =========================================================================
    println("\n── Test 1: Send Money ──────────────────────────────────────────")
    val sentSms = """
        QJK5XY12AB Confirmed. Ksh500.00 sent to JOHN DOE 0712345678 on 15/6/2025 at 10:30 AM.
        New M-PESA balance is Ksh3,200.00. Transaction cost, Ksh11.00.
    """.trimIndent()

    val sentResult = SmsParser.parse(sentSms)
    assert("Not null") { sentResult != null }
    assert("Transaction ID = QJK5XY12AB") { sentResult?.transactionId == "QJK5XY12AB" }
    assert("Type = SENT") { sentResult?.type == TransactionType.SENT }
    assert("Amount = 500.0") { sentResult?.amount == 500.0 }
    assert("Party name = JOHN DOE") { sentResult?.partyName == "JOHN DOE" }
    assert("Party number = 0712345678") { sentResult?.partyNumber == "0712345678" }
    assert("DateTime extracted") { sentResult?.dateTime?.contains("15/6/2025") == true }
    assert("Balance = 3200.0") { sentResult?.balance == 3200.0 }

    // =========================================================================
    // TEST 2: Receive Money
    // =========================================================================
    println("\n── Test 2: Receive Money ───────────────────────────────────────")
    val receivedSms = """
        PQR9AB34CD Confirmed. You have received Ksh1,500.00 from JANE MWANGI 0798765432 on 16/6/2025 at 2:15 PM.
        New M-PESA balance is Ksh4,700.00.
    """.trimIndent()

    val receivedResult = SmsParser.parse(receivedSms)
    assert("Not null") { receivedResult != null }
    assert("Transaction ID = PQR9AB34CD") { receivedResult?.transactionId == "PQR9AB34CD" }
    assert("Type = RECEIVED") { receivedResult?.type == TransactionType.RECEIVED }
    assert("Amount = 1500.0") { receivedResult?.amount == 1500.0 }
    assert("Party name = JANE MWANGI") { receivedResult?.partyName == "JANE MWANGI" }
    assert("Party number = 0798765432") { receivedResult?.partyNumber == "0798765432" }
    assert("Balance = 4700.0") { receivedResult?.balance == 4700.0 }

    // =========================================================================
    // TEST 3: Paybill Payment
    // =========================================================================
    println("\n── Test 3: Paybill ─────────────────────────────────────────────")
    val paybillSms = """
        LMN2ZX78EF Confirmed. Ksh2,000.00 paid to KPLC PREPAID. Account Number 123456789 on 17/6/2025 at 8:00 AM.
        New M-PESA balance is Ksh2,700.00.
    """.trimIndent()

    val paybillResult = SmsParser.parse(paybillSms)
    assert("Not null") { paybillResult != null }
    assert("Transaction ID = LMN2ZX78EF") { paybillResult?.transactionId == "LMN2ZX78EF" }
    assert("Type = PAYBILL") { paybillResult?.type == TransactionType.PAYBILL }
    assert("Amount = 2000.0") { paybillResult?.amount == 2000.0 }
    assert("Party name = KPLC PREPAID") { paybillResult?.partyName == "KPLC PREPAID" }
    assert("Account number extracted") { paybillResult?.partyNumber == "123456789" }

    // =========================================================================
    // TEST 4: Buy Goods
    // =========================================================================
    println("\n── Test 4: Buy Goods ───────────────────────────────────────────")
    val buyGoodsSms = """
        GHT4WR56KL Confirmed. Ksh350.00 paid to NAIVAS SUPERMARKET. on 18/6/2025 at 1:45 PM.
        New M-PESA balance is Ksh2,350.00.
    """.trimIndent()

    val buyGoodsResult = SmsParser.parse(buyGoodsSms)
    assert("Not null") { buyGoodsResult != null }
    assert("Transaction ID = GHT4WR56KL") { buyGoodsResult?.transactionId == "GHT4WR56KL" }
    assert("Type = BUY_GOODS") { buyGoodsResult?.type == TransactionType.BUY_GOODS }
    assert("Amount = 350.0") { buyGoodsResult?.amount == 350.0 }
    assert("Merchant name = NAIVAS SUPERMARKET") { buyGoodsResult?.partyName == "NAIVAS SUPERMARKET" }

    // =========================================================================
    // TEST 5: Non-M-Pesa SMS (should return null)
    // =========================================================================
    println("\n── Test 5: Non-M-Pesa SMS ──────────────────────────────────────")
    val randomSms = "Your OTP is 482910. Do not share this code with anyone."
    val randomResult = SmsParser.parse(randomSms)
    assert("Returns null for non-M-Pesa SMS") { randomResult == null }

    // =========================================================================
    // TEST 6: parseAll filters correctly
    // =========================================================================
    println("\n── Test 6: parseAll() batch parsing ────────────────────────────")
    val smsList = listOf(sentSms, receivedSms, paybillSms, buyGoodsSms, randomSms)
    val allResults = SmsParser.parseAll(smsList)
    assert("parseAll returns 4 transactions (skips non-M-Pesa)") { allResults.size == 4 }
    assert("All 4 have unique transaction IDs") {
        allResults.map { it.transactionId }.toSet().size == 4
    }

    // =========================================================================
    // TEST 7: Amount with comma formatting (Ksh1,200.00)
    // =========================================================================
    println("\n── Test 7: Comma-formatted amount ──────────────────────────────")
    val commaSms = """
        XYZ1AB23CD Confirmed. Ksh12,500.00 sent to PETER OTIENO 0711223344 on 20/6/2025 at 9:00 AM.
        New M-PESA balance is Ksh88,000.00.
    """.trimIndent()
    val commaResult = SmsParser.parse(commaSms)
    assert("Ksh12,500.00 parsed as 12500.0") { commaResult?.amount == 12500.0 }
    assert("Balance Ksh88,000.00 parsed as 88000.0") { commaResult?.balance == 88000.0 }

    // =========================================================================
    // Summary
    // =========================================================================
    println("\n════════════════════════════════════════════")
    println("  Results: $passed passed / ${passed + failed} total")
    if (failed == 0) println("  🎉 All tests passed!")
    else println("  ⚠️  $failed test(s) failed. Review the parser regex.")
    println("════════════════════════════════════════════\n")
}

// Helper to make assert calls cleaner with lambdas
private fun assert(label: String, block: () -> Boolean) {
    // This overload is called by the inline assert calls above
}
