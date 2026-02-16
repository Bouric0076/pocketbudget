package com.ics2300.pocketbudget.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.CategorySpending
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.data.RecurringTransactionEntity
import com.ics2300.pocketbudget.data.CategoryBudgetProgress
import com.ics2300.pocketbudget.ui.ChartData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class TimeRange {
    DAY, WEEK, MONTH, YEAR, ALL
}

enum class TransactionFilter {
    ALL, INCOME, EXPENSE, UNCATEGORIZED
}

data class DashboardStats(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val transactionCount: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val timeRange = MutableStateFlow(TimeRange.DAY)
    private val filterType = MutableStateFlow(TransactionFilter.ALL)
    
    private val currentCalendar = Calendar.getInstance()
    val currentMonth = MutableStateFlow(currentCalendar.get(Calendar.MONTH) + 1) // 1-12
    val currentYear = MutableStateFlow(currentCalendar.get(Calendar.YEAR))

    // For Budget Fragment
    val budgetProgress: LiveData<List<CategoryBudgetProgress>> = combine(
        currentMonth,
        currentYear
    ) { month, year ->
        repository.getBudgetProgress(month, year)
    }.flatMapLatest { it }.asLiveData()

    fun setBudget(categoryId: Int, amount: Double) {
        viewModelScope.launch {
            repository.setBudget(categoryId, amount, currentMonth.value, currentYear.value)
        }
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insert(transaction)
        }
    }

    fun addRecurringTransaction(
        amount: Double,
        description: String,
        categoryId: Int,
        type: String,
        frequency: String,
        startDate: Long
    ) {
        viewModelScope.launch {
            val recurring = RecurringTransactionEntity(
                amount = amount,
                description = description,
                categoryId = categoryId,
                type = type,
                frequency = frequency,
                startDate = startDate,
                nextDueDate = startDate, // Starts immediately or scheduled date
                isActive = true
            )
            repository.addRecurringTransaction(recurring)
            
            // Trigger a check immediately to create the first one if due
            repository.processDueRecurringTransactions()
        }
    }

    // For Transactions Fragment (Searchable + Filterable + Date Range)
    private val dateRangeFilter = MutableStateFlow<Pair<Long, Long>?>(null)

    fun setDateRangeFilter(start: Long, end: Long) {
        dateRangeFilter.value = Pair(start, end)
    }

    fun clearDateRangeFilter() {
        dateRangeFilter.value = null
    }

    val allTransactions: LiveData<List<TransactionEntity>> = combine(
        searchQuery,
        filterType,
        dateRangeFilter,
        repository.allTransactions
    ) { query, filter, dateRange, transactions ->
        var result = transactions
        
        // Apply Date Range
        if (dateRange != null) {
            result = result.filter { it.timestamp in dateRange.first..dateRange.second }
        }
        
        // Apply Search
        if (query.isNotBlank()) {
            result = result.filter { 
                it.partyName.contains(query, ignoreCase = true)
            }
        }
        
        // Apply Filter
        result = when (filter) {
            TransactionFilter.ALL -> result
            TransactionFilter.INCOME -> result.filter { it.type.equals("Received", ignoreCase = true) || it.type.equals("Deposit", ignoreCase = true) }
            TransactionFilter.EXPENSE -> result.filter { !it.type.equals("Received", ignoreCase = true) && !it.type.equals("Deposit", ignoreCase = true) }
            TransactionFilter.UNCATEGORIZED -> result.filter { it.categoryId == 1 } 
        }
        
        result
    }.asLiveData()

    // For Dashboard Stats (Filtered by TimeRange)
    val dashboardStats: LiveData<DashboardStats> = combine(
        repository.allTransactions,
        timeRange
    ) { transactions, range ->
        val (start, end) = getTimestampRange(range)
        val filtered = transactions.filter { 
            if (range == TimeRange.ALL) true else it.timestamp in start..end 
        }
        
        var income = 0.0
        var expense = 0.0
        filtered.forEach {
            if (it.type == "Received" || it.type == "Deposit") {
                income += it.amount
            } else {
                expense += it.amount
            }
        }
        DashboardStats(income, expense, income - expense, filtered.size)
    }.asLiveData()

    // Recent Transactions
    val recentTransactions: LiveData<List<TransactionEntity>> = combine(
        repository.allTransactions,
        timeRange
    ) { transactions, range ->
        val (start, end) = getTimestampRange(range)
        transactions
            .filter { if (range == TimeRange.ALL) true else it.timestamp in start..end }
            .take(5)
    }.asLiveData()

    val categorySpending: LiveData<List<CategorySpending>> = repository.categorySpending.asLiveData()

    val dailySpending: LiveData<List<ChartData>> = repository.allTransactions.map { transactions ->
        // Group by day (last 7 days)
        val today = Calendar.getInstance()
        val days = (0..6).map { 
            val cal = today.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -it)
            cal
        }.reversed()
        
        days.map { day ->
            val start = day.clone() as Calendar
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            
            val end = day.clone() as Calendar
            end.set(Calendar.HOUR_OF_DAY, 23)
            end.set(Calendar.MINUTE, 59)
            end.set(Calendar.SECOND, 59)
            
            val dayTotal = transactions.filter { 
                it.timestamp >= start.timeInMillis && it.timestamp <= end.timeInMillis && (it.type != "Received" && it.type != "Deposit")
            }.sumOf { it.amount }
            
            val label = SimpleDateFormat("EEE", Locale.getDefault()).format(day.time)
            ChartData(label, dayTotal)
        }
    }.asLiveData()

    private val _categories = MutableLiveData<List<CategoryEntity>>()
    val categories: LiveData<List<CategoryEntity>> = _categories

    // Analytics
    private val analyticsMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    private val analyticsYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

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
        analyticsYear,
        repository.allTransactions
    ) { month: Int, year: Int, transactions: List<TransactionEntity> ->
        val filtered = transactions.filter {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            (cal.get(Calendar.MONTH) + 1) == month && cal.get(Calendar.YEAR) == year
        }
        
        var income = 0.0
        var expense = 0.0
        filtered.forEach {
            if (it.type == "Received" || it.type == "Deposit") {
                income += it.amount
            } else {
                expense += it.amount
            }
        }
        DashboardStats(income, expense, income - expense, filtered.size)
    }.asLiveData()

    fun setAnalyticsFilter(isThisMonth: Boolean) {
        val cal = Calendar.getInstance()
        if (!isThisMonth) {
            cal.add(Calendar.MONTH, -1)
        }
        analyticsMonth.value = cal.get(Calendar.MONTH) + 1
        analyticsYear.value = cal.get(Calendar.YEAR)
    }

    fun setTimeRange(range: TimeRange) {
        timeRange.value = range
    }

    fun setFilterType(filter: TransactionFilter) {
        filterType.value = filter
    }

    fun search(query: String) {
        searchQuery.value = query
    }

    // State for Sync Status
    private val _syncStatus = MutableLiveData<SyncResult>()
    val syncStatus: LiveData<SyncResult> = _syncStatus

    fun syncSms() {
        viewModelScope.launch {
            _syncStatus.value = SyncResult.Loading
            try {
                val count = repository.syncTransactions()
                _syncStatus.value = SyncResult.Success(count)
            } catch (e: Exception) {
                _syncStatus.value = SyncResult.Error(e.message)
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = repository.getAllCategoriesList()
        }
    }

    fun updateTransactionCategory(transactionId: Int, categoryId: Int) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transactionId, categoryId)
        }
    }

    private fun getTimestampRange(range: TimeRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis
        when (range) {
            TimeRange.DAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimeRange.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimeRange.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimeRange.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimeRange.ALL -> return Pair(0L, Long.MAX_VALUE)
        }
        return Pair(calendar.timeInMillis, end)
    }
}

sealed class SyncResult {
    object Loading : SyncResult()
    data class Success(val count: Int) : SyncResult()
    data class Error(val message: String?) : SyncResult()
}

class DashboardViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
