package com.pocketbudgetke.categorization

class Categorizer {

    /**
     * Categorizes a transaction based on description text.
     * Manual category override takes priority.
     *
     * @param description SMS body or transaction party name
     * @param manualCategoryId Category selected by user (nullable)
     * @return Category
     */
    fun categorize(
        description: String,
        manualCategoryId: Int? = null
    ): Category {

        // Manual override
        if (manualCategoryId != null) {
            return CategoryRules.getCategoryById(manualCategoryId)
        }

        val text = description.lowercase()

        for ((categoryId, keywords) in CategoryRules.keywordMap) {
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    return CategoryRules.getCategoryById(categoryId)
                }
            }
        }

        return CategoryRules.getCategoryById(6) // Uncategorized
    }
}
