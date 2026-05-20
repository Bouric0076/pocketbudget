package com.ics2300.pocketbudget

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ics2300.pocketbudget.data.NotificationRepository
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.ui.AppLockActivity
import com.ics2300.pocketbudget.utils.AppLockManager
import com.ics2300.pocketbudget.utils.NotificationHelper
import com.ics2300.pocketbudget.utils.SecurityUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import java.util.Calendar
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), Application.ActivityLifecycleCallbacks {

    @Inject lateinit var repository: TransactionRepository
    @Inject lateinit var notificationRepository: NotificationRepository

    val applicationScope = MainScope()

    private val startedActivities = Collections.newSetFromMap(
        WeakHashMap<Activity, Boolean>()
    )

    private var isChangingConfigurations = false

    fun resetBackgroundTime() {
        AppLockManager.unlockSession()
    }

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.createNotificationChannels(this)

        scheduleDailySummary()
        scheduleRecurringTaskCheck()
        scheduleBillReminders()
        scheduleBudgetWatcher()

        registerActivityLifecycleCallbacks(this)
        repository.startSmsListener(applicationScope)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        val wasInBackground = startedActivities.isEmpty()

        startedActivities.add(activity)
        isChangingConfigurations = false

        if (wasInBackground) {
            maybeLaunchAppLock(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        maybeLaunchAppLock(activity)
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        isChangingConfigurations = activity.isChangingConfigurations
        startedActivities.remove(activity)

        if (startedActivities.isEmpty() && !isChangingConfigurations) {
            AppLockManager.markAppBackgrounded()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        startedActivities.remove(activity)
    }

    private fun maybeLaunchAppLock(activity: Activity) {
        if (activity is AppLockActivity) return
        if (!SecurityUtils.isSecurityEnabled(this)) return

        if (AppLockManager.shouldLockAfterTimeout(SecurityUtils.LOCK_TIMEOUT_MS)) {
            val intent = Intent(activity, AppLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            activity.startActivity(intent)
            suppressTransition(activity)
        }
    }

    private fun suppressTransition(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(0, 0)
        }
    }

    private fun scheduleRecurringTaskCheck() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val recurringWorkRequest =
            PeriodicWorkRequestBuilder<com.ics2300.pocketbudget.workers.RecurringTaskWorker>(
                4,
                TimeUnit.HOURS
            )
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
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest =
            PeriodicWorkRequestBuilder<com.ics2300.pocketbudget.workers.DailySummaryWorker>(
                24,
                TimeUnit.HOURS
            )
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

        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val billWorkRequest =
            PeriodicWorkRequestBuilder<com.ics2300.pocketbudget.workers.BillReminderWorker>(
                24,
                TimeUnit.HOURS
            )
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

        val budgetWorkRequest =
            PeriodicWorkRequestBuilder<com.ics2300.pocketbudget.workers.BudgetWatcherWorker>(
                4,
                TimeUnit.HOURS
            )
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