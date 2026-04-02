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
import com.ics2300.pocketbudget.data.TransactionWithCategory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class SortType {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, ALPHABETICAL
}

data class Filters(val min: Double?, val max: Double?, val actor: String?, val sort: SortType)

@HiltViewModel
class TransactionsViewModel @Inject constructor(private val repository: TransactionRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val filterType = MutableStateFlow(TransactionFilter.ALL)
    private val dateRangeFilter = MutableStateFlow<Pair<Long, Long>?>(null)
    val minAmountFilter = MutableStateFlow<Double?>(null)
    val maxAmountFilter = MutableStateFlow<Double?>(null)
    val selectedActorFilter = MutableStateFlow<String?>(null)
    private val sortType = MutableStateFlow(SortType.DATE_DESC)

    val transactionsWithCategory: LiveData<List<com.ics2300.pocketbudget.data.TransactionWithCategory>> = combine(
        repository.transactionsWithCategory,
        searchQuery,
        filterType,
        dateRangeFilter,
        combine(minAmountFilter, maxAmountFilter, selectedActorFilter, sortType) { min, max, actor, sort ->
            Filters(min, max, actor, sort)
        }
    ) { transactions, query, filter, dateRange, filters ->
        val (min, max, actor, sort) = filters
        var filteredList = transactions

        if (query.isNotBlank()) {
            filteredList = filteredList.filter {
                it.transaction.partyName.contains(query, ignoreCase = true) ||
                        (it.transaction.fullSmsBody?.contains(query, ignoreCase = true) == true) ||
                        (it.category?.name?.contains(query, ignoreCase = true) == true)
            }
        }

        filteredList = when (filter) {
            TransactionFilter.INCOME -> filteredList.filter { it.transaction.amount > 0 }
            TransactionFilter.EXPENSE -> filteredList.filter { it.transaction.amount < 0 }
            TransactionFilter.UNCATEGORIZED -> filteredList.filter { it.category?.name?.equals("Uncategorized", ignoreCase = true) == true || it.transaction.categoryId == null }
            else -> filteredList
        }

        dateRange?.let { (start, end) ->
            filteredList = filteredList.filter { it.transaction.timestamp in start..end }
        }

        min?.let { minValue ->
            filteredList = filteredList.filter { Math.abs(it.transaction.amount) >= minValue }
        }

        max?.let { maxValue ->
            filteredList = filteredList.filter { Math.abs(it.transaction.amount) <= maxValue }
        }

        actor?.let { actorValue ->
            filteredList = filteredList.filter { it.transaction.partyName == actorValue }
        }

        // Apply sorting
        filteredList = when (sort) {
            SortType.DATE_DESC -> filteredList.sortedByDescending { it.transaction.timestamp }
            SortType.DATE_ASC -> filteredList.sortedBy { it.transaction.timestamp }
            SortType.AMOUNT_DESC -> filteredList.sortedByDescending { Math.abs(it.transaction.amount) }
            SortType.AMOUNT_ASC -> filteredList.sortedBy { Math.abs(it.transaction.amount) }
            SortType.ALPHABETICAL -> filteredList.sortedBy { it.transaction.partyName }
        }

        filteredList
    }.asLiveData()

    fun setSortType(sort: SortType) {
        sortType.value = sort
    }

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

    fun bulkCategorizeSimilarTransactions(partyName: String, categoryId: Int) {
        viewModelScope.launch {
            repository.bulkCategorizeSimilarTransactions(partyName, categoryId)
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
