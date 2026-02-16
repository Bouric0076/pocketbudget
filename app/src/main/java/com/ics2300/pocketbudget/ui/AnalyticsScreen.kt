package com.ics2300.pocketbudget.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.asFlow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.ui.dashboard.DashboardStats
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModel
import com.ics2300.pocketbudget.ui.theme.*
import com.ics2300.pocketbudget.utils.CurrencyFormatter

@Composable
fun AnalyticsScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val analyticsSummary by viewModel.analyticsSummary.asFlow().collectAsState(initial = DashboardStats(0.0, 0.0, 0.0, 0))
    val dailyTrend by viewModel.analyticsDailyTrend.asFlow().collectAsState(initial = emptyList())
    val categoryData by viewModel.analyticsCategoryData.asFlow().collectAsState(initial = emptyList())
    
    var selectedTabIndex by remember { mutableIntStateOf(1) }

    LaunchedEffect(selectedTabIndex) {
        viewModel.setAnalyticsFilter(selectedTabIndex == 1)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Report",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_my_calendar),
                        contentDescription = "Calendar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Last Month") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("This Month") }
                    )
                }
            }

            // Main Chart Card (Bar Chart)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Total Spending",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = CurrencyFormatter.formatKsh(analyticsSummary.totalExpense),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        BarChartCompose(
                            data = dailyTrend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }

            // Income / Expense Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Income Card
                    SummaryCard(
                        title = "Income",
                        amount = analyticsSummary.totalIncome,
                        color = AnalyticsTeal,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Expense Card
                    SummaryCard(
                        title = "Expense",
                        amount = analyticsSummary.totalExpense,
                        color = AnalyticsCoral,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Category Breakdown Title
            item {
                Text(
                    text = "Category Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Category List Items
            val totalSpent = categoryData.sumOf { it.totalSpent }
            items(categoryData.sortedByDescending { it.totalSpent }) { item ->
                CategoryItem(
                    name = item.categoryName,
                    amount = item.totalSpent,
                    percentage = if (totalSpent > 0) (item.totalSpent / totalSpent).toFloat() else 0f
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = CurrencyFormatter.formatKsh(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Donut Chart
            Box(contentAlignment = Alignment.Center) {
                DonutChart(
                    value = if (amount > 0) 1f else 0f,
                    color = color,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
    }
}

@Composable
fun BarChartCompose(
    data: List<ChartData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxValue = (data.maxOfOrNull { it.value } ?: 0.0) * 1.2
    val barColor = AnalyticsTeal
    val labelColor = TextSecondary.toArgb()
    val gridColor = Color.LightGray.copy(alpha = 0.5f).toArgb()

    val scrollState = rememberScrollState()
    
    // Calculate width based on data size to enable scrolling
    val barWidth = 20.dp
    val spacing = 16.dp
    val minWidth = 300.dp // Minimum width to fill screen
    val calculatedWidth = (barWidth + spacing) * data.size
    
    // We wrap Canvas in a Box with horizontalScroll
    Box(modifier = modifier) {
        // Y-Axis Labels (Fixed)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val availableHeight = size.height - 40.dp.toPx()
            val steps = 3
            val textPaint = Paint().apply {
                color = labelColor
                textSize = 30f
                textAlign = Paint.Align.RIGHT
            }
            
            for (i in 0..steps) {
                val y = 20.dp.toPx() + (availableHeight / steps) * i
                if (maxValue > 0) {
                    val value = maxValue * (1 - i.toFloat() / steps)
                    val label = if (value >= 1000) "${(value/1000).toInt()}k" else "${value.toInt()}"
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        50.dp.toPx(), // Fixed width for Y labels
                        y + 10f,
                        textPaint
                    )
                }
                drawLine(
                    color = Color(gridColor),
                    start = Offset(60.dp.toPx(), y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f
                )
            }
        }

        // Scrollable Bars
        Row(
            modifier = Modifier
                .padding(start = 60.dp, top = 20.dp, bottom = 20.dp) // Offset for Y labels
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(maxOf(minWidth, calculatedWidth))
                    .fillMaxHeight()
            ) {
                val availableHeight = size.height - 20.dp.toPx() // Bottom padding for X labels
                val barWidthPx = barWidth.toPx()
                val spacingPx = spacing.toPx()
                
                val textPaint = Paint().apply {
                    color = labelColor
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                }

                data.forEachIndexed { index, item ->
                    val x = index * (barWidthPx + spacingPx)
                    val barHeight = (item.value / maxValue * availableHeight).toFloat()
                    
                    // Draw Bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, availableHeight - barHeight),
                        size = Size(barWidthPx, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    
                    // Draw X Label
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
    value: Float, // 0..1
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 12.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        
        // Background Ring
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.2f),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )
        
        // Progress Arc
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360 * value,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
    }
}

@Composable
fun CategoryItem(
    name: String,
    amount: Double,
    percentage: Float
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = CurrencyFormatter.formatKsh(amount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AnalyticsTeal,
                    trackColor = Color.LightGray.copy(alpha = 0.2f),
                )
            }
        }
    }
}
