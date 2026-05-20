package com.ics2300.pocketbudget.data

import android.util.Log
import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val notificationDao: NotificationDao
) {

    val allNotifications: Flow<List<NotificationEntity>> =
        notificationDao.getAllNotifications()

    val unreadCount: Flow<Int> =
        notificationDao.getUnreadCount()

    suspend fun addNotification(
        title: String,
        message: String,
        type: String,
        actionData: String? = null,
        subtype: String = "General",
        severity: String = "NORMAL",
        expandedMessage: String? = null,
        amount: Double? = null,
        currency: String = "KES",
        categoryLabel: String? = null,
        transactionId: String? = null,
        actorName: String? = null,
        isExpandable: Boolean = false,
        originalMessage: String? = null,
        balanceAfter: Double? = null,
        transactionCost: Double? = null
    ): Int {
        return try {
            val notification = NotificationEntity(
                title = title,
                message = message,
                type = type,
                subtype = subtype,
                severity = severity,
                expandedMessage = expandedMessage,
                amount = amount,
                currency = currency,
                categoryLabel = categoryLabel,
                transactionId = transactionId,
                actorName = actorName,
                timestamp = System.currentTimeMillis(),
                isExpandable = isExpandable,
                originalMessage = originalMessage,
                balanceAfter = balanceAfter,
                transactionCost = transactionCost,
                actionData = actionData
            )

            notificationDao.insertNotification(notification).toInt()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert notification.", e)
            -1
        }
    }

    suspend fun addTransactionNotification(
        title: String,
        message: String,
        subtype: String,
        severity: String,
        amount: Double?,
        categoryLabel: String?,
        transactionId: String?,
        actorName: String?,
        expandedMessage: String?,
        originalMessage: String?,
        balanceAfter: Double?,
        transactionCost: Double?,
        actionData: String?
    ): Int {
        return addNotification(
            title = title,
            message = message,
            type = "Transaction",
            subtype = subtype,
            severity = severity,
            expandedMessage = expandedMessage,
            amount = amount,
            categoryLabel = categoryLabel,
            transactionId = transactionId,
            actorName = actorName,
            isExpandable = true,
            originalMessage = originalMessage,
            balanceAfter = balanceAfter,
            transactionCost = transactionCost,
            actionData = actionData
        )
    }

    suspend fun addBudgetNotification(
        title: String,
        message: String,
        severity: String = "HIGH",
        amount: Double? = null,
        categoryLabel: String? = null,
        actionData: String? = null
    ): Int {
        return addNotification(
            title = title,
            message = message,
            type = "Budget",
            subtype = "BudgetAlert",
            severity = severity,
            expandedMessage = message,
            amount = amount,
            categoryLabel = categoryLabel,
            isExpandable = true,
            actionData = actionData
        )
    }

    suspend fun addBillNotification(
        title: String,
        message: String,
        amount: Double? = null,
        actorName: String? = null,
        actionData: String? = null
    ): Int {
        return addNotification(
            title = title,
            message = message,
            type = "Bill",
            subtype = "Reminder",
            severity = "HIGH",
            expandedMessage = message,
            amount = amount,
            actorName = actorName,
            isExpandable = true,
            actionData = actionData
        )
    }

    suspend fun addInsightNotification(
        title: String,
        message: String,
        subtype: String = "Insight",
        severity: String = "NORMAL",
        categoryLabel: String? = null,
        actionData: String? = null
    ): Int {
        return addNotification(
            title = title,
            message = message,
            type = "Insight",
            subtype = subtype,
            severity = severity,
            expandedMessage = message,
            categoryLabel = categoryLabel,
            isExpandable = true,
            actionData = actionData
        )
    }

    suspend fun markAsRead(id: Int) {
        try {
            notificationDao.markAsRead(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark notification as read: $id", e)
        }
    }

    suspend fun markAllAsRead() {
        try {
            notificationDao.markAllAsRead()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark all notifications as read.", e)
        }
    }

    suspend fun clearAll() {
        try {
            notificationDao.deleteAllNotifications()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear notifications.", e)
        }
    }

    suspend fun deleteOldNotifications(olderThanTimestamp: Long) {
        try {
            notificationDao.deleteOldNotifications(olderThanTimestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete old notifications.", e)
        }
    }

    companion object {
        private const val TAG = "NotificationRepository"
    }
}