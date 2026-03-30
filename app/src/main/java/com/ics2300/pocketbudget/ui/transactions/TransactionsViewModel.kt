package com.ics2300.pocketbudget.ui.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.ui.dashboard.TransactionFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val filterType = MutableStateFlow(TransactionFilter.ALL)
    private val dateRangeFilter = MutableStateFlow<Pair<Long, Long>?>(null)
    val minAmountFilter = MutableStateFlow<Double?>(null)
    val maxAmountFilter = MutableStateFlow<Double?>(null)
    val selectedActorFilter = MutableStateFlow<String?>(null)

    val allTransactions: LiveData<List<TransactionEntity>> = combine(
        combine(searchQuery, filterType, dateRangeFilter) { query, filter, dateRange ->
            Triple(query, filter, dateRange)
        },
        combine(minAmountFilter, maxAmountFilter, selectedActorFilter) { min, max, actor ->
            Triple(min, max, actor)
        }
    ) { basicFilters, advancedFilters ->
        val (query, filter, dateRange) = basicFilters
        val (minAmount, maxAmount, actor) = advancedFilters
        
        repository.getFilteredTransactions(
            query,
            dateRange?.first,
            dateRange?.second,
            minAmount,
            maxAmount,
            actor,
            filter.name
        )
    }.flatMapLatest { it }.asLiveData()

    fun search(query: String) {
        searchQuery.value = query
    }

    fun setFilterType(filter: TransactionFilter) {
        filterType.value = filter
    }

    fun setDateRangeFilter(start: Long, end: Long) {
        dateRangeFilter.value = Pair(start, end)
    }

    fun clearDateRangeFilter() {
        dateRangeFilter.value = null
    }

    fun setAdvancedFilters(min: Double?, max: Double?, actor: String?) {
        minAmountFilter.value = min
        maxAmountFilter.value = max
        selectedActorFilter.value = actor
    }

    fun clearAdvancedFilters() {
        minAmountFilter.value = null
        maxAmountFilter.value = null
        selectedActorFilter.value = null
    }

    fun updateTransactionCategory(transactionId: Int, categoryId: Int) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transactionId, categoryId)
        }
    }
}

class TransactionsViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
