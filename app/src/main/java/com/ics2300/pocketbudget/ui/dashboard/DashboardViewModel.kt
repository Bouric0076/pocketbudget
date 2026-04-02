package com.ics2300.pocketbudget.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.ActorSpending
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.CategorySpending
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.data.DashboardStats
import com.ics2300.pocketbudget.data.RecurringTransactionEntity
import com.ics2300.pocketbudget.ui.ChartData
import com.ics2300.pocketbudget.utils.AnalyticsUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.ics2300.pocketbudget.domain.usecase.SyncTransactionsUseCase

@OptIn(ExperimentalCoroutinesApi::class)
enum class TimeRange {
    DAY, WEEK, MONTH, YEAR, ALL
}

enum class TransactionFilter {
    ALL, INCOME, EXPENSE, UNCATEGORIZED
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val syncTransactionsUseCase: SyncTransactionsUseCase
) : ViewModel() {

    private val timeRange = MutableStateFlow(TimeRange.DAY)

    // For Dashboard Stats (Filtered by TimeRange)
    val dashboardStats: LiveData<DashboardStats> = timeRange.flatMapLatest { range ->
        val (start, end) = getTimestampRange(range)
        repository.getDashboardStats(
            if (range == TimeRange.ALL) null else start,
            if (range == TimeRange.ALL) null else end
        )
    }.asLiveData()

    // Recent Transactions
    val recentTransactions: LiveData<List<TransactionEntity>> = timeRange.flatMapLatest { range ->
        val (start, end) = getTimestampRange(range)
        repository.getFilteredTransactions(
            null,
            if (range == TimeRange.ALL) null else start,
            if (range == TimeRange.ALL) null else end,
            null,
            null,
            null,
            "ALL"
        ).map { it.take(5) }
    }.asLiveData()

    val topSpendingActors: LiveData<List<ActorSpending>> = timeRange.flatMapLatest { range ->
        val (start, end) = getTimestampRange(range)
        repository.getTopSpendingActorsByDate(5, start, end)
    }.asLiveData()

    val categories: LiveData<List<CategoryEntity>> = repository.allCategories.asLiveData()

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
                nextDueDate = startDate,
                isActive = true
            )
            repository.addRecurringTransaction(recurring)
            repository.processDueRecurringTransactions()
        }
    }

    fun setTimeRange(range: TimeRange) {
        timeRange.value = range
    }

    fun updateTransactionCategory(transactionId: Int, categoryId: Int) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transactionId, categoryId)
        }
    }

    fun bulkCategorizeSimilarTransactions(partyName: String, categoryId: Int) {
        viewModelScope.launch {
            repository.bulkCategorizeSimilarTransactions(partyName, categoryId)
        }
    }

    // State for Sync Status
    private val _syncStatus = MutableLiveData<SyncResult>()
    val syncStatus: LiveData<SyncResult> = _syncStatus

    fun syncSms() {
        viewModelScope.launch {
            _syncStatus.value = SyncResult.Loading
            try {
                val count = syncTransactionsUseCase()
                _syncStatus.value = SyncResult.Success(count)
            } catch (e: Exception) {
                _syncStatus.value = SyncResult.Error(e.message)
            }
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
