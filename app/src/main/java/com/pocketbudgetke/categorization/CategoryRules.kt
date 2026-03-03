package com.pocketbudgetke.categorization

object CategoryRules {

    val categories = listOf(
        Category(1, "Transport"),
        Category(2, "Food"),
        Category(3, "Airtime & Bundles"),
        Category(4, "Rent"),
        Category(5, "School"),
        Category(6, "Uncategorized")
    )

    val keywordMap: Map<Int, List<String>> = mapOf(
        1 to listOf("uber", "bolt", "stage", "fare", "taxi", "bus"),
        2 to listOf("hotel", "cafe", "restaurant", "food", "pizza"),
        3 to listOf("airtime", "bundle", "data", "safaricom"),
        4 to listOf("rent", "landlord", "house"),
        5 to listOf("school", "fees", "tuition", "college")
    )

    fun getCategoryById(id: Int): Category {
        return categories.firstOrNull { it.categoryId == id }
            ?: categories.last()
    }
}
