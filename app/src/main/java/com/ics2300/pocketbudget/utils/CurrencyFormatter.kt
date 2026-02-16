package com.ics2300.pocketbudget.utils

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    
    fun formatKsh(amount: Double): String {
        // Use US locale for comma separation, but manual currency symbol
        val format = NumberFormat.getNumberInstance(Locale.US)
        
        // Remove decimal places if it's a whole number, otherwise keep 2
        if (amount % 1 == 0.0) {
            format.maximumFractionDigits = 0
        } else {
            format.minimumFractionDigits = 2
            format.maximumFractionDigits = 2
        }
        
        return "Ksh ${format.format(amount)}"
    }
}
