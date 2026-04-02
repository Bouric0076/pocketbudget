package com.ics2300.pocketbudget.ui

import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.TransactionEntity

sealed class TransactionListItem {
    data class Transaction(val transaction: TransactionEntity, val category: CategoryEntity?) : TransactionListItem()
    data class Header(val title: String) : TransactionListItem()
}