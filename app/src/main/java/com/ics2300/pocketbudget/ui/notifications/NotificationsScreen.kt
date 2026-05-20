package com.ics2300.pocketbudget.ui.notifications

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.mutableStateMapOf
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
import java.util.Locale

private enum class NotificationFilter {
    ALL, UNREAD
}

private enum class TypeFilter {
    ALL, TRANSACTION, BILL, BUDGET, INSIGHT
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
    var typeFilter by remember { mutableStateOf(TypeFilter.ALL) }
    val expandedItems = remember { mutableStateMapOf<Int, Boolean>() }

    val notifications by viewModel.notifications.asFlow().collectAsState(initial = emptyList())

    val unreadCount = notifications.count { !it.isRead }
    val highPriorityCount = notifications.count {
        !it.isRead && (it.severity == "HIGH" || it.severity == "CRITICAL")
    }

    val filtered = notifications.filter { notification ->
        val matchesRead = when (filter) {
            NotificationFilter.ALL -> true
            NotificationFilter.UNREAD -> !notification.isRead
        }

        val matchesType = when (typeFilter) {
            TypeFilter.ALL -> true
            TypeFilter.TRANSACTION -> notification.type == "Transaction"
            TypeFilter.BILL -> notification.type == "Bill"
            TypeFilter.BUDGET -> notification.type == "Budget"
            TypeFilter.INSIGHT -> notification.type == "Insight" || notification.type == "Summary"
        }

        matchesRead && matchesType
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
                            text = when {
                                highPriorityCount > 0 -> "$highPriorityCount important • $unreadCount unread"
                                unreadCount > 0 -> "$unreadCount unread updates"
                                else -> "No pending alerts"
                            },
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
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SmartUnreadBanner(
                    unreadCount = unreadCount,
                    highPriorityCount = highPriorityCount,
                    transactionCount = notifications.count { !it.isRead && it.type == "Transaction" },
                    budgetCount = notifications.count { !it.isRead && it.type == "Budget" },
                    onMarkAllRead = { viewModel.markAllAsRead() }
                )
            }

