package com.ics2300.pocketbudget.ui.analytics

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.ics2300.pocketbudget.data.ActorSpending
import com.ics2300.pocketbudget.data.CategoryBudgetProgress
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.DashboardStats
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.ui.ChartData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(private val repository: TransactionRepository) : ViewModel() {

    private val analyticsMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    private val analyticsYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    val categories: LiveData<List<CategoryEntity>> = repository.allCategories.asLiveData()
    val allTransactions: LiveData<List<TransactionEntity>> = repository.allTransactions.asLiveData()

    val analyticsCategoryData: LiveData<List<CategoryBudgetProgress>> = combine(
        analyticsMonth,
        analyticsYear
    ) { month: Int, year: Int ->
        repository.getBudgetProgress(month, year)
    }.flatMapLatest { it }.asLiveData()

    val analyticsDailyTrend: LiveData<List<ChartData>> = combine(
        analyticsMonth,
        analyticsYear,
        repository.allTransactions
    ) { month: Int, year: Int, transactions: List<TransactionEntity> ->
        val filtered = transactions.filter {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            (cal.get(Calendar.MONTH) + 1) == month && cal.get(Calendar.YEAR) == year &&
            (it.type != "Received" && it.type != "Deposit")
        }
        
        // Group by day
        val grouped = filtered.groupBy { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.DAY_OF_MONTH)
        }
        
        // Map to ChartData (Day 1..31)
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.YEAR, year)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        (1..daysInMonth).map { day ->
            val amount = grouped[day]?.sumOf { it.amount } ?: 0.0
            
            val dayCal = Calendar.getInstance()
            dayCal.set(Calendar.MONTH, month - 1)
            dayCal.set(Calendar.YEAR, year)
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            val label = SimpleDateFormat("d MMM", Locale.getDefault()).format(dayCal.time)
            
            ChartData(label, amount)
        }
    }.asLiveData()

    val analyticsSummary: LiveData<DashboardStats> = combine(
        analyticsMonth,
        analyticsYear
    ) { month: Int, year: Int ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis

        repository.getDashboardStats(start, end)
    }.flatMapLatest { it }.asLiveData()

    val previousMonthSummary: LiveData<DashboardStats> = combine(
        analyticsMonth,
        analyticsYear
    ) { month: Int, year: Int ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.YEAR, year)
        cal.add(Calendar.MONTH, -1) // Previous Month
        
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis

        repository.getDashboardStats(start, end)
    }.flatMapLatest { it }.asLiveData()

    val analyticsTopActors: LiveData<List<ActorSpending>> = combine(
        analyticsMonth,
        analyticsYear
    ) { month: Int, year: Int ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis

        repository.getTopSpendingActorsByDate(5, start, end)
    }.flatMapLatest { it }.asLiveData()

    fun setAnalyticsFilter(isThisMonth: Boolean) {
        val cal = Calendar.getInstance()
        if (!isThisMonth) {
            cal.add(Calendar.MONTH, -1)
        }
        analyticsMonth.value = cal.get(Calendar.MONTH) + 1
        analyticsYear.value = cal.get(Calendar.YEAR)
    }
}
