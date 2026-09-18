package com.ics2300.pocketbudget.data

data class DashboardStats(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val transactionCount: Int,
    val totalSavings: Double = 0.0,
    val totalTransfers: Double = 0.0,
    val totalBorrowed: Double = 0.0
)
