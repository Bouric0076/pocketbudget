package com.ics2300.pocketbudget.utils

import android.content.Context
import android.net.Uri
import com.ics2300.pocketbudget.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

object CsvImporter {

    // Simple CSV parser that handles standard CSV format
    // Assumes header: ID,Date,Amount,Type,Party,Category
    // ID can be ignored/regenerated if we want, or used for deduplication. 
    // Here we will use the ID from CSV if present, or generate if missing (though export has it).
    
    data class ImportResult(val successCount: Int, val failedCount: Int, val errors: List<String>)

    suspend fun importTransactions(context: Context, uri: Uri): Result<List<TransactionEntity>> {
        return withContext(Dispatchers.IO) {
            val transactions = mutableListOf<TransactionEntity>()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line = reader.readLine() // Read Header
                        // Validate header if needed, for now skip
                        
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        
                        while (reader.readLine().also { line = it } != null) {
                            if (line.isNullOrBlank()) continue
                            
                            try {
                                val tokens = parseCsvLine(line!!)
                                if (tokens.size >= 5) {
                                    // ID, Date, Amount, Type, Party, Category
                                    val id = tokens[0].trim()
                                    val dateStr = tokens[1].trim()
                                    val amount = tokens[2].trim().toDoubleOrNull() ?: 0.0
                                    val type = tokens[3].trim()
                                    val party = tokens[4].trim()
                                    val categoryId = if (tokens.size > 5) tokens[5].trim().toIntOrNull() else null

                                    val timestamp = try {
                                        dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    }

                                    // If ID is empty (e.g. from manual CSV), generate one
                                    val finalId = if (id.isNotEmpty()) id else "IMP_${System.currentTimeMillis()}_${(0..1000).random()}"

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
                                // Skip malformed lines
                                e.printStackTrace()
                            }
                        }
                    }
                }
                Result.success(transactions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
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
