package com.ics2300.pocketbudget.utils

import android.content.Context
import android.net.Uri
import com.ics2300.pocketbudget.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    suspend fun exportTransactions(context: Context, uri: Uri, transactions: List<TransactionEntity>): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        // Write Header
                        writer.write("ID,Date,Amount,Type,Party,Category\n")

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                        // Write Rows
                        for (t in transactions) {
                            val line = buildString {
                                append("${t.transactionId},")
                                append("${dateFormat.format(Date(t.timestamp))},")
                                append("${t.amount},")
                                append("${escapeCsv(t.type)},")
                                append("${escapeCsv(t.partyName)},")
                                append("${t.categoryId ?: ""}\n")
                            }
                            writer.write(line)
                        }
                    }
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun escapeCsv(value: String): String {
        var result = value.replace("\"", "\"\"") // Escape double quotes
        if (result.contains(",") || result.contains("\n")) {
            result = "\"$result\"" // Wrap in quotes if contains comma
        }
        return result
    }
}