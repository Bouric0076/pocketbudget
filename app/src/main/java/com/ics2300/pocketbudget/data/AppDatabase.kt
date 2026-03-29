package com.ics2300.pocketbudget.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class, RecurringTransactionEntity::class, NotificationEntity::class, ActorCategoryMapping::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun notificationDao(): NotificationDao
}
