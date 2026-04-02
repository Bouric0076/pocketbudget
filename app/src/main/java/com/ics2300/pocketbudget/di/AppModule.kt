package com.ics2300.pocketbudget.di

import android.content.Context
import androidx.room.Room
import com.ics2300.pocketbudget.data.AppDatabase
import com.ics2300.pocketbudget.data.TransactionDao
import com.ics2300.pocketbudget.data.NotificationDao
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.data.NotificationRepository
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pocket-budget-database"
        ).fallbackToDestructiveMigration().build()
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
    fun provideNotificationRepository(notificationDao: NotificationDao): NotificationRepository {
        return NotificationRepository(notificationDao)
    }
}
