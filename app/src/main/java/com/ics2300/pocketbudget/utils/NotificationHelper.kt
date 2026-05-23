package com.ics2300.pocketbudget.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ics2300.pocketbudget.MainActivity
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.receivers.NotificationActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val PREF_NAME = "notification_settings"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

    const val CHANNEL_TRANSACTIONS = "transaction_alerts"
    const val CHANNEL_DAILY_SUMMARY = "daily_summary"
    const val CHANNEL_BILL_REMINDERS = "bill_reminders"
    const val CHANNEL_BUDGET_ALERTS = "budget_alerts"

    private const val GROUP_MPESA_TRANSACTIONS = "group_mpesa_transactions"

    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class TransactionNotificationCopy(
        val title: String,
        val message: String,
        val expandedText: String,
        val subtype: String,
        val severity: String
    )

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val transactionChannel = NotificationChannel(
            CHANNEL_TRANSACTIONS,
            "Transaction Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Smart alerts for received money, payments, withdrawals, reversals, and M-Pesa activity"
        }

        val summaryChannel = NotificationChannel(
            CHANNEL_DAILY_SUMMARY,
            "Daily Summary",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Daily spending summary"
        }

        val billsChannel = NotificationChannel(
            CHANNEL_BILL_REMINDERS,
            "Bill Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for upcoming bills"
        }

        val budgetChannel = NotificationChannel(
            CHANNEL_BUDGET_ALERTS,
            "Budget Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when approaching or exceeding budget limits"
        }

        manager.createNotificationChannels(
            listOf(transactionChannel, summaryChannel, billsChannel, budgetChannel)
        )
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        expandedText: String = message,
        type: String,
        subtype: String = "General",
        severity: String = "NORMAL",
        amount: Double? = null,
        categoryLabel: String? = null,
        transactionId: String? = null,
        actorName: String? = null,
        originalMessage: String? = null,
        balanceAfter: Double? = null,
        transactionCost: Double? = null,
        androidId: Int,
        iconRes: Int = R.drawable.ic_money,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        actionData: String? = null,
        extraActions: List<NotificationCompat.Action> = emptyList(),
        groupId: String? = null
    ): Boolean {
        val app = context.applicationContext as? MainApplication
        if (app == null) {
            Log.e(TAG, "Application context is not MainApplication.")
            return false
        }

        if (!areNotificationsEnabled(context)) {
            Log.i(TAG, "Notifications are disabled by user preference. Notification skipped.")
            return false
        }

        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Notification skipped.")
            return false
        }

        notificationScope.launch {
            try {
                val dbId = app.notificationRepository.addNotification(
                    title = title,
                    message = message,
                    type = type,
                    subtype = subtype,
                    severity = severity,
                    expandedMessage = expandedText,
                    amount = amount,
                    categoryLabel = categoryLabel,
                    transactionId = transactionId,
                    actorName = actorName,
                    isExpandable = expandedText.isNotBlank(),
                    originalMessage = originalMessage,
                    balanceAfter = balanceAfter,
                    transactionCost = transactionCost,
                    actionData = actionData
                )

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "notifications")
                    putExtra("notification_id", dbId)
                    actionData?.let { putExtra("action_data", it) }
                }

                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    androidId,
                    openIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_MARK_AS_READ
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_DB_ID, dbId)
                    putExtra(NotificationActionReceiver.EXTRA_ANDROID_NOTIFICATION_ID, androidId)
                }

                val markReadPendingIntent = PendingIntent.getBroadcast(
                    context,
                    dbId,
                    markReadIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(iconRes)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
                    .setContentIntent(openPendingIntent)
                    .setAutoCancel(true)
                    .setPriority(priority)
                    .addAction(
                        android.R.drawable.ic_menu_view,
                        "Open",
                        openPendingIntent
                    )
                    .addAction(
                        android.R.drawable.ic_menu_edit,
                        "Mark Read",
                        markReadPendingIntent
                    )

                if (groupId != null) {
                    builder.setGroup(groupId)
                }

                extraActions.forEach { action ->
                    builder.addAction(action)
                }

                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                manager.notify(androidId, builder.build())

                if (groupId != null) {
                    showGroupSummary(context, channelId, groupId)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to create or show notification.", e)
            }
        }

        return true
    }

    private fun showGroupSummary(
        context: Context,
        channelId: String,
        groupId: String
    ) {
        if (!areNotificationsEnabled(context) || !hasNotificationPermission(context)) return

        try {
            val summaryNotification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_money)
                .setContentTitle("PocketBudget transactions")
                .setContentText("New M-Pesa activity detected")
                .setStyle(
                    NotificationCompat.InboxStyle()
                        .addLine("Recent transactions have been added")
                        .addLine("Open PocketBudget to review categories")
                        .setSummaryText("M-Pesa activity")
                )
                .setGroup(groupId)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.notify(groupId.hashCode(), summaryNotification)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show transaction group summary.", e)
        }
    }

    fun notifyNewTransaction(
        context: Context,
        transaction: TransactionEntity,
        categoryName: String
    ) {
        val copy = buildTransactionNotificationCopy(context, transaction, categoryName)
        val notificationId = transaction.transactionId.hashCode()

        val changeCategoryIntent =
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_CHANGE_CATEGORY
                putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transaction.transactionId)
                putExtra(NotificationActionReceiver.EXTRA_ANDROID_NOTIFICATION_ID, notificationId)
            }

        val changeCategoryPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            changeCategoryIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        showNotification(
            context = context,
            channelId = CHANNEL_TRANSACTIONS,
            title = copy.title,
            message = copy.message,
            expandedText = copy.expandedText,
            type = "Transaction",
            subtype = copy.subtype,
            severity = copy.severity,
            amount = transaction.amount,
            categoryLabel = categoryName,
            transactionId = transaction.transactionId,
            actorName = transaction.partyName,
            originalMessage = transaction.fullSmsBody,
            balanceAfter = transaction.balanceAfter,
            transactionCost = transaction.transactionCost,
            androidId = notificationId,
            iconRes = iconForTransaction(transaction),
            priority = priorityForSeverity(copy.severity),
            actionData = transaction.transactionId,
            extraActions = listOf(
                NotificationCompat.Action(
                    R.drawable.ic_category,
                    "Change Category",
                    changeCategoryPendingIntent
                )
            ),
            groupId = GROUP_MPESA_TRANSACTIONS
        )
    }

    private fun buildTransactionNotificationCopy(
        context: Context,
        transaction: TransactionEntity,
        categoryName: String
    ): TransactionNotificationCopy {
        val privacyMode = SecurityUtils.isPrivacyModeEnabled(context)

        val amount = CurrencyFormatter.formatKsh(transaction.amount, privacyMode)
        val party = if (privacyMode) {
            "Private contact"
        } else {
            transaction.partyName.ifBlank { "Unknown" }
        }

        val category = categoryName.ifBlank { "Uncategorized" }
        val account = transaction.accountName?.takeIf { it.isNotBlank() }

        if (privacyMode) {
            return TransactionNotificationCopy(
                title = titleForTransactionType(transaction.type),
                message = "A ${transaction.type.lowercase()} transaction was added.",
                expandedText = "Privacy mode is on. Open PocketBudget to review this transaction securely.",
                subtype = subtypeForTransaction(transaction),
                severity = severityForTransaction(transaction)
            )
        }

        return when (transaction.type) {
            "Received" -> TransactionNotificationCopy(
                title = "Money Received",
                message = "$amount received from $party",
                expandedText = "$amount received from $party. Categorized as $category.",
                subtype = "Income",
                severity = "NORMAL"
            )

            "Sent" -> TransactionNotificationCopy(
                title = "Money Sent",
                message = "$amount sent to $party",
                expandedText = "$amount sent to $party. Categorized as $category.",
                subtype = "Expense",
                severity = severityForTransaction(transaction)
            )

            "Paybill" -> TransactionNotificationCopy(
                title = "Paybill Payment",
                message = "$amount paid to $party",
                expandedText = buildString {
                    append("$amount paid to $party.")
                    if (account != null) append(" Account: $account.")
                    append(" Categorized as $category.")
                },
                subtype = "Expense",
                severity = severityForTransaction(transaction)
            )

            "Buy Goods" -> TransactionNotificationCopy(
                title = "Buy Goods Payment",
                message = "$amount paid to $party",
                expandedText = "$amount paid to $party. Categorized as $category.",
                subtype = "Expense",
                severity = severityForTransaction(transaction)
            )

            "Withdraw" -> TransactionNotificationCopy(
                title = "Cash Withdrawal",
                message = "$amount withdrawn from $party",
                expandedText = "$amount withdrawn from $party. Categorized as $category.",
                subtype = "Withdrawal",
                severity = severityForTransaction(transaction)
            )

            "Airtime" -> TransactionNotificationCopy(
                title = "Airtime Purchase",
                message = "$amount airtime bought for $party",
                expandedText = "$amount airtime bought for $party. Categorized as $category.",
                subtype = "Airtime",
                severity = "LOW"
            )

            "Reversal" -> TransactionNotificationCopy(
                title = "Transaction Reversed",
                message = "$amount reversal detected",
                expandedText = "$amount reversal detected for $party. Categorized as $category.",
                subtype = "Reversal",
                severity = "HIGH"
            )

            "Fuliza Loan" -> TransactionNotificationCopy(
                title = "Fuliza Used",
                message = "$amount covered by Fuliza",
                expandedText = "$amount was covered through Fuliza M-PESA. Categorized as $category.",
                subtype = "Fuliza",
                severity = "HIGH"
            )

            "Fuliza Repayment" -> TransactionNotificationCopy(
                title = "Fuliza Repaid",
                message = "$amount used to repay Fuliza",
                expandedText = "$amount was used to repay Fuliza M-PESA. Categorized as $category.",
                subtype = "Fuliza",
                severity = "NORMAL"
            )

            "Deposit" -> TransactionNotificationCopy(
                title = "Money Moved In",
                message = "$amount moved from $party",
                expandedText = "$amount moved from $party. Categorized as $category.",
                subtype = "Savings",
                severity = "NORMAL"
            )

            "Savings" -> TransactionNotificationCopy(
                title = "Savings Transfer",
                message = "$amount moved to $party",
                expandedText = "$amount moved to $party. Categorized as $category.",
                subtype = "Savings",
                severity = "NORMAL"
            )

            else -> TransactionNotificationCopy(
                title = "Transaction Added",
                message = "$amount transaction with $party",
                expandedText = "$amount transaction with $party. Categorized as $category.",
                subtype = "General",
                severity = "NORMAL"
            )
        }
    }

    private fun titleForTransactionType(type: String): String {
        return when (type) {
            "Received" -> "Money Received"
            "Sent" -> "Money Sent"
            "Paybill" -> "Paybill Payment"
            "Buy Goods" -> "Buy Goods Payment"
            "Withdraw" -> "Cash Withdrawal"
            "Airtime" -> "Airtime Purchase"
            "Reversal" -> "Transaction Reversed"
            "Fuliza Loan" -> "Fuliza Used"
            "Fuliza Repayment" -> "Fuliza Repaid"
            "Deposit" -> "Money Moved In"
            "Savings" -> "Savings Transfer"
            else -> "Transaction Added"
        }
    }

    private fun subtypeForTransaction(transaction: TransactionEntity): String {
        return when (transaction.type) {
            "Received" -> "Income"
            "Sent", "Paybill", "Buy Goods" -> "Expense"
            "Withdraw" -> "Withdrawal"
            "Airtime" -> "Airtime"
            "Reversal" -> "Reversal"
            "Fuliza Loan", "Fuliza Repayment" -> "Fuliza"
            "Deposit", "Savings" -> "Savings"
            else -> "General"
        }
    }

    private fun severityForTransaction(transaction: TransactionEntity): String {
        return when {
            transaction.type == "Reversal" -> "HIGH"
            transaction.type == "Fuliza Loan" -> "HIGH"
            transaction.amount >= 10_000 -> "HIGH"
            transaction.amount >= 3_000 -> "NORMAL"
            transaction.type == "Airtime" -> "LOW"
            else -> "NORMAL"
        }
    }

    private fun iconForTransaction(transaction: TransactionEntity): Int {
        return when (subtypeForTransaction(transaction)) {
            "Reversal", "Fuliza" -> R.drawable.ic_popup_reminder
            else -> R.drawable.ic_money
        }
    }

    private fun priorityForSeverity(severity: String): Int {
        return when (severity) {
            "CRITICAL" -> NotificationCompat.PRIORITY_HIGH
            "HIGH" -> NotificationCompat.PRIORITY_HIGH
            "LOW" -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    fun showBillReminder(
        context: Context,
        billName: String,
        amount: Double,
        daysUntil: Int,
        isSnoozed: Boolean = false
    ): Boolean {
        val isPrivacyMode = SecurityUtils.isPrivacyModeEnabled(context)
        val amountText = CurrencyFormatter.formatKsh(amount, isPrivacyMode)

        val timeText = when (daysUntil) {
            0 -> "today"
            1 -> "tomorrow"
            else -> "in $daysUntil days"
        }

        val titleText = if (isSnoozed) {
            "Snoozed Bill Reminder"
        } else {
            "Upcoming Bill"
        }

        val message = if (isPrivacyMode) {
            "A bill is likely due $timeText."
        } else {
            "$billName of $amountText is likely due $timeText."
        }

        val notificationId = billName.hashCode()

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_BILL_NAME, billName)
            putExtra(NotificationActionReceiver.EXTRA_AMOUNT, amount)
            putExtra(NotificationActionReceiver.EXTRA_ANDROID_NOTIFICATION_ID, notificationId)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_popup_reminder,
            "Snooze 1h",
            snoozePendingIntent
        ).build()

        return showNotification(
            context = context,
            channelId = CHANNEL_BILL_REMINDERS,
            title = titleText,
            message = message,
            expandedText = message,
            type = "Bill",
            subtype = "Reminder",
            severity = "HIGH",
            amount = amount,
            actorName = billName,
            androidId = notificationId,
            iconRes = R.drawable.ic_popup_reminder,
            priority = NotificationCompat.PRIORITY_HIGH,
            actionData = billName,
            extraActions = listOf(snoozeAction)
        )
    }

    fun showBudgetAlert(
        context: Context,
        title: String,
        message: String,
        id: Int
    ) {
        showNotification(
            context = context,
            channelId = CHANNEL_BUDGET_ALERTS,
            title = title,
            message = message,
            expandedText = message,
            type = "Budget",
            subtype = "BudgetAlert",
            severity = "HIGH",
            androidId = id,
            iconRes = R.drawable.ic_popup_reminder,
            priority = NotificationCompat.PRIORITY_HIGH,
            actionData = id.toString()
        )
    }

    fun showDailySummaryNotification(
        context: Context,
        amount: Double
    ) {
        val isPrivacyMode = SecurityUtils.isPrivacyModeEnabled(context)
        val formattedAmount = CurrencyFormatter.formatKsh(amount, isPrivacyMode)

        val message = if (isPrivacyMode) {
            "Your daily spending summary is ready."
        } else {
            "You spent $formattedAmount today."
        }

        showNotification(
            context = context,
            channelId = CHANNEL_DAILY_SUMMARY,
            title = "Daily Spending Summary",
            message = message,
            expandedText = message,
            type = "Summary",
            subtype = "DailySummary",
            severity = "LOW",
            amount = amount,
            androidId = 1001,
            iconRes = R.drawable.ic_money,
            priority = NotificationCompat.PRIORITY_LOW
        )
    }
}
