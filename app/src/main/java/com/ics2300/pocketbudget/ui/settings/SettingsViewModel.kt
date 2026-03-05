package com.ics2300.pocketbudget.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.TransactionRepository
import kotlinx.coroutines.launch

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.MutableLiveData
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.ui.dashboard.SyncResult

import android.net.Uri
import android.content.Context
import com.ics2300.pocketbudget.utils.CsvExporter
import com.ics2300.pocketbudget.utils.PdfExporter
import com.ics2300.pocketbudget.utils.CsvImporter
import kotlinx.coroutines.flow.first

class SettingsViewModel(private val repository: TransactionRepository) : ViewModel() {

    val categories: LiveData<List<CategoryEntity>> = repository.getAllCategories().asLiveData()

    private val _syncStatus = MutableLiveData<SyncResult>()
    val syncStatus: LiveData<SyncResult> = _syncStatus

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                // Get all transactions (this is a Flow, so we take the first emission)
                val transactions = repository.allTransactions.first()
                val categories = repository.getAllCategoriesList()
                
                // If the URI ends in .csv, export CSV, otherwise PDF
                // Or since we create the intent, we know what we want.
                // For now, let's just use the new PDF exporter if requested or separate methods.
                // The user asked to "update/enhance", so let's default to PDF but maybe check file extension or mime type if possible, 
                // but simpler to just add a new method.
                
                // For backward compatibility or clarity, let's just make this export CSV for now 
                // and add exportPdf separately, then call the right one from UI.
                val result = CsvExporter.exportTransactions(context, uri, transactions)
                if (result.isSuccess) {
                     android.widget.Toast.makeText(context, "CSV Export Successful", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                     android.widget.Toast.makeText(context, "Export Failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                 android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportPdf(context: Context, uri: Uri, startDate: Long? = null, endDate: Long? = null) {
        viewModelScope.launch {
            try {
                var transactions = repository.allTransactions.first()
                
                // Filter if dates are provided
                var reportPeriod = "All Time"
                if (startDate != null && endDate != null) {
                    transactions = transactions.filter { it.timestamp in startDate..endDate }
                    
                    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    reportPeriod = "${dateFormat.format(java.util.Date(startDate))} - ${dateFormat.format(java.util.Date(endDate))}"
                }

                val categories = repository.getAllCategoriesList()
                val result = PdfExporter.exportTransactionsToPdf(context, uri, transactions, categories, reportPeriod)
                
                if (result.isSuccess) {
                     android.widget.Toast.makeText(context, "PDF Export Successful", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                     android.widget.Toast.makeText(context, "Export Failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                 android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _syncStatus.value = SyncResult.Loading
            try {
                val result = CsvImporter.importTransactions(context, uri)
                if (result.isSuccess) {
                    val transactions = result.getOrNull() ?: emptyList()
                    if (transactions.isNotEmpty()) {
                        val importedCount = repository.importData(transactions)
                        _syncStatus.value = SyncResult.Success(importedCount)
                    } else {
                        _syncStatus.value = SyncResult.Error("No valid transactions found in file")
                    }
                } else {
                    _syncStatus.value = SyncResult.Error(result.exceptionOrNull()?.message ?: "Import failed")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncResult.Error(e.message)
            }
        }
    }

    fun addCategory(name: String, keywords: String, iconName: String = "ic_default", colorHex: String = "#0A3D2E") {
        viewModelScope.launch {
            repository.addCategory(CategoryEntity(name = name, keywords = keywords, iconName = iconName, colorHex = colorHex))
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
            _syncStatus.value = SyncResult.Loading
            try {
                val count = repository.syncTransactions()
                _syncStatus.value = SyncResult.Success(count)
            } catch (e: Exception) {
                _syncStatus.value = SyncResult.Error(e.message)
            }
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
