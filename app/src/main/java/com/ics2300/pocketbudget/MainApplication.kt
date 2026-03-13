package com.ics2300.pocketbudget

import android.app.Application
import androidx.room.Room
import com.ics2300.pocketbudget.data.AppDatabase
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.data.NotificationRepository
import com.ics2300.pocketbudget.utils.SmsReader

import com.ics2300.pocketbudget.utils.NotificationHelper
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import com.ics2300.pocketbudget.ui.AppLockActivity
import com.ics2300.pocketbudget.utils.SecurityUtils

import com.ics2300.pocketbudget.utils.AppLockManager

class MainApplication : Application(), Application.ActivityLifecycleCallbacks {

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false
    // Removed legacy lastBackgroundTime variable in favor of AppLockManager

    fun resetBackgroundTime() {
        // Legacy support if needed, but primarily handled by AppLockManager
        AppLockManager.unlockSession()
    }

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
            applicationContext,
            database.transactionDao(), 
            SmsReader(applicationContext)
        ) 
    }

    val notificationRepository by lazy {
        NotificationRepository(database.notificationDao())
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        scheduleDailySummary()
        scheduleRecurringTaskCheck()
        scheduleBillReminders()
        scheduleBudgetWatcher()
        
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // App enters foreground
            if (SecurityUtils.isSecurityEnabled(this) && activity !is AppLockActivity) {
                if (AppLockManager.shouldLock()) {
                     val intent = Intent(activity, AppLockActivity::class.java)
                     intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                     activity.startActivity(intent)
                     activity.overridePendingTransition(0, 0)
                }
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        // Double check on resume to handle fast switches
        if (SecurityUtils.isSecurityEnabled(this) && activity !is AppLockActivity) {
            if (AppLockManager.shouldLock()) {
                 val intent = Intent(activity, AppLockActivity::class.java)
                 intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                 activity.startActivity(intent)
                 activity.overridePendingTransition(0, 0)
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App enters background
            AppLockManager.lockSession()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

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

    private fun scheduleBillReminders() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
            
        // Check once a day at 8 AM
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        dueDate.set(Calendar.HOUR_OF_DAY, 8) 
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        
        val billWorkRequest = PeriodicWorkRequestBuilder<com.ics2300.pocketbudget.workers.BillReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag("bill_reminders")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "bill_reminders",
            ExistingPeriodicWorkPolicy.UPDATE, 
            billWorkRequest
        )
    }

    private fun scheduleBudgetWatcher() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
            
        // Check every 4 hours
        val budgetWorkRequest = PeriodicWorkRequestBuilder<com.ics2300.pocketbudget.workers.BudgetWatcherWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("budget_watcher")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "budget_watcher",
            ExistingPeriodicWorkPolicy.UPDATE, 
            budgetWorkRequest
        )
    }
}
