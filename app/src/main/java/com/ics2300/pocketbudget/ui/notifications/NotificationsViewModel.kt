package com.ics2300.pocketbudget.ui.notifications

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.NotificationEntity
import com.ics2300.pocketbudget.data.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    val notifications: LiveData<List<NotificationEntity>> =
        repository.allNotifications.asLiveData()

    val unreadCount: LiveData<Int> =
        repository.unreadCount.asLiveData()

    fun markAsRead(notification: NotificationEntity) {
        if (notification.isRead) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.markAsRead(notification.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark notification as read.", e)
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.markAllAsRead()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark all notifications as read.", e)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.clearAll()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear notifications.", e)
            }
        }
    }

    fun handleNotificationClick(notification: NotificationEntity) {
        markAsRead(notification)
    }

    fun canSnooze(notification: NotificationEntity): Boolean {
        return notification.type == "Bill"
    }

    fun snooze(notification: NotificationEntity) {
        if (!canSnooze(notification)) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.markAsRead(notification.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to snooze notification.", e)
            }
        }
    }

    companion object {
        private const val TAG = "NotificationsViewModel"
    }
}

class NotificationsViewModelFactory(
    private val repository: NotificationRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}