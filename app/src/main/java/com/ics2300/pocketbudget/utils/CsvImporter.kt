package com.ics2300.pocketbudget.utils

import android.content.Context
import android.net.Uri
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

object CsvImporter {

    data class ImportResult(val successCount: Int, val failedCount: Int, val errors: List<String>)

    internal fun parseTransactions(
        lines: Sequence<String>,
        categories: List<CategoryEntity> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): List<TransactionEntity> {
        val transactions = mutableListOf<TransactionEntity>()
        val iterator = lines.iterator()

        if (!iterator.hasNext()) {
            return transactions
        }

        val header = parseCsvLine(iterator.next())
            .map { token -> normalizeHeader(token) }
        val columns = CsvColumns.fromHeader(header)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val categoryIdByName =
            categories.associateBy { normalizeCategoryName(it.name) }

        while (iterator.hasNext()) {
            val line = iterator.next()
            if (line.isBlank()) continue

            try {
                val tokens = parseCsvLine(line)
                if (tokens.size >= 5) {
                    val id = tokens.getOrBlank(columns.id)
                    val dateStr = tokens.getOrBlank(columns.date)
                    val amount = tokens.getOrBlank(columns.amount).toDoubleOrNull() ?: 0.0
                    val type = tokens.getOrBlank(columns.type)
                    val party = tokens.getOrBlank(columns.party)
                    val categoryName = tokens.getOrBlank(columns.categoryName)
                    val legacyCategoryId =
                        tokens.getOrBlank(columns.categoryId).toIntOrNull()
                    val categoryId =
                        categoryIdByName[normalizeCategoryName(categoryName)]?.id
                            ?: legacyCategoryId?.takeIf { id ->
                                categories.any { it.id == id }
                            }

                    val timestamp = try {
                        dateFormat.parse(dateStr)?.time ?: nowMillis
                    } catch (e: Exception) {
                        nowMillis
                    }

                    val finalId = if (id.isNotEmpty()) {
                        id
                    } else {
                        "IMP_${nowMillis}_${transactions.size}"
                    }

                    transactions.add(
                        TransactionEntity(
                            transactionId = finalId,
                            amount = amount,
                            type = type,
                            partyName = party,
                            timestamp = timestamp,
                            categoryId = categoryId
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return transactions
    }

    suspend fun importTransactions(
        context: Context,
        uri: Uri,
        categories: List<CategoryEntity> = emptyList()
    ): Result<List<TransactionEntity>> {
        return withContext(Dispatchers.IO) {
            val transactions = mutableListOf<TransactionEntity>()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        transactions.addAll(
                            parseTransactions(
                                reader.lineSequence(),
                                categories
                            )
                        )
                    }
                }
                Result.success(transactions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private data class CsvColumns(
        val id: Int = 0,
        val date: Int = 1,
        val amount: Int = 2,
        val type: Int = 3,
        val party: Int = 4,
        val categoryId: Int = 5,
        val categoryName: Int = -1
    ) {
        companion object {
            fun fromHeader(header: List<String>): CsvColumns {
                if (header.isEmpty()) {
                    return CsvColumns()
                }

                fun indexOf(name: String): Int {
                    return header.indexOf(name)
                }

                return CsvColumns(
                    id = indexOf("id").takeIfFound(default = 0),
                    date = indexOf("date").takeIfFound(default = 1),
                    amount = indexOf("amount").takeIfFound(default = 2),
                    type = indexOf("type").takeIfFound(default = 3),
                    party = indexOf("party").takeIfFound(default = 4),
                    categoryId = listOf(
                        indexOf("categoryid"),
                        indexOf("category")
                    ).firstOrNull { it >= 0 } ?: 5,
                    categoryName = indexOf("categoryname")
                )
            }
        }
    }

    private fun Int.takeIfFound(default: Int): Int {
        return if (this >= 0) this else default
    }

    private fun List<String>.getOrBlank(index: Int): String {
        return if (index in indices) this[index].trim() else ""
    }

    private fun normalizeHeader(value: String): String {
        return value
            .trim()
            .replace("_", "")
            .replace(" ", "")
            .lowercase(Locale.US)
    }

    private fun normalizeCategoryName(value: String): String {
        return value.trim().uppercase(Locale.US)
    }

    internal fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var sb = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    tokens.add(sb.toString())
                    sb = StringBuilder()
                }
                else -> sb.append(char)
            }
        }
        tokens.add(sb.toString())
        
        // Handle escaped quotes
        return tokens.map { 
             var token = it
             if (token.startsWith("\"") && token.endsWith("\"")) {
                 token = token.substring(1, token.length - 1)
             }
             token.replace("\"\"", "\"")
        }
    }
}
