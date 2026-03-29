package com.ics2300.pocketbudget.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "actor_category_mappings")
data class ActorCategoryMapping(
    @PrimaryKey val partyName: String, // Normalized uppercase party name
    val categoryId: Int
)
