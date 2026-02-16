package com.ics2300.pocketbudget.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.CategoryBudgetProgress
import com.ics2300.pocketbudget.data.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _currentMonthYear = MutableStateFlow(getCurrentMonthYear())

    val budgetProgress = _currentMonthYear.flatMapLatest { (month, year) ->
        repository.getBudgetProgress(month, year)
    }.asLiveData()

    fun setBudget(categoryId: Int, amount: Double) {
        viewModelScope.launch {
            val (month, year) = _currentMonthYear.value
            repository.setBudget(categoryId, amount, month, year)
        }
    }

    private fun getCurrentMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        // Month is 0-indexed in Calendar, but 1-indexed in our DB logic usually (or stick to 0-11 if consistent)
        // Let's use 1-12 for SQL printf('%02d') compatibility
        return Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
    }
}

class BudgetViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
