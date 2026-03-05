package com.ics2300.pocketbudget.utils

import android.graphics.Color
import com.ics2300.pocketbudget.R

object CategoryUtils {

    val availableColors = listOf(
        "#0A3D2E", // Brand Dark Green
        "#D4E157", // Brand Light Green
        "#F44336", // Red
        "#E91E63", // Pink
        "#9C27B0", // Purple
        "#673AB7", // Deep Purple
        "#3F51B5", // Indigo
        "#2196F3", // Blue
        "#03A9F4", // Light Blue
        "#00BCD4", // Cyan
        "#009688", // Teal
        "#4CAF50", // Green
        "#8BC34A", // Light Green
        "#CDDC39", // Lime
        "#FFEB3B", // Yellow
        "#FFC107", // Amber
        "#FF9800", // Orange
        "#FF5722", // Deep Orange
        "#795548", // Brown
        "#9E9E9E", // Grey
        "#607D8B"  // Blue Grey
    )

    // Using standard android drawables as placeholders for now since we don't have custom assets
    // In a real app, these would be R.drawable.ic_category_food, etc.
    val iconMap = mapOf(
        "ic_default" to android.R.drawable.ic_menu_my_calendar,
        "ic_food" to android.R.drawable.ic_menu_my_calendar, // Placeholder
        "ic_transport" to android.R.drawable.ic_menu_directions,
        "ic_shopping" to android.R.drawable.ic_menu_gallery,
        "ic_home" to android.R.drawable.ic_menu_manage, // Using settings icon for home/utilities
        "ic_bills" to android.R.drawable.ic_menu_agenda,
        "ic_health" to android.R.drawable.ic_menu_add,
        "ic_entertainment" to android.R.drawable.ic_menu_camera,
        "ic_education" to android.R.drawable.ic_menu_edit,
        "ic_savings" to android.R.drawable.ic_menu_save,
        "ic_other" to android.R.drawable.ic_menu_help
    )

    fun getIconResId(iconName: String): Int {
        return iconMap[iconName] ?: android.R.drawable.ic_menu_my_calendar
    }

    fun getColor(colorHex: String): Int {
        return try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            Color.parseColor("#0A3D2E") // Default
        }
    }
}
