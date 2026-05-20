package com.ics2300.pocketbudget.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ics2300.pocketbudget.data.AppDatabase
import com.ics2300.pocketbudget.data.NotificationDao
import com.ics2300.pocketbudget.data.NotificationRepository
import com.ics2300.pocketbudget.data.TransactionDao
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.utils.SmsReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `actor_category_mappings` (
                    `partyName` TEXT NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    PRIMARY KEY(`partyName`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `subtype` TEXT NOT NULL DEFAULT 'General'")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `severity` TEXT NOT NULL DEFAULT 'NORMAL'")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `expandedMessage` TEXT")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `amount` REAL")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'KES'")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `categoryLabel` TEXT")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `transactionId` TEXT")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `actorName` TEXT")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `isExpandable` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `originalMessage` TEXT")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `balanceAfter` REAL")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `transactionCost` REAL")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pocket-budget-database"
        )
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(appDatabase: AppDatabase): TransactionDao {
        return appDatabase.transactionDao()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(appDatabase: AppDatabase): NotificationDao {
        return appDatabase.notificationDao()
    }

    @Provides
    @Singleton
    fun provideSmsReader(@ApplicationContext context: Context): SmsReader {
        return SmsReader(context)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        @ApplicationContext context: Context,
        transactionDao: TransactionDao,
        smsReader: SmsReader
    ): TransactionRepository {
        return TransactionRepository(context, transactionDao, smsReader)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationDao: NotificationDao
    ): NotificationRepository {
        return NotificationRepository(notificationDao)
    }
}