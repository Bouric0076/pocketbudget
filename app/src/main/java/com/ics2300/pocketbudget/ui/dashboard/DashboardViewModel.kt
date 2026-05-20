package com.ics2300.pocketbudget.ui.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.ActorSpending
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.DashboardStats
import com.ics2300.pocketbudget.data.RecurringTransactionEntity
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.domain.usecase.SyncTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

private const val TAG = "DashboardViewModel"

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

    val dashboardStats: LiveData<DashboardStats> =
        timeRange.flatMapLatest { range ->
            val (start, end) = getTimestampRange(range)

            repository.getDashboardStats(
                if (range == TimeRange.ALL) null else start,
                if (range == TimeRange.ALL) null else end
            )
        }.asLiveData()

    val recentTransactions: LiveData<List<TransactionEntity>> =
        timeRange.flatMapLatest { range ->
            val (start, end) = getTimestampRange(range)

            repository.getFilteredTransactions(
                null,
                if (range == TimeRange.ALL) null else start,
                if (range == TimeRange.ALL) null else end,
                null,
                null,
                null,
                "ALL"
            ).map { transactions ->
                transactions.take(5)
            }
        }.asLiveData()

    val topSpendingActors: LiveData<List<ActorSpending>> =
        timeRange.flatMapLatest { range ->
            val (start, end) = getTimestampRange(range)

            repository.getTopSpendingActorsByDate(
                limit = 5,
                start = start,
                end = end
            )
        }.asLiveData()

    val categories: LiveData<List<CategoryEntity>> =
        repository.allCategories.asLiveData()

    private val _syncStatus = MutableLiveData<SyncResult>()
    val syncStatus: LiveData<SyncResult> = _syncStatus

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.insert(transaction)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add transaction.", e)
            }
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
            try {
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

                withContext(Dispatchers.IO) {
                    repository.addRecurringTransaction(recurring)
                    repository.processDueRecurringTransactions()
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add recurring transaction.", e)
            }
        }
    }

    fun setTimeRange(range: TimeRange) {
        timeRange.value = range
    }

    fun updateTransactionCategory(transactionId: Int, categoryId: Int) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateTransactionCategory(transactionId, categoryId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update transaction category.", e)
            }
        }
    }

    fun bulkCategorizeSimilarTransactions(
        partyName: String,
        categoryId: Int
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.bulkCategorizeSimilarTransactions(
                        partyName,
                        categoryId
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed bulk categorization.", e)
            }
        }
    }

    fun syncSms() {
        viewModelScope.launch {
            _syncStatus.postValue(SyncResult.Loading)

            try {
                val count = withContext(Dispatchers.IO) {
                    syncTransactionsUseCase()
                }

                _syncStatus.postValue(SyncResult.Success(count))

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Log.e(TAG, "SMS sync failed.", e)

                _syncStatus.postValue(
                    SyncResult.Error(
                        e.message ?: "Unknown sync error"
                    )
                )
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

            TimeRange.ALL -> {
                return Pair(0L, Long.MAX_VALUE)
            }
        }

        return Pair(calendar.timeInMillis, end)
    }
}

sealed class SyncResult {
    object Loading : SyncResult()
    data class Success(val count: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}