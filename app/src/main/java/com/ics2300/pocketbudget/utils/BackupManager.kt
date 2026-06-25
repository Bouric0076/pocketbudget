package com.ics2300.pocketbudget.utils

import android.content.Context
import android.net.Uri
import com.ics2300.pocketbudget.data.AppDatabase
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.BudgetEntity
import com.ics2300.pocketbudget.data.RecurringTransactionEntity
import com.ics2300.pocketbudget.data.ActorCategoryMapping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.room.withTransaction

object BackupManager {

    suspend fun exportBackup(
        context: Context,
        uri: Uri,
        database: AppDatabase,
        password: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val dao = database.transactionDao()
                
                val transactions = dao.getAllTransactionsList()
                val categories = dao.getAllCategoriesList()
                val budgets = dao.getAllBudgetsList()
                val recurring = dao.getAllRecurringTransactionsList()
                val mappings = dao.getAllActorMappings()
                
                val root = JSONObject()
                root.put("version", 1)
                root.put("timestamp", System.currentTimeMillis())
                
                // Transactions
                val txArray = JSONArray()
                for (t in transactions) {
                    val o = JSONObject()
                    o.put("transactionId", t.transactionId)
                    o.put("amount", t.amount)
                    o.put("type", t.type)
                    o.put("partyName", t.partyName)
                    o.put("timestamp", t.timestamp)
                    o.put("categoryId", t.categoryId ?: JSONObject.NULL)
                    o.put("accountName", t.accountName ?: JSONObject.NULL)
                    o.put("balanceAfter", t.balanceAfter ?: JSONObject.NULL)
                    o.put("transactionCost", t.transactionCost ?: JSONObject.NULL)
                    o.put("fullSmsBody", t.fullSmsBody ?: JSONObject.NULL)
                    txArray.put(o)
                }
                root.put("transactions", txArray)
                
                // Categories
                val catArray = JSONArray()
                for (c in categories) {
                    val o = JSONObject()
                    o.put("id", c.id)
                    o.put("name", c.name)
                    o.put("keywords", c.keywords)
                    o.put("iconName", c.iconName)
                    o.put("colorHex", c.colorHex)
                    catArray.put(o)
                }
                root.put("categories", catArray)
                
                // Budgets
                val budgetArray = JSONArray()
                for (b in budgets) {
                    val o = JSONObject()
                    o.put("amount", b.amount)
                    o.put("month", b.month)
                    o.put("year", b.year)
                    o.put("categoryId", b.categoryId ?: JSONObject.NULL)
                    budgetArray.put(o)
                }
                root.put("budgets", budgetArray)
                
                // Recurring Transactions
                val recArray = JSONArray()
                for (r in recurring) {
                    val o = JSONObject()
                    o.put("amount", r.amount)
                    o.put("description", r.description)
                    o.put("categoryId", r.categoryId)
                    o.put("type", r.type)
                    o.put("frequency", r.frequency)
                    o.put("startDate", r.startDate)
                    o.put("nextDueDate", r.nextDueDate)
                    o.put("isActive", r.isActive)
                    recArray.put(o)
                }
                root.put("recurring_transactions", recArray)
                
                // Actor Category Mappings
                val mapArray = JSONArray()
                for (m in mappings) {
                    val o = JSONObject()
                    o.put("partyName", m.partyName)
                    o.put("categoryId", m.categoryId)
                    mapArray.put(o)
                }
                root.put("actor_category_mappings", mapArray)
                
                val jsonString = root.toString()
                val encryptedBytes = EncryptionUtils.encrypt(
                    jsonString.toByteArray(Charsets.UTF_8),
                    password.toCharArray()
                )
                
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw Exception("Could not open output stream for URI: $uri")
                outputStream.use { stream ->
                    stream.write(encryptedBytes)
                }
                
                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun importBackup(
        context: Context,
        uri: Uri,
        database: AppDatabase,
        password: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Could not open input stream for URI: $uri")
                val encryptedBytes = inputStream.use { it.readBytes() }
                
                val decryptedBytes = EncryptionUtils.decrypt(
                    encryptedBytes,
                    password.toCharArray()
                )
                
                val jsonString = String(decryptedBytes, Charsets.UTF_8)
                val root = JSONObject(jsonString)
                
                val txList = mutableListOf<TransactionEntity>()
                val catList = mutableListOf<CategoryEntity>()
                val budgetList = mutableListOf<BudgetEntity>()
                val recList = mutableListOf<RecurringTransactionEntity>()
                val mapList = mutableListOf<ActorCategoryMapping>()
                
                // 1. Parse Categories
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val o = catArray.getJSONObject(i)
                    catList.add(
                        CategoryEntity(
                            id = o.getInt("id"),
                            name = o.getString("name"),
                            keywords = o.getString("keywords"),
                            iconName = o.getString("iconName"),
                            colorHex = o.getString("colorHex")
                        )
                    )
                }
                
                // 2. Parse Transactions
                val txArray = root.getJSONArray("transactions")
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    txList.add(
                        TransactionEntity(
                            transactionId = o.getString("transactionId"),
                            amount = o.getDouble("amount"),
                            type = o.getString("type"),
                            partyName = o.getString("partyName"),
                            timestamp = o.getLong("timestamp"),
                            categoryId = if (o.isNull("categoryId")) null else o.getInt("categoryId"),
                            accountName = if (o.isNull("accountName")) null else o.getString("accountName"),
                            balanceAfter = if (o.isNull("balanceAfter")) null else o.getDouble("balanceAfter"),
                            transactionCost = if (o.isNull("transactionCost")) null else o.getDouble("transactionCost"),
                            fullSmsBody = if (o.isNull("fullSmsBody")) null else o.getString("fullSmsBody")
                        )
                    )
                }
                
