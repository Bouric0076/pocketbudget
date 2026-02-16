package com.ics2300.pocketbudget.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.TransactionRepository
import kotlinx.coroutines.launch

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.ics2300.pocketbudget.data.CategoryEntity

import android.net.Uri
import android.content.Context
import com.ics2300.pocketbudget.utils.CsvExporter
import kotlinx.coroutines.flow.first

class SettingsViewModel(private val repository: TransactionRepository) : ViewModel() {

    val categories: LiveData<List<CategoryEntity>> = repository.getAllCategories().asLiveData()

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                // Get all transactions (this is a Flow, so we take the first emission)
                val transactions = repository.allTransactions.first()
                val result = CsvExporter.exportTransactions(context, uri, transactions)
                if (result.isSuccess) {
                     android.widget.Toast.makeText(context, "Export Successful", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                     android.widget.Toast.makeText(context, "Export Failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                 android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addCategory(name: String, keywords: String) {
        viewModelScope.launch {
            repository.addCategory(CategoryEntity(name = name, keywords = keywords))
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun resyncData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.syncTransactions()
        }
    }
    
    fun processMpesaSms(body: String, onResult: (com.ics2300.pocketbudget.data.TransactionEntity?) -> Unit) {
        viewModelScope.launch {
            val transaction = repository.processNewSms(body)
            onResult(transaction)
        }
    }
}

class SettingsViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
