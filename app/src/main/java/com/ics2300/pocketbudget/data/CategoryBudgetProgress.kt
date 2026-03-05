package com.ics2300.pocketbudget.data

data class CategoryBudgetProgress(
    val categoryId: Int,
    val categoryName: String,
    val totalSpent: Double,
    val budgetAmount: Double,
    val month: Int,
    val year: Int,
    val iconName: String = "ic_default",
    val colorHex: String = "#0A3D2E"
) {
    val progress: Int
        get() = if (budgetAmount > 0) ((totalSpent / budgetAmount) * 100).toInt() else 0
        
    val remaining: Double
        get() = budgetAmount - totalSpent
        
    val isOverBudget: Boolean
        get() = totalSpent > budgetAmount && budgetAmount > 0
}
