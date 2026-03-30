package com.ics2300.pocketbudget.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.NotificationEntity
import com.ics2300.pocketbudget.data.NotificationRepository
import kotlinx.coroutines.launch

class NotificationsViewModel(private val repository: NotificationRepository) : ViewModel() {

    val notifications: LiveData<List<NotificationEntity>> = repository.allNotifications.asLiveData()
    val unreadCount: LiveData<Int> = repository.unreadCount.asLiveData()

    fun markAsRead(notification: NotificationEntity) {
        viewModelScope.launch {
            repository.markAsRead(notification.id)
        }
    }

    fun snooze(notification: NotificationEntity) {
        // Implement snooze logic - for now, just mark as read to clear from list
        // In a real app, this would reschedule the notification.
        markAsRead(notification)
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}

class NotificationsViewModelFactory(private val repository: NotificationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
