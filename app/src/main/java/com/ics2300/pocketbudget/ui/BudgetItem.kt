package com.ics2300.pocketbudget.ui

data class BudgetItem(
    val categoryId: Int,
    val categoryName: String,
    val spentAmount: Double,
    val limitAmount: Double
)
