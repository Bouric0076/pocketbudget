package com.ics2300.pocketbudget.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val keywords: String, // Comma separated keywords for auto-categorization
    val iconName: String = "ic_default", // e.g. "ic_food", "ic_transport"
    val colorHex: String = "#0A3D2E" // Default brand dark green
)
