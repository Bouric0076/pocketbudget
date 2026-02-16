package com.ics2300.pocketbudget.data

data class CategoryBudgetProgress(
    val categoryId: Int,
    val categoryName: String,
    val totalSpent: Double,
    val budgetAmount: Double,
    val month: Int,
    val year: Int
) {
    val progress: Int
        get() = if (budgetAmount > 0) ((totalSpent / budgetAmount) * 100).toInt() else 0
        
    val remaining: Double
        get() = budgetAmount - totalSpent
        
    val isOverBudget: Boolean
        get() = totalSpent > budgetAmount && budgetAmount > 0
}
