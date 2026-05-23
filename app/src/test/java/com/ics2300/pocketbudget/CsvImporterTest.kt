package com.ics2300.pocketbudget

import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.utils.CsvImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CsvImporterTest {

    @Test
    fun parseTransactions_prefersCategoryNameOverLegacyId() {
        val categories = listOf(
            CategoryEntity(id = 2, name = "Food & Dining", keywords = "CAFE"),
            CategoryEntity(id = 9, name = "Transport", keywords = "UBER")
        )
        val lines = sequenceOf(
            "ID,Date,Amount,Type,Party,CategoryId,CategoryName",
            "TX1,2026-05-23 10:00:00,450.0,Buy Goods,Java House,9,Food & Dining"
        )

        val transactions = CsvImporter.parseTransactions(lines, categories)

        assertEquals(1, transactions.size)
        assertEquals(2, transactions.first().categoryId)
    }

    @Test
    fun parseTransactions_supportsLegacyCategoryIdWhenItExists() {
        val categories = listOf(
            CategoryEntity(id = 7, name = "Utilities", keywords = "KPLC")
        )
        val lines = sequenceOf(
            "ID,Date,Amount,Type,Party,Category",
            "TX2,2026-05-23 10:00:00,1000.0,Paybill,KPLC,7"
        )

        val transactions = CsvImporter.parseTransactions(lines, categories)

        assertEquals(1, transactions.size)
        assertEquals(7, transactions.first().categoryId)
    }

    @Test
    fun parseTransactions_ignoresLegacyCategoryIdWhenItDoesNotExist() {
        val categories = listOf(
            CategoryEntity(id = 3, name = "Transport", keywords = "UBER")
        )
        val lines = sequenceOf(
            "ID,Date,Amount,Type,Party,Category",
            "TX3,2026-05-23 10:00:00,300.0,Buy Goods,Unknown Merchant,99"
        )

        val transactions = CsvImporter.parseTransactions(lines, categories)

        assertEquals(1, transactions.size)
        assertNull(transactions.first().categoryId)
    }
}
