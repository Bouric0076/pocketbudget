package com.ics2300.pocketbudget

import android.app.Application
import androidx.room.Room
import com.ics2300.pocketbudget.data.AppDatabase
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.utils.SmsReader

import com.ics2300.pocketbudget.utils.NotificationHelper
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainApplication : Application() {
    val database by lazy { 
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "pocket-budget-database"
        )
        .fallbackToDestructiveMigration() // Simplified for dev; use migrations in prod
        .build() 
    }
    
    val repository by lazy { 
        TransactionRepository(
            database.transactionDao(), 
            SmsReader(applicationContext)
        ) 
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        scheduleDailySummary()
        scheduleRecurringTaskCheck()
    }

    private fun scheduleRecurringTaskCheck() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Check every 4 hours (minimum 15 mins, but 4-12h is reasonable)
        val recurringWorkRequest = PeriodicWorkRequestBuilder<com.ics2300.pocketbudget.workers.RecurringTaskWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("recurring_check")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "recurring_check",
            ExistingPeriodicWorkPolicy.UPDATE, 
            recurringWorkRequest
        )
    }

    private fun scheduleDailySummary() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        dueDate.set(Calendar.HOUR_OF_DAY, 21) // 9 PM
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<com.ics2300.pocketbudget.workers.DailySummaryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag("daily_summary")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_summary",
            ExistingPeriodicWorkPolicy.UPDATE, 
            dailyWorkRequest
        )
    }
}
