package com.ics2300.pocketbudget.utils

import android.content.Context
import android.net.Uri
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    suspend fun exportTransactions(
        context: Context,
        uri: Uri,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity> = emptyList()
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri) ?: throw Exception("Could not open output stream for URI: $uri")

                outputStream.use { stream ->
                    BufferedWriter(OutputStreamWriter(stream)).use { writer ->
                        // Write Header
                        writer.write("ID,Date,Amount,Type,Party,CategoryId,CategoryName\n")

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val categoryNamesById =
                            categories.associate { it.id to it.name }

                        // Write Rows
                        for (t in transactions) {
                            val categoryName =
                                t.categoryId?.let { categoryNamesById[it] }.orEmpty()

                            val line = StringBuilder()
                            // Escape transaction ID as it might contain commas
                            line.append("${escapeCsv(t.transactionId)},")
                            line.append("${dateFormat.format(Date(t.timestamp))},")
                            line.append("${t.amount},")
                            line.append("${escapeCsv(t.type)},")
                            line.append("${escapeCsv(t.partyName)},")
                            line.append("${t.categoryId ?: ""},")
                            line.append("${escapeCsv(categoryName)}\n")
                            
                            writer.write(line.toString())
                        }
                    }
                }
                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private fun escapeCsv(value: String): String {
        var result = value.replace("\"", "\"\"") // Escape double quotes
        if (result.contains(",") || result.contains("\n") || result.contains("\"")) {
            result = "\"$result\"" // Wrap in quotes if contains comma, newline or quotes
        }
        return result
    }
}
