package com.ics2300.pocketbudget.data

import com.ics2300.pocketbudget.utils.MpesaParser
import com.ics2300.pocketbudget.utils.SmsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val smsReader: SmsReader
) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> {
        return transactionDao.searchTransactions(query)
    }

    suspend fun getTransactionsByDateRange(start: Long, end: Long): List<TransactionEntity> {
        return transactionDao.getTransactionsListByDateRange(start, end)
    }
    
    val categorySpending: Flow<List<CategorySpending>> = transactionDao.getCategorySpending()

    fun getBudgetProgress(month: Int, year: Int): Flow<List<CategoryBudgetProgress>> {
        return transactionDao.getCategoryBudgetProgress(month, year)
    }

    suspend fun setBudget(categoryId: Int, amount: Double, month: Int, year: Int) {
        val existing = transactionDao.getBudgetForCategory(categoryId, month, year)
        val budget = if (existing != null) {
            existing.copy(amount = amount)
        } else {
            BudgetEntity(amount = amount, month = month, year = year, categoryId = categoryId)
        }
        transactionDao.insertBudget(budget)
    }
    
    suspend fun updateTransactionCategory(transactionId: Int, categoryId: Int) {
        transactionDao.updateTransactionCategory(transactionId, categoryId)
    }

    // Recurring Transaction Methods
    val recurringTransactions: Flow<List<RecurringTransactionEntity>> = transactionDao.getAllRecurringTransactions()

    suspend fun addRecurringTransaction(recurring: RecurringTransactionEntity) {
        transactionDao.insertRecurringTransaction(recurring)
    }

    suspend fun disableRecurringTransaction(id: Int) {
        transactionDao.disableRecurringTransaction(id)
    }

    suspend fun processDueRecurringTransactions() {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val dueItems = transactionDao.getDueRecurringTransactions(currentTime)
            
            for (item in dueItems) {
                // 1. Create the transaction
                val transaction = TransactionEntity(
                    transactionId = "REC_${item.id}_${item.nextDueDate}",
                    amount = item.amount,
                    type = item.type, // "Expense" or "Income"
                    partyName = item.description,
                    timestamp = item.nextDueDate,
                    categoryId = item.categoryId
                )
                
                try {
                    transactionDao.insertTransaction(transaction)
                    
                    // 2. Update next due date
                    val calendar = java.util.Calendar.getInstance()
                    calendar.timeInMillis = item.nextDueDate
                    
                    when (item.frequency) {
                        "Daily" -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                        "Weekly" -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                        "Monthly" -> calendar.add(java.util.Calendar.MONTH, 1)
                        "Yearly" -> calendar.add(java.util.Calendar.YEAR, 1)
                    }
                    
                    transactionDao.updateRecurringNextDueDate(item.id, calendar.timeInMillis)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun getAllCategoriesList(): List<CategoryEntity> {
        return transactionDao.getAllCategoriesList()
    }

    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return transactionDao.getAllCategories()
    }

    suspend fun addCategory(category: CategoryEntity) {
        transactionDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        transactionDao.updateCategory(category.id, category.name, category.keywords)
    }

    suspend fun deleteCategory(categoryId: Int) {
        transactionDao.deleteCategory(categoryId)
    }

    suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        transactionDao.deleteAllBudgets()
    }

    suspend fun processNewSms(body: String): TransactionEntity? {
        val transaction = MpesaParser.parse(body) ?: return null
        
        val categories = transactionDao.getAllCategoriesList()
        val categorizedTransaction = categorizeTransaction(transaction, categories)
        
        transactionDao.insertTransaction(categorizedTransaction)
        return categorizedTransaction
    }

    private fun categorizeTransaction(transaction: TransactionEntity, categories: List<CategoryEntity>): TransactionEntity {
        var matchedCategoryId: Int? = null
        val party = transaction.partyName.uppercase()
        val type = transaction.type.uppercase()
        
        for (cat in categories) {
            val keywords = cat.keywords.split(",")
            for (keyword in keywords) {
                val kw = keyword.trim().uppercase()
                if (kw.isNotEmpty() && (party.contains(kw) || type.contains(kw))) {
                    matchedCategoryId = cat.id
                    break
                }
            }
            if (matchedCategoryId != null) break
        }
        
        if (matchedCategoryId == null) {
            if (transaction.type == "Sent") {
                 matchedCategoryId = categories.find { it.name == "Transfer" }?.id
            } else if (transaction.type == "Received") {
                 matchedCategoryId = categories.find { it.name == "Income" }?.id
            }
        }
        
        return transaction.copy(categoryId = matchedCategoryId ?: 1) // Default to Uncategorized (ID 1 usually)
    }

    suspend fun syncTransactions(): Int {
        return withContext(Dispatchers.IO) {
            // 1. Ensure categories exist
            if (transactionDao.getCategoryCount() == 0) {
                val defaults = listOf(
                    CategoryEntity(name = "Food", keywords = "HOTEL,CAFE,RESTAURANT,JAVA,KFC,PIZZA,BURGER"),
                    CategoryEntity(name = "Groceries", keywords = "SUPERMARKET,MART,NAIVAS,QUICKMART,CARREFOUR,CHANDARANA"),
                    CategoryEntity(name = "Transport", keywords = "UBER,BOLT,MATATU,SHELL,TOTAL,RUBIS,PETROL,STATION"),
                    CategoryEntity(name = "Utilities", keywords = "KPLC,TOKEN,ZUKU,SAFARICOM,AIRTEL,INTERNET,WIFI,POWER"),
                    CategoryEntity(name = "Entertainment", keywords = "NETFLIX,CINEMA,MOVIE,DSTV,SHOWMAX"),
                    CategoryEntity(name = "Shopping", keywords = "CLOTHING,MALL,FASHION,STORE,SHOP"),
                    CategoryEntity(name = "Health", keywords = "HOSPITAL,CHEMIST,PHARMACY,DOCTOR,CLINIC"),
                    CategoryEntity(name = "Rent", keywords = "RENT,LANDLORD,HOUSING"),
                    CategoryEntity(name = "Education", keywords = "SCHOOL,COLLEGE,UNIVERSITY,FEES,TUITION"),
                    CategoryEntity(name = "Transfer", keywords = "SENT"),
                    CategoryEntity(name = "Income", keywords = "RECEIVED")
                )
                defaults.forEach { transactionDao.insertCategory(it) }
            }

            val categories = transactionDao.getAllCategoriesList()

            // 2. Read and parse SMS incrementally
            // In a real app, store this in SharedPreferences
            // For now, we will read everything but only insert new ones (Room ignores conflicts)
            // Ideally: val lastSync = prefs.getLong("last_sync", 0)
            // But since we rely on unique IDs, reading all is safe but slow. 
            // Let's implement a simple memory-based optimization or just read all for now
            // as we don't have access to SharedPrefs here easily without injection.
            
            // However, SmsReader now supports filtering.
            // Let's assume we want to scan all for robustness since we don't persist the cursor.
            val messages = smsReader.readMpesaMessages(0L) 
            
            var newCount = 0
            for (msg in messages) {
                val transaction = MpesaParser.parse(msg)
                if (transaction != null) {
                    val categorized = categorizeTransaction(transaction, categories)
                    try {
                        transactionDao.insertTransaction(categorized)
                        // If we reach here, it might have been inserted (or ignored). 
                        // Room's OnConflictStrategy.IGNORE returns -1L if ignored.
                        // We can't easily count "new" without changing DAO to return Long.
                        // For UI feedback, let's just count valid parsed messages.
                        newCount++
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
            newCount
        }
    }

    suspend fun insert(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }
}
