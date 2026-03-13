package com.ics2300.pocketbudget.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.ui.dashboard.DashboardStats
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModel
import com.ics2300.pocketbudget.ui.theme.*
import com.ics2300.pocketbudget.utils.CategoryUtils
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val analyticsSummary by viewModel.analyticsSummary.asFlow()
        .collectAsState(initial = DashboardStats(0.0, 0.0, 0.0, 0))
    val dailyTrend by viewModel.analyticsDailyTrend.asFlow().collectAsState(initial = emptyList())
    val categoryData by viewModel.analyticsCategoryData.asFlow().collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(1) }
    val tabs = listOf("Last Month", "This Month")

    LaunchedEffect(selectedTabIndex) {
        viewModel.setAnalyticsFilter(selectedTabIndex == 1)
    }

    val totalIncome = analyticsSummary.totalIncome
    val totalExpense = analyticsSummary.totalExpense
    val netFlow = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netFlow / totalIncome) * 100.0).coerceIn(-999.0, 999.0) else 0.0
    val avgDailySpend = if (dailyTrend.isNotEmpty()) totalExpense / dailyTrend.size else 0.0

    Scaffold(
        containerColor = BrandBackgroundGray,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderSection()
            }

            item {
                PeriodSegmentedControl(
                    tabs = tabs,
                    selectedIndex = selectedTabIndex,
                    onSelected = { selectedTabIndex = it }
                )
            }

            item {
                HeroSummaryCard(
                    netFlow = netFlow,
                    income = totalIncome,
                    expense = totalExpense,
                    savingsRate = savingsRate
                )
            }

            item {
                SpendingChartCard(
                    totalExpense = totalExpense,
                    dailyTrend = dailyTrend
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Income",
                        amount = totalIncome,
                        color = AnalyticsTeal,
                        iconRes = R.drawable.ic_money,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Expense",
                        amount = totalExpense,
                        color = AnalyticsCoral,
                        iconRes = R.drawable.ic_money,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InsightMiniCard(
                        title = "Avg Daily Spend",
                        value = CurrencyFormatter.formatKsh(avgDailySpend),
                        tint = AnalyticsPurple,
                        modifier = Modifier.weight(1f)
                    )
                    InsightMiniCard(
                        title = "Transactions",
                        value = analyticsSummary.transactionCount.toString(),
                        tint = BrandSecondaryGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Top Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandDarkGreen,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val totalSpent = categoryData.sumOf { it.totalSpent }
            val sortedCategories = categoryData.sortedByDescending { it.totalSpent }

            if (sortedCategories.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                BrandDarkGreen.copy(alpha = 0.08f),
                                RoundedCornerShape(18.dp)
                            )
                    ) {
                        Text(
                            text = "No spending data available for this period.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(sortedCategories.withIndex().toList()) { indexed ->
                    val rank = indexed.index + 1
                    val item = indexed.value
                    CategoryItem(
                        rank = rank,
                        name = item.categoryName,
                        amount = item.totalSpent,
                        percentage = if (totalSpent > 0) (item.totalSpent / totalSpent).toFloat() else 0f,
                        iconRes = CategoryUtils.getIconResId(item.iconName),
                        color = Color(CategoryUtils.getColor(item.colorHex))
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Report & Stats",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = BrandDarkGreen
            )
            Text(
                text = "Track trends and spending insights",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, BrandDarkGreen.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_my_calendar),
                contentDescription = "Calendar",
                tint = BrandDarkGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PeriodSegmentedControl(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(22.dp))
            .border(1.dp, BrandDarkGreen.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(5.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) BrandDarkGreen else Color.Transparent)
                    .clickable { onSelected(index) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) BrandLightGreen else TextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun HeroSummaryCard(
    netFlow: Double,
    income: Double,
    expense: Double,
    savingsRate: Double
) {
    val gradient = Brush.linearGradient(
        colors = listOf(BrandDarkGreen, BrandSecondaryGreen)
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            Text(
                text = "Net Flow",
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = CurrencyFormatter.formatKsh(netFlow),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroMetric(
                    label = "Income",
                    value = CurrencyFormatter.formatKsh(income),
                    valueColor = BrandLightGreen
                )
                HeroMetric(
                    label = "Expense",
                    value = CurrencyFormatter.formatKsh(expense),
                    valueColor = Color(0xFFFFB199)
                )
                HeroMetric(
                    label = "Savings",
                    value = "${savingsRate.roundToInt()}%",
                    valueColor = if (savingsRate >= 0) BrandLightGreen else Color(0xFFFFB199)
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SpendingChartCard(
    totalExpense: Double,
    dailyTrend: List<ChartData>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrandDarkGreen.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Total Spending",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = CurrencyFormatter.formatKsh(totalExpense),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = BrandDarkGreen,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            BarChartCompose(
                data = dailyTrend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.border(
            1.dp,
            BrandDarkGreen.copy(alpha = 0.08f),
            RoundedCornerShape(20.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = CurrencyFormatter.formatKsh(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = BrandDarkGreen,
                modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)
            )

            DonutChart(
                value = if (amount > 0) 1f else 0f,
                color = color,
                modifier = Modifier.size(58.dp)
            )
        }
    }
}

@Composable
private fun InsightMiniCard(
    title: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.border(
            1.dp,
            BrandDarkGreen.copy(alpha = 0.08f),
            RoundedCornerShape(16.dp)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = tint,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun BarChartCompose(
    data: List<ChartData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "No data for this period", color = TextSecondary)
        }
        return
    }

    val maxValue = (data.maxOfOrNull { it.value } ?: 0.0) * 1.2
    val barColor = BrandSecondaryGreen
    val labelColor = TextSecondary.toArgb()
    val gridColor = Color.LightGray.copy(alpha = 0.3f).toArgb()

    val scrollState = rememberScrollState()
    val barWidth = 24.dp
    val spacing = 14.dp
    val minWidth = 320.dp
    val calculatedWidth = (barWidth + spacing) * data.size

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val availableHeight = size.height - 40.dp.toPx()
            val steps = 3
            val textPaint = Paint().apply {
                color = labelColor
                textSize = 30f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            for (i in 0..steps) {
                val y = 20.dp.toPx() + (availableHeight / steps) * i
                if (maxValue > 0) {
                    val value = maxValue * (1 - i.toFloat() / steps)
                    val label = if (value >= 1000) "${(value / 1000).toInt()}k" else "${value.toInt()}"
                    drawContext.canvas.nativeCanvas.drawText(label, 55.dp.toPx(), y + 10f, textPaint)
                }
                drawLine(
                    color = Color(gridColor),
                    start = Offset(65.dp.toPx(), y),
                    end = Offset(size.width, y),
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(start = 65.dp, top = 20.dp, bottom = 20.dp)
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(maxOf(minWidth, calculatedWidth))
                    .fillMaxHeight()
            ) {
                val availableHeight = size.height - 20.dp.toPx()
                val barWidthPx = barWidth.toPx()
                val spacingPx = spacing.toPx()

                val textPaint = Paint().apply {
                    color = labelColor
                    textSize = 26f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                data.forEachIndexed { index, item ->
                    val x = index * (barWidthPx + spacingPx)
                    val barHeight = if (maxValue > 0) (item.value / maxValue * availableHeight).toFloat() else 0f

                    if (barHeight > 0) {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, availableHeight - barHeight),
                            size = Size(barWidthPx, barHeight),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }

                    if (data.size <= 15 || index % 2 == 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            item.label,
                            x + barWidthPx / 2,
                            size.height,
                            textPaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    value: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 12.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2

        drawCircle(
            color = Color.LightGray.copy(alpha = 0.2f),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )

        if (value > 0) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360 * value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }
    }
}

@Composable
fun CategoryItem(
    rank: Int,
    name: String,
    amount: Double,
    percentage: Float,
    iconRes: Int,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrandDarkGreen.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandDarkGreen.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandDarkGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandDarkGreen
                    )
                    Text(
                        text = CurrencyFormatter.formatKsh(amount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = color,
                        trackColor = Color.LightGray.copy(alpha = 0.16f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(percentage * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
