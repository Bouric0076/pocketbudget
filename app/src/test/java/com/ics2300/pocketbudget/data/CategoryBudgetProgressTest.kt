package com.ics2300.pocketbudget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBudgetProgressTest {

    @Test
    fun zeroBudgetHasZeroProgressAndRemaining() {
        val progress = CategoryBudgetProgress(
            categoryId = 1,
            categoryName = "Food",
            totalSpent = 0.0,
            budgetAmount = 0.0,
            month = 1,
            year = 2026
        )

        assertEquals(0, progress.progress)
        assertEquals(0.0, progress.remaining, 0.0)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun underBudgetHasPositiveRemainingAndNotOverBudget() {
        val progress = CategoryBudgetProgress(
            categoryId = 2,
            categoryName = "Transport",
            totalSpent = 250.0,
            budgetAmount = 1000.0,
            month = 1,
            year = 2026
        )

        assertEquals(25, progress.progress)
        assertEquals(750.0, progress.remaining, 0.0)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun overBudgetHasNegativeRemainingAndIsOverBudget() {
        val progress = CategoryBudgetProgress(
            categoryId = 3,
            categoryName = "Entertainment",
            totalSpent = 200.0,
            budgetAmount = 100.0,
            month = 1,
            year = 2026
        )

        assertEquals(200, progress.progress)
        assertEquals(-100.0, progress.remaining, 0.0)
        assertTrue(progress.isOverBudget)
    }
}

