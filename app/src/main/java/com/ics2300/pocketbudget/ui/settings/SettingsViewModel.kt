package com.ics2300.pocketbudget.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.ui.dashboard.SyncResult
import com.ics2300.pocketbudget.utils.CsvExporter
import com.ics2300.pocketbudget.utils.CsvImporter
import com.ics2300.pocketbudget.utils.PdfExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    val categories: LiveData<List<CategoryEntity>> =
        repository.allCategories.asLiveData()

    private val _syncStatus = MutableLiveData<SyncResult>()
    val syncStatus: LiveData<SyncResult> = _syncStatus

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val transactions = repository.allTransactions.first()
                val result = CsvExporter.exportTransactions(context, uri, transactions)

                if (result.isSuccess) {
                    Toast.makeText(context, "CSV Export Successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "Export Failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun exportPdf(
        context: Context,
        uri: Uri,
        startDate: Long? = null,
        endDate: Long? = null
    ) {
        viewModelScope.launch {
            try {
                var transactions = repository.allTransactions.first()

                var reportPeriod = "All Time"
                if (startDate != null && endDate != null) {
                    transactions = transactions.filter { it.timestamp in startDate..endDate }

                    val dateFormat = java.text.SimpleDateFormat(
                        "MMM dd, yyyy",
                        java.util.Locale.getDefault()
                    )

                    reportPeriod =
                        "${dateFormat.format(java.util.Date(startDate))} - ${dateFormat.format(java.util.Date(endDate))}"
                }

                val categories = repository.getAllCategoriesList()

                val result = PdfExporter.exportTransactionsToPdf(
                    context,
                    uri,
                    transactions,
                    categories,
                    reportPeriod
                )

                if (result.isSuccess) {
                    Toast.makeText(context, "PDF Export Successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "Export Failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _syncStatus.value = SyncResult.Loading

            try {
                val result = CsvImporter.importTransactions(context, uri)

                if (result.isSuccess) {
                    val transactions = result.getOrNull().orEmpty()

                    if (transactions.isNotEmpty()) {
                        val importedCount = repository.importData(transactions)
                        _syncStatus.value = SyncResult.Success(importedCount)
                    } else {
                        _syncStatus.value =
                            SyncResult.Error("No valid transactions found in file")
                    }
                } else {
                    _syncStatus.value =
                        SyncResult.Error(result.exceptionOrNull()?.message ?: "Import failed")
                }
            } catch (e: Exception) {
                _syncStatus.value =
                    SyncResult.Error(e.message ?: "Unknown import error")
            }
        }
    }

    fun addCategory(
        name: String,
        keywords: String,
        iconName: String = "ic_default",
        colorHex: String = "#0A3D2E"
    ) {
        viewModelScope.launch {
            repository.addCategory(
                CategoryEntity(
                    name = name,
                    keywords = keywords,
                    iconName = iconName,
                    colorHex = colorHex
                )
            )
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
                val count = repository.resyncAllTransactions()
                _syncStatus.value = SyncResult.Success(count)
            } catch (e: Exception) {
                _syncStatus.value =
                    SyncResult.Error(e.message ?: "Unknown sync error")
            }
        }
    }

    fun processMpesaSms(
        body: String,
        onResult: (com.ics2300.pocketbudget.data.TransactionEntity?) -> Unit
    ) {
        viewModelScope.launch {
            val transaction = repository.processNewSms(body)
            onResult(transaction)
        }
    }
}

class SettingsViewModelFactory(
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}