                // 3. Parse Budgets
                val budgetArray = root.getJSONArray("budgets")
                for (i in 0 until budgetArray.length()) {
                    val o = budgetArray.getJSONObject(i)
                    budgetList.add(
                        BudgetEntity(
                            amount = o.getDouble("amount"),
                            month = o.getInt("month"),
                            year = o.getInt("year"),
                            categoryId = if (o.isNull("categoryId")) null else o.getInt("categoryId")
                        )
                    )
                }
                
                // 4. Parse Recurring
                val recArray = root.getJSONArray("recurring_transactions")
                for (i in 0 until recArray.length()) {
                    val o = recArray.getJSONObject(i)
                    recList.add(
                        RecurringTransactionEntity(
                            amount = o.getDouble("amount"),
                            description = o.getString("description"),
                            categoryId = o.getInt("categoryId"),
                            type = o.getString("type"),
                            frequency = o.getString("frequency"),
                            startDate = o.getLong("startDate"),
                            nextDueDate = o.getLong("nextDueDate"),
                            isActive = o.getBoolean("isActive")
                        )
                    )
                }
                
                // 5. Parse Mappings
                val mapArray = root.getJSONArray("actor_category_mappings")
                for (i in 0 until mapArray.length()) {
                    val o = mapArray.getJSONObject(i)
                    mapList.add(
                        ActorCategoryMapping(
                            partyName = o.getString("partyName"),
                            categoryId = o.getInt("categoryId")
                        )
                    )
                }
                
                // Perform DB restore inside a transaction scope
                database.withTransaction {
                    val dao = database.transactionDao()
                    
                    dao.deleteAllTransactions()
                    dao.deleteAllBudgets()
                    dao.deleteAllCategories()
                    dao.deleteAllRecurringTransactions()
                    dao.deleteAllActorMappings()
                    
                    dao.insertCategories(catList)
                    dao.insertTransactions(txList)
                    dao.insertBudgets(budgetList)
                    dao.insertRecurringTransactions(recList)
                    dao.insertActorMappings(mapList)
                }
                
                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
