package com.ics2300.pocketbudget.data

import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) {

    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun addNotification(title: String, message: String, type: String, actionData: String? = null): Int {
        val notification = NotificationEntity(
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            actionData = actionData
        )
        return notificationDao.insertNotification(notification).toInt()
    }

    suspend fun markAsRead(id: Int) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun clearAll() {
        notificationDao.deleteAllNotifications()
    }
}
