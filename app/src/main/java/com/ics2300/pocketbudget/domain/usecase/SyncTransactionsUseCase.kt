package com.ics2300.pocketbudget.domain.usecase

import com.ics2300.pocketbudget.data.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SyncTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(): Int {
        return withContext(Dispatchers.IO) {
            repository.syncTransactions()
        }
    }
}