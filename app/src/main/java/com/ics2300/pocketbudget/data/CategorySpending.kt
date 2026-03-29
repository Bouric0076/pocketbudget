package com.ics2300.pocketbudget.data

data class CategorySpending(
    val categoryName: String,
    val totalAmount: Double,
    val iconName: String? = "ic_default",
    val colorHex: String? = "#0A3D2E"
)

data class ActorSpending(
    val partyName: String,
    val totalAmount: Double
)
