package com.ics2300.pocketbudget.data

data class DashboardStats(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val transactionCount: Int
)
