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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class SortType {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, ALPHABETICAL
}

private data class QueryParams(
    val query: String?,
    val filterType: String,
    val startDate: Long?,
    val endDate: Long?,
    val minAmount: Double?,
    val maxAmount: Double?,
    val actor: String?,
    val sortType: String
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(private val repository: TransactionRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val filterType = MutableStateFlow(TransactionFilter.ALL)
    private val dateRangeFilter = MutableStateFlow<Pair<Long, Long>?>(null)
    val minAmountFilter = MutableStateFlow<Double?>(null)
    val maxAmountFilter = MutableStateFlow<Double?>(null)
    val selectedActorFilter = MutableStateFlow<String?>(null)
    private val sortType = MutableStateFlow(SortType.DATE_DESC)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactionsWithCategory: LiveData<List<com.ics2300.pocketbudget.data.TransactionWithCategory>> = combine(
        searchQuery,
        filterType,
        dateRangeFilter,
        minAmountFilter,
        maxAmountFilter,
        selectedActorFilter,
        sortType
    ) { array ->
        val query = array[0] as String
        val filter = array[1] as TransactionFilter
        @Suppress("UNCHECKED_CAST")
        val dateRange = array[2] as Pair<Long, Long>?
        val min = array[3] as Double?
        val max = array[4] as Double?
        val actor = array[5] as String?
        val sort = array[6] as SortType

        QueryParams(
            query = if (query.isBlank()) null else query,
            filterType = filter.name,
            startDate = dateRange?.first,
            endDate = dateRange?.second,
            minAmount = min,
            maxAmount = max,
            actor = actor,
            sortType = sort.name
        )
    }.flatMapLatest { params ->
        repository.getFilteredTransactionsWithCategory(
            query = params.query,
            filterType = params.filterType,
            startDate = params.startDate,
            endDate = params.endDate,
            minAmount = params.minAmount,
            maxAmount = params.maxAmount,
            actor = params.actor
        ).map { list ->
            val sort = SortType.valueOf(params.sortType)
            when (sort) {
                SortType.DATE_DESC -> list.sortedByDescending { it.transaction.timestamp }
                SortType.DATE_ASC -> list.sortedBy { it.transaction.timestamp }
                SortType.AMOUNT_DESC -> list.sortedByDescending { Math.abs(it.transaction.amount) }
                SortType.AMOUNT_ASC -> list.sortedBy { Math.abs(it.transaction.amount) }
                SortType.ALPHABETICAL -> list.sortedBy { it.transaction.partyName }
            }
        }
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
