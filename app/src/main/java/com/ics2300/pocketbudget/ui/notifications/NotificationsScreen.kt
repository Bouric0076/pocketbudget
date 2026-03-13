package com.ics2300.pocketbudget.ui.notifications

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.NotificationEntity
import com.ics2300.pocketbudget.ui.theme.BrandBackgroundGray
import com.ics2300.pocketbudget.ui.theme.BrandDarkGreen
import com.ics2300.pocketbudget.ui.theme.BrandLightGreen
import com.ics2300.pocketbudget.ui.theme.BrandSecondaryGreen
import com.ics2300.pocketbudget.ui.theme.TextSecondary
import java.util.Calendar

private enum class NotificationFilter {
    ALL, UNREAD
}

private data class NotificationUiStyle(
    val iconRes: Int,
    val accent: Color,
    val iconTint: Color,
    val iconBg: Color,
    val chipBg: Color,
    val chipText: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onBackClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(NotificationFilter.ALL) }

    val notifications by viewModel.notifications.asFlow().collectAsState(initial = emptyList())

    val unreadCount = notifications.count { !it.isRead }
    val filtered = when (filter) {
        NotificationFilter.ALL -> notifications
        NotificationFilter.UNREAD -> notifications.filter { !it.isRead }
    }

    val grouped = remember(filtered) { groupNotifications(filtered) }

    Scaffold(
        containerColor = BrandBackgroundGray,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandDarkGreen
                        )
                        Text(
                            text = if (unreadCount > 0) "$unreadCount unread" else "All caught up",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandDarkGreen
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = BrandDarkGreen
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mark all as read") },
                                onClick = {
                                    viewModel.markAllAsRead()
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear all", color = Color(0xFFE53935)) },
                                onClick = {
                                    viewModel.clearAll()
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935)
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBackgroundGray)
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                UnreadBanner(
                    unreadCount = unreadCount,
                    onMarkAllRead = { viewModel.markAllAsRead() }
                )
            }

            item {
                FilterRow(
                    selected = filter,
                    onFilterChanged = { filter = it },
                    unreadCount = unreadCount
                )
            }

            grouped.forEach { section ->
                item {
                    SectionHeader(title = section.first)
                }
                items(section.second, key = { it.id }) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            if (!notification.isRead) {
                                viewModel.markAsRead(notification)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnreadBanner(
    unreadCount: Int,
    onMarkAllRead: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(BrandDarkGreen, BrandSecondaryGreen)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(BrandLightGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = BrandLightGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (unreadCount > 0) "You have $unreadCount unread updates" else "Everything is read",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (unreadCount > 0) "Stay on top of budgets, bills, and transactions" else "You're up to date",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (unreadCount > 0) {
                Surface(
                    onClick = onMarkAllRead,
                    color = BrandLightGreen,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Mark all",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = BrandDarkGreen,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: NotificationFilter,
    onFilterChanged: (NotificationFilter) -> Unit,
    unreadCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterChip(
            selected = selected == NotificationFilter.ALL,
            onClick = { onFilterChanged(NotificationFilter.ALL) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandDarkGreen,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
            ),
            border = FilterChipDefaults.filterChipBorder(
                selected = selected == NotificationFilter.ALL,
                enabled = true,
                selectedBorderColor = BrandDarkGreen,
                borderColor = BrandDarkGreen.copy(alpha = 0.2f)
            )
        )

        FilterChip(
            selected = selected == NotificationFilter.UNREAD,
            onClick = { onFilterChanged(NotificationFilter.UNREAD) },
            label = { Text("Unread ($unreadCount)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandDarkGreen,
                selectedLabelColor = Color.White
            ),
            border = FilterChipDefaults.filterChipBorder(
                selected = selected == NotificationFilter.UNREAD,
                enabled = true,
                selectedBorderColor = BrandDarkGreen,
                borderColor = BrandDarkGreen.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = BrandDarkGreen,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(BrandDarkGreen.copy(alpha = 0.08f))
        )
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationEntity,
    onClick: () -> Unit
) {
    val style = notificationStyle(notification.type)
    val titleColor = if (notification.isRead) BrandDarkGreen.copy(alpha = 0.9f) else BrandDarkGreen
    val messageColor = if (notification.isRead) TextSecondary else Color(0xFF3C4A46)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFFCFFFD)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = BrandDarkGreen.copy(alpha = if (notification.isRead) 0.07f else 0.14f),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .heightIn(min = 94.dp)
                    .background(if (notification.isRead) BrandDarkGreen.copy(alpha = 0.08f) else style.accent)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(style.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = style.iconRes),
                        contentDescription = null,
                        tint = style.iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.ExtraBold,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = relativeTime(notification.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = messageColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = style.chipBg,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = notification.type.ifBlank { "General" },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = style.chipText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!notification.isRead) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(BrandDarkGreen.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_notifications_black_24dp),
                contentDescription = null,
                tint = BrandDarkGreen.copy(alpha = 0.7f),
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No notifications yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = BrandDarkGreen
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You’re all caught up. Important reminders and budget updates will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun notificationStyle(type: String): NotificationUiStyle {
    return when (type) {
        "Bill" -> NotificationUiStyle(
            iconRes = R.drawable.ic_popup_reminder,
            accent = Color(0xFFFF9800),
            iconTint = Color(0xFFFF9800),
            iconBg = Color(0xFFFFF3E0),
            chipBg = Color(0xFFFFF3E0),
            chipText = Color(0xFF9A5B00)
        )

        "Transaction" -> NotificationUiStyle(
            iconRes = R.drawable.ic_money,
            accent = Color(0xFF26A69A),
            iconTint = Color(0xFF00897B),
            iconBg = Color(0xFFE0F2F1),
            chipBg = Color(0xFFE0F2F1),
            chipText = Color(0xFF00695C)
        )

        "Budget" -> NotificationUiStyle(
            iconRes = R.drawable.ic_popup_reminder,
            accent = Color(0xFFE53935),
            iconTint = Color(0xFFD32F2F),
            iconBg = Color(0xFFFFEBEE),
            chipBg = Color(0xFFFFEBEE),
            chipText = Color(0xFFB71C1C)
        )

        else -> NotificationUiStyle(
            iconRes = R.drawable.ic_notifications_black_24dp,
            accent = BrandSecondaryGreen,
            iconTint = BrandSecondaryGreen,
            iconBg = Color(0xFFE8F5E9),
            chipBg = Color(0xFFE8F5E9),
            chipText = Color(0xFF1B5E20)
        )
    }
}

private fun relativeTime(timestamp: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

private fun groupNotifications(
    notifications: List<NotificationEntity>
): List<Pair<String, List<NotificationEntity>>> {
    if (notifications.isEmpty()) return emptyList()

    val now = Calendar.getInstance()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterdayStart = Calendar.getInstance().apply {
        timeInMillis = todayStart
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

    val weekStart = Calendar.getInstance().apply {
        timeInMillis = todayStart
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }.timeInMillis

    val monthStart = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val buckets = linkedMapOf(
        "Today" to mutableListOf<NotificationEntity>(),
        "Yesterday" to mutableListOf(),
        "This Week" to mutableListOf(),
        "This Month" to mutableListOf(),
        "Earlier" to mutableListOf()
    )

    notifications.forEach { n ->
        when {
            n.timestamp >= todayStart -> buckets["Today"]?.add(n)
            n.timestamp >= yesterdayStart -> buckets["Yesterday"]?.add(n)
            n.timestamp >= weekStart -> buckets["This Week"]?.add(n)
            n.timestamp >= monthStart -> buckets["This Month"]?.add(n)
            else -> buckets["Earlier"]?.add(n)
        }
    }

    return buckets
        .filter { it.value.isNotEmpty() }
        .map { it.key to it.value.sortedByDescending { item -> item.timestamp } }
}
