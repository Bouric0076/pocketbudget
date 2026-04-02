package com.ics2300.pocketbudget.utils

import android.text.format.DateUtils
import com.ics2300.pocketbudget.data.TransactionWithCategory
import com.ics2300.pocketbudget.ui.TransactionListItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TransactionGrouper {

    fun groupTransactions(transactions: List<TransactionWithCategory>): List<TransactionListItem> {
        val groupedList = mutableListOf<TransactionListItem>()
        val sortedTransactions = transactions.sortedByDescending { it.transaction.timestamp }

        var lastHeader = ""

        for (transaction in sortedTransactions) {
            val header = getHeaderForTimestamp(transaction.transaction.timestamp)
            if (header != lastHeader) {
                groupedList.add(TransactionListItem.Header(header))
                lastHeader = header
            }
            groupedList.add(TransactionListItem.Transaction(transaction.transaction, transaction.category))
        }

        return groupedList
    }

    private fun getHeaderForTimestamp(timestamp: Long): String {
        val now = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            DateUtils.isToday(timestamp) -> "Today"
            isYesterday(now, date) -> "Yesterday"
            isSameWeek(now, date) -> "This Week"
            isSameMonth(now, date) -> "This Month"
            isSameYear(now, date) -> SimpleDateFormat("MMMM", Locale.getDefault()).format(date.time)
            else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date.time)
        }
    }

    private fun isYesterday(now: Calendar, date: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return yesterday.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(now: Calendar, date: Calendar): Boolean {
        return now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == date.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(now: Calendar, date: Calendar): Boolean {
        return now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == date.get(Calendar.MONTH)
    }
    
    private fun isSameYear(now: Calendar, date: Calendar): Boolean {
        return now.get(Calendar.YEAR) == date.get(Calendar.YEAR)
    }
}
