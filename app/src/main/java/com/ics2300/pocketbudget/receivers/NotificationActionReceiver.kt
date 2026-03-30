package com.ics2300.pocketbudget.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ics2300.pocketbudget.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra("notification_db_id", -1)
        val androidNotificationId = intent.getIntExtra("android_notification_id", -1)
        
        val app = context.applicationContext as? MainApplication ?: return
        val repository = app.notificationRepository

        when (action) {
            ACTION_MARK_AS_READ -> {
                if (notificationId != -1) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.markAsRead(notificationId)
                    }
                }
                
                // Dismiss the notification from tray
                if (androidNotificationId != -1) {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(androidNotificationId)
                }
            }
            ACTION_DISMISS -> {
                if (androidNotificationId != -1) {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(androidNotificationId)
                }
            }
        }
    }

    companion object {
        const val ACTION_MARK_AS_READ = "com.ics2300.pocketbudget.ACTION_MARK_AS_READ"
        const val ACTION_DISMISS = "com.ics2300.pocketbudget.ACTION_DISMISS"
    }
}
