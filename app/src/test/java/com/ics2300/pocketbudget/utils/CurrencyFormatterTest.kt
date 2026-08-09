package com.ics2300.pocketbudget.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun formatWholeNumberOmitsDecimalPlaces() {
        val formatted = CurrencyFormatter.formatKsh(1500.0)
        assertEquals("Ksh 1,500", formatted)
    }

    @Test
    fun formatDecimalKeepsTwoFractionDigits() {
        val formatted = CurrencyFormatter.formatKsh(1500.5)
        assertEquals("Ksh 1,500.50", formatted)
    }

    @Test
    fun formatLargeNumberUsesGroupingSeparators() {
        val formatted = CurrencyFormatter.formatKsh(1234567.89)
        assertEquals("Ksh 1,234,567.89", formatted)
    }

    @Test
    fun formatNegativeNumberPreservesSign() {
        val formatted = CurrencyFormatter.formatKsh(-250.0)
        assertEquals("Ksh -250", formatted)
    }
}

