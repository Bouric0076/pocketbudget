package com.ics2300.pocketbudget.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Bill", "Budget", "System"
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionData: String? = null // Optional data for actions (e.g., transaction ID, category ID)
)
