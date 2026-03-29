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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.asFlow
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.ActorSpending
import com.ics2300.pocketbudget.ui.dashboard.DashboardStats
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModel
import com.ics2300.pocketbudget.ui.theme.*
import com.ics2300.pocketbudget.utils.CategoryUtils
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import kotlin.math.roundToInt

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.sqrt

@Composable
fun AnalyticsScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val analyticsSummary by viewModel.analyticsSummary.asFlow()
        .collectAsState(initial = DashboardStats(0.0, 0.0, 0.0, 0))
    val dailyTrend by viewModel.analyticsDailyTrend.asFlow().collectAsState(initial = emptyList())
    val categoryData by viewModel.analyticsCategoryData.asFlow().collectAsState(initial = emptyList())
    val topActors by viewModel.analyticsTopActors.asFlow().collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(1) }
    val tabs = listOf("Last Month", "This Month")

    var selectedCategoryForDrillDown by remember { mutableStateOf<String?>(null) }
    val showDrillDownSheet = remember { mutableStateOf(false) }

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
                    dailyTrend = dailyTrend,
                    categoryData = categoryData,
                    onCategorySelected = { categoryName ->
                        selectedCategoryForDrillDown = categoryName
                        showDrillDownSheet.value = true
                    }
                )
            }

            if (topActors.isNotEmpty()) {
                item {
                    TopActorsSection(topActors)
                }
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

            val totalSpentCount = categoryData.sumOf { it.totalSpent }
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
                        percentage = if (totalSpentCount > 0) (item.totalSpent / totalSpentCount).toFloat() else 0f,
                        iconRes = CategoryUtils.getIconResId(item.iconName),
                        color = Color(CategoryUtils.getColor(item.colorHex)),
                        onClick = {
                            selectedCategoryForDrillDown = item.categoryName
                            showDrillDownSheet.value = true
                        }
                    )
                }
            }
        }

        if (showDrillDownSheet.value && selectedCategoryForDrillDown != null) {
            CategoryDrillDownBottomSheet(
                categoryName = selectedCategoryForDrillDown!!,
                viewModel = viewModel,
                onDismiss = { showDrillDownSheet.value = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDrillDownBottomSheet(
    categoryName: String,
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val allTransactions by viewModel.allTransactions.asFlow().collectAsState(initial = emptyList())
    val categories by viewModel.categories.asFlow().collectAsState(initial = emptyList())
    
    val category = categories.find { it.name == categoryName }
    val filteredTransactions = allTransactions.filter { it.categoryId == category?.id }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "$categoryName Transactions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandDarkGreen,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No transactions found", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        DrillDownTransactionItem(transaction)
                    }
                }
            }
        }
    }
}

@Composable
fun DrillDownTransactionItem(transaction: com.ics2300.pocketbudget.data.TransactionEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandBackgroundGray, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BrandDarkGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_money),
                contentDescription = null,
                tint = BrandDarkGreen,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.partyName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = BrandDarkGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sdf = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }
            Text(
                text = sdf.format(java.util.Date(transaction.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        
        Text(
            text = CurrencyFormatter.formatKsh(transaction.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = BrandDarkGreen
        )
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
                    label = if (savingsRate >= 0) "Savings" else "Deficit",
                    value = "${savingsRate.roundToInt()}%",
                    valueColor = if (savingsRate >= 0) BrandLightGreen else Color(0xFFFFB199)
                )
            }
        }
    }
}

@Composable
private fun TopActorsSection(actors: List<ActorSpending>) {
    Column {
        Text(
            text = "Top Recipients",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actors.forEach { actor ->
                TopActorCard(actor)
            }
        }
    }
}

@Composable
private fun TopActorCard(actor: ActorSpending) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(140.dp)
            .border(1.dp, BrandDarkGreen.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandBackgroundGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_money),
                    contentDescription = null,
                    tint = BrandDarkGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = actor.partyName,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = CurrencyFormatter.formatKsh(actor.totalAmount),
                style = MaterialTheme.typography.labelSmall,
                color = BrandSecondaryGreen,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
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
    dailyTrend: List<ChartData>,
    categoryData: List<com.ics2300.pocketbudget.data.CategoryBudgetProgress>,
    onCategorySelected: (String) -> Unit
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
                text = "Spending Distribution",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InteractivePieChart(
                    categoryData = categoryData,
                    onCategorySelected = onCategorySelected,
                    modifier = Modifier
                        .size(180.dp)
                        .weight(1f)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryData.sortedByDescending { it.totalSpent }.take(4).forEach { item ->
                        ChartLegendItem(
                            label = item.categoryName,
                            color = Color(CategoryUtils.getColor(item.colorHex))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Daily Trend",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BrandDarkGreen,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            BarChartCompose(
                data = dailyTrend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}

@Composable
fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun InteractivePieChart(
    categoryData: List<com.ics2300.pocketbudget.data.CategoryBudgetProgress>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSpent = categoryData.sumOf { it.totalSpent }
    if (totalSpent <= 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "No data", color = TextSecondary)
        }
        return
    }

    var center by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .pointerInput(categoryData) {
                detectTapGestures { offset ->
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    val distance = sqrt(dx * dx + dy * dy)
                    val radius = (if (size.width < size.height) size.width else size.height).toFloat() / 2
                    
                    // Only detect taps inside the ring (donut)
                    if (distance <= radius && distance >= radius * 0.6f) {
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angle < 0) angle += 360f
                        
                        // Pie starts at -90 degrees (top)
                        var currentAngle = 270f 
                        
                        categoryData.forEach { item ->
                            val sweep = ((item.totalSpent / totalSpent) * 360).toFloat()
                            val start = currentAngle % 360
                            val end = (currentAngle + sweep) % 360
                            
                            val isWithin = if (start < end) {
                                angle in start..end
                            } else {
                                angle >= start || angle <= end
                            }
                            
                            if (isWithin && item.totalSpent > 0) {
                                onCategorySelected(item.categoryName)
                                return@detectTapGestures
                            }
                            currentAngle += sweep
                        }
                    }
                }
            }
    ) {
        center = this.center
        val strokeWidth = 35.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        var currentAngle = -90f

        categoryData.forEach { item ->
            if (item.totalSpent > 0) {
                val sweep = ((item.totalSpent / totalSpent) * 360).toFloat()
                drawArc(
                    color = Color(CategoryUtils.getColor(item.colorHex)),
                    startAngle = currentAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                currentAngle += sweep
            }
        }
        
        // Draw total in center
        drawContext.canvas.nativeCanvas.apply {
            val paint = Paint().apply {
                color = BrandDarkGreen.toArgb()
                textSize = 14.dp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            drawText("Top", center.x, center.y - 5.dp.toPx(), paint)
            paint.textSize = 11.dp.toPx()
            paint.color = TextSecondary.toArgb()
            drawText("Spends", center.x, center.y + 12.dp.toPx(), paint)
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
    color: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BrandBackgroundGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandDarkGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandDarkGreen
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f),
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatKsh(amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandDarkGreen
                )
                Text(
                    text = "${(percentage * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
