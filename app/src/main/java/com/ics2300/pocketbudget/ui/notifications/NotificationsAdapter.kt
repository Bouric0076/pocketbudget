package com.ics2300.pocketbudget.ui.notifications

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.NotificationEntity

class NotificationsAdapter(private val onItemClick: (NotificationEntity) -> Unit) :
    ListAdapter<NotificationEntity, NotificationsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
        holder.itemView.setOnClickListener { onItemClick(notification) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textTitle)
        private val message: TextView = itemView.findViewById(R.id.textMessage)
        private val time: TextView = itemView.findViewById(R.id.textTime)
        private val icon: ImageView = itemView.findViewById(R.id.iconNotification)
        private val unreadIndicator: View = itemView.findViewById(R.id.viewUnreadIndicator)

        fun bind(notification: NotificationEntity) {
            title.text = notification.title
            message.text = notification.message
            time.text = DateUtils.getRelativeTimeSpanString(
                notification.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            
            // Set icons based on type
            when (notification.type) {
                "Bill" -> {
                    icon.setImageResource(R.drawable.ic_popup_reminder)
                    icon.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFFF3E0.toInt()) // Light Orange
                    icon.imageTintList = android.content.res.ColorStateList.valueOf(0xFFFF9800.toInt()) // Orange
                }
                "Transaction" -> {
                    icon.setImageResource(R.drawable.ic_money)
                    icon.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE8F5E9.toInt()) // Light Green
                    icon.imageTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt()) // Green
                }
                "Budget" -> {
                    icon.setImageResource(R.drawable.ic_popup_reminder)
                    icon.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFFEBEE.toInt()) // Light Red
                    icon.imageTintList = android.content.res.ColorStateList.valueOf(0xFFF44336.toInt()) // Red
                }
                else -> {
                    icon.setImageResource(R.drawable.ic_notifications_black_24dp)
                    icon.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE3F2FD.toInt()) // Light Blue
                    icon.imageTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt()) // Blue
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationEntity>() {
        override fun areItemsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem == newItem
    }
}