            item {
                FilterRow(
                    selected = filter,
                    onFilterChanged = { filter = it },
                    unreadCount = unreadCount,
                    selectedType = typeFilter,
                    onTypeFilterChanged = { typeFilter = it }
                )
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyFilteredState()
                }
            }

            grouped.forEach { section ->
                item {
                    SectionHeader(
                        title = section.first,
                        count = section.second.size
                    )
                }

                items(section.second, key = { it.id }) { notification ->
                    NotificationItem(
                        notification = notification,
                        expanded = expandedItems[notification.id] == true,
                        onToggleExpand = {
                            expandedItems[notification.id] =
                                !(expandedItems[notification.id] ?: false)

                            if (!notification.isRead) {
                                viewModel.markAsRead(notification)
                            }
                        },
                        onClick = {
                            if (notification.isExpandable) {
                                expandedItems[notification.id] =
                                    !(expandedItems[notification.id] ?: false)
                            }

                            if (!notification.isRead) {
                                viewModel.markAsRead(notification)
                            }
                        },
                        onMarkRead = { viewModel.markAsRead(notification) },
                        onSnooze = { viewModel.snooze(notification) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartUnreadBanner(
    unreadCount: Int,
    highPriorityCount: Int,
    transactionCount: Int,
    budgetCount: Int,
    onMarkAllRead: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = if (highPriorityCount > 0) {
            listOf(Color(0xFF7F1D1D), Color(0xFFE53935))
        } else {
            listOf(BrandDarkGreen, BrandSecondaryGreen)
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        highPriorityCount > 0 -> "$highPriorityCount important alert${plural(highPriorityCount)}"
                        unreadCount > 0 -> "$unreadCount update${plural(unreadCount)} need review"
                        else -> "No pending alerts"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = when {
                        highPriorityCount > 0 -> "Review these first before clearing notifications."
                        transactionCount > 0 || budgetCount > 0 ->
                            "${transactionCount} transaction${plural(transactionCount)} • ${budgetCount} budget alert${plural(budgetCount)}"
                        else -> "Your financial activity is quiet right now."
                    },
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (unreadCount > 0) {
                Surface(
                    onClick = onMarkAllRead,
                    color = Color.White,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Clear",
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                        color = if (highPriorityCount > 0) Color(0xFF7F1D1D) else BrandDarkGreen,
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
    unreadCount: Int,
    selectedType: TypeFilter,
    onTypeFilterChanged: (TypeFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    selectedLabelColor = Color.White
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TypeFilter.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeFilterChanged(type) },
                    label = {
                        Text(
                            text = when (type) {
                                TypeFilter.ALL -> "Everything"
                                TypeFilter.TRANSACTION -> "Transactions"
                                TypeFilter.BILL -> "Bills"
                                TypeFilter.BUDGET -> "Budget"
                                TypeFilter.INSIGHT -> "Insights"
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandSecondaryGreen,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        selected = selectedType == type,
                        enabled = true,
                        selectedBorderColor = BrandSecondaryGreen,
                        borderColor = BrandDarkGreen.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title • $count",
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
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    onSnooze: () -> Unit = {}
) {
    val style = notificationStyle(notification)
    val titleColor =
        if (notification.isRead) BrandDarkGreen.copy(alpha = 0.78f) else BrandDarkGreen
    val messageColor =
        if (notification.isRead) TextSecondary else Color(0xFF2F3E3A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
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
                    color = style.accent.copy(alpha = if (notification.isRead) 0.12f else 0.28f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .heightIn(min = 108.dp)
                    .background(if (notification.isRead) style.accent.copy(alpha = 0.18f) else style.accent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
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

                        Spacer(modifier = Modifier.height(4.dp))

                        AmountLine(notification = notification, style = style)

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = messageColor,
                            maxLines = if (expanded) 6 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(11.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotificationChip(
                        text = notification.subtype.ifBlank { notification.type },
                        style = style
                    )

                    notification.categoryLabel
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            NotificationChip(
                                text = it,
                                style = style,
                                muted = true
                            )
                        }

                    SeverityPill(notification.severity)

                    Spacer(modifier = Modifier.weight(1f))

                    if (notification.isExpandable) {
                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = BrandDarkGreen
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    ExpandedNotificationDetails(notification = notification)
                }

                if (!notification.isRead) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (notification.type == "Bill") {
                            ActionPill(
                                text = "Snooze",
                                iconRes = R.drawable.ic_popup_reminder,
                                color = style.accent,
                                onClick = onSnooze
                            )
                        }

                        ActionPill(
                            text = "Read",
                            iconRes = null,
                            color = BrandDarkGreen,
                            onClick = onMarkRead
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountLine(
    notification: NotificationEntity,
    style: NotificationUiStyle
) {
    val amount = notification.amount

    if (amount == null) {
        Text(
            text = notification.actorName ?: notification.type,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatAmount(notification.currency, amount),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = style.accent
        )

        notification.actorName?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "• $it",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpandedNotificationDetails(notification: NotificationEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandDarkGreen.copy(alpha = 0.035f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        notification.expandedMessage
            ?.takeIf { it.isNotBlank() }
            ?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF34423E)
                )
            }

        DetailRow(label = "Type", value = notification.subtype)
        notification.categoryLabel?.let { DetailRow(label = "Category", value = it) }
        notification.actorName?.let { DetailRow(label = "Actor", value = it) }
        notification.balanceAfter?.let {
            DetailRow(label = "Balance after", value = formatAmount(notification.currency, it))
        }
        notification.transactionCost?.let {
            DetailRow(label = "Transaction fee", value = formatAmount(notification.currency, it))
        }

        notification.originalMessage
            ?.takeIf { it.isNotBlank() }
            ?.let {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Original SMS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandDarkGreen
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = BrandDarkGreen
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun NotificationChip(
    text: String,
    style: NotificationUiStyle,
    muted: Boolean = false
) {
    Surface(
        color = if (muted) BrandDarkGreen.copy(alpha = 0.05f) else style.chipBg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = if (muted) TextSecondary else style.chipText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SeverityPill(severity: String) {
    if (severity == "NORMAL") return

    val color = when (severity) {
        "CRITICAL" -> Color(0xFFB71C1C)
        "HIGH" -> Color(0xFFE53935)
        "LOW" -> Color(0xFF607D8B)
        else -> TextSecondary
    }

    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = severity.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActionPill(
    text: String,
    iconRes: Int?,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.07f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
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
            text = "No alerts right now",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = BrandDarkGreen
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your spending activity is quiet. PocketBudget will notify you when something needs attention.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyFilteredState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = "No notifications match this filter.",
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

private fun notificationStyle(notification: NotificationEntity): NotificationUiStyle {
    return when (notification.subtype) {
        "Income" -> NotificationUiStyle(
            iconRes = R.drawable.ic_money,
            accent = Color(0xFF16A34A),
            iconTint = Color(0xFF15803D),
            iconBg = Color(0xFFE8F5E9),
            chipBg = Color(0xFFE8F5E9),
            chipText = Color(0xFF166534)
        )

        "Expense" -> NotificationUiStyle(
            iconRes = R.drawable.ic_money,
            accent = Color(0xFFF97316),
            iconTint = Color(0xFFEA580C),
            iconBg = Color(0xFFFFF3E0),
            chipBg = Color(0xFFFFF3E0),
            chipText = Color(0xFF9A3412)
        )

        "Withdrawal" -> NotificationUiStyle(
            iconRes = R.drawable.ic_money,
            accent = Color(0xFFE11D48),
            iconTint = Color(0xFFBE123C),
            iconBg = Color(0xFFFFE4E6),
            chipBg = Color(0xFFFFE4E6),
            chipText = Color(0xFF9F1239)
        )

        "Reversal" -> NotificationUiStyle(
            iconRes = R.drawable.ic_popup_reminder,
            accent = Color(0xFF2563EB),
            iconTint = Color(0xFF1D4ED8),
            iconBg = Color(0xFFDBEAFE),
            chipBg = Color(0xFFDBEAFE),
            chipText = Color(0xFF1E40AF)
        )

        "Fuliza" -> NotificationUiStyle(
            iconRes = R.drawable.ic_popup_reminder,
            accent = Color(0xFFD97706),
            iconTint = Color(0xFFB45309),
            iconBg = Color(0xFFFEF3C7),
            chipBg = Color(0xFFFEF3C7),
            chipText = Color(0xFF92400E)
        )

        "Savings" -> NotificationUiStyle(
            iconRes = R.drawable.ic_money,
            accent = Color(0xFF059669),
            iconTint = Color(0xFF047857),
            iconBg = Color(0xFFD1FAE5),
            chipBg = Color(0xFFD1FAE5),
            chipText = Color(0xFF065F46)
        )

        "Airtime" -> NotificationUiStyle(
            iconRes = R.drawable.ic_money,
            accent = Color(0xFF7C3AED),
            iconTint = Color(0xFF6D28D9),
            iconBg = Color(0xFFEDE9FE),
            chipBg = Color(0xFFEDE9FE),
            chipText = Color(0xFF5B21B6)
        )

        else -> when (notification.type) {
            "Bill" -> NotificationUiStyle(
                iconRes = R.drawable.ic_popup_reminder,
                accent = Color(0xFFFF9800),
                iconTint = Color(0xFFFF9800),
                iconBg = Color(0xFFFFF3E0),
                chipBg = Color(0xFFFFF3E0),
                chipText = Color(0xFF9A5B00)
            )

            "Budget" -> NotificationUiStyle(
                iconRes = R.drawable.ic_popup_reminder,
                accent = Color(0xFFE53935),
                iconTint = Color(0xFFD32F2F),
                iconBg = Color(0xFFFFEBEE),
                chipBg = Color(0xFFFFEBEE),
                chipText = Color(0xFFB71C1C)
            )

            "Insight", "Summary" -> NotificationUiStyle(
                iconRes = R.drawable.ic_notifications_black_24dp,
                accent = BrandSecondaryGreen,
                iconTint = BrandSecondaryGreen,
                iconBg = Color(0xFFE8F5E9),
                chipBg = Color(0xFFE8F5E9),
                chipText = Color(0xFF1B5E20)
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
}

private fun formatAmount(currency: String, amount: Double): String {
    val normalizedCurrency = if (currency.isBlank()) "KES" else currency
    return String.format(
        Locale.US,
        "%s %,.2f",
        if (normalizedCurrency == "KES") "KSh" else normalizedCurrency,
        amount
    )
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

    notifications.forEach { notification ->
        when {
            notification.timestamp >= todayStart -> buckets["Today"]?.add(notification)
            notification.timestamp >= yesterdayStart -> buckets["Yesterday"]?.add(notification)
            notification.timestamp >= weekStart -> buckets["This Week"]?.add(notification)
            notification.timestamp >= monthStart -> buckets["This Month"]?.add(notification)
            else -> buckets["Earlier"]?.add(notification)
        }
    }

    return buckets
        .filter { it.value.isNotEmpty() }
        .map { it.key to it.value.sortedByDescending { item -> item.timestamp } }
}

private fun plural(count: Int): String {
    return if (count == 1) "" else "s"
}