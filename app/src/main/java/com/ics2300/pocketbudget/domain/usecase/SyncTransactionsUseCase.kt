package com.ics2300.pocketbudget.domain.usecase

import com.ics2300.pocketbudget.data.TransactionRepository
import javax.inject.Inject

class SyncTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(): Int {
        return repository.syncTransactions()
    }
}
