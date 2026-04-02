package com.ics2300.pocketbudget.ui.budget

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.CategoryBudgetProgress
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(private val repository: TransactionRepository) : ViewModel() {

    private val currentCalendar = Calendar.getInstance()
    val currentMonth = MutableStateFlow(currentCalendar.get(Calendar.MONTH) + 1) // 1-12
    val currentYear = MutableStateFlow(currentCalendar.get(Calendar.YEAR))

    val budgetProgress: LiveData<List<CategoryBudgetProgress>> = combine(
        currentMonth,
        currentYear
    ) { month, year ->
        repository.getBudgetProgress(month, year)
    }.flatMapLatest { it }.asLiveData()

    val categories: LiveData<List<CategoryEntity>> = repository.allCategories.asLiveData()

    fun setBudget(categoryId: Int, amount: Double) {
        viewModelScope.launch {
            repository.setBudget(categoryId, amount, currentMonth.value, currentYear.value)
        }
    }
}


