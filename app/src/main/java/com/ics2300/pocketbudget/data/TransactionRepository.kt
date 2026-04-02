package com.ics2300.pocketbudget.data

import android.content.Context
import com.ics2300.pocketbudget.utils.CategoryUtils
import com.ics2300.pocketbudget.utils.MpesaParser
import com.ics2300.pocketbudget.utils.NotificationHelper
import com.ics2300.pocketbudget.utils.SmsReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class TransactionRepository(
    private val context: Context,
    private val transactionDao: TransactionDao,
    private val smsReader: SmsReader
) {

    fun startSmsListener(scope: CoroutineScope) {
        // Since we are back to standard SMS_RECEIVED broadcast receiver, 
        // we don't need to start a listener here manually anymore.
        // The SmsReceiver in the manifest will handle it.
        // However, we'll keep the method signature for compatibility.
    }

    val transactionsWithCategory: Flow<List<TransactionWithCategory>> = transactionDao.getTransactionsWithCategory()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allCategories: Flow<List<CategoryEntity>> = transactionDao.getAllCategories()

    private var cachedCategories: List<CategoryEntity>? = null

    suspend fun getCategoriesCached(): List<CategoryEntity> {
        return cachedCategories ?: withContext(Dispatchers.IO) {
            val cats = transactionDao.getAllCategoriesList()
            cachedCategories = cats
            cats
        }
    }

    fun getFilteredTransactions(
        query: String?,
        startDate: Long?,
        endDate: Long?,
        minAmount: Double?,
        maxAmount: Double?,
        actor: String?,
        filterType: String
    ): Flow<List<TransactionEntity>> {
        return transactionDao.getFilteredTransactions(
            if (query.isNullOrBlank()) null else query,
            startDate,
            endDate,
            minAmount,
            maxAmount,
            if (actor.isNullOrBlank()) null else actor,
            filterType
        )
    }

    fun getDashboardStats(startDate: Long?, endDate: Long?): Flow<DashboardStats> {
        return transactionDao.getDashboardStats(startDate, endDate)
    }

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

    fun getTopSpendingActors(limit: Int = 5): Flow<List<ActorSpending>> {
        return transactionDao.getTopSpendingActors(limit)
    }

    fun getTopSpendingActorsByDate(limit: Int = 5, start: Long, end: Long): Flow<List<ActorSpending>> {
        return transactionDao.getTopSpendingActorsByDate(limit, start, end)
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
        withContext(Dispatchers.IO) {
            transactionDao.updateTransactionCategory(transactionId, categoryId)
            
            // Learn from this edit!
            val transaction = transactionDao.getTransactionById(transactionId)
            if (transaction != null) {
                val normalizedParty = transaction.partyName.trim().uppercase()
                if (normalizedParty.isNotEmpty()) {
                    transactionDao.insertActorMapping(ActorCategoryMapping(normalizedParty, categoryId))
                }
            }
        }
    }

    suspend fun bulkCategorizeSimilarTransactions(partyName: String, categoryId: Int) {
        withContext(Dispatchers.IO) {
            val normalizedParty = partyName.trim().uppercase()
            if (normalizedParty.isEmpty()) return@withContext

            // 1. Get all transactions for this party
            val transactions = transactionDao.getAllTransactions().firstOrNull() ?: emptyList()
            val targets = transactions.filter { it.partyName.trim().uppercase() == normalizedParty }

            // 2. Update each transaction
            for (t in targets) {
                transactionDao.updateTransactionCategory(t.id, categoryId)
            }

            // 3. Update/Insert actor mapping for future transactions
            transactionDao.insertActorMapping(ActorCategoryMapping(normalizedParty, categoryId))
        }
    }

    // Recurring Transaction Methods
    val recurringTransactions: Flow<List<RecurringTransactionEntity>> = transactionDao.getAllRecurringTransactions()

    suspend fun addRecurringTransaction(recurring: RecurringTransactionEntity) {
        transactionDao.insertRecurringTransaction(recurring)
    }

    suspend fun disableRecurringTransaction(id: Int) {
        transactionDao.disableRecurringTransaction(id)
    }

    suspend fun getUpcomingRecurringTransactions(start: Long, end: Long): List<RecurringTransactionEntity> {
        return transactionDao.getUpcomingRecurringTransactions(start, end)
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

    suspend fun addCategory(category: CategoryEntity) {
        transactionDao.insertCategory(category)
        cachedCategories = null // Invalidate cache
    }

    suspend fun updateCategory(category: CategoryEntity) {
        transactionDao.updateCategory(category.id, category.name, category.keywords, category.iconName, category.colorHex)
        cachedCategories = null // Invalidate cache
    }

    suspend fun deleteCategory(categoryId: Int) {
        transactionDao.deleteCategory(categoryId)
        cachedCategories = null // Invalidate cache
    }

    suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        transactionDao.deleteAllBudgets()
    }

    suspend fun processNewSms(body: String, timestamp: Long = System.currentTimeMillis()): TransactionEntity? = withContext(Dispatchers.IO) {
        val transaction = MpesaParser.parse(body, timestamp) ?: return@withContext null

        val categories = getCategoriesCached()
        val categorizedTransaction = categorizeTransaction(transaction, categories)

        val result = transactionDao.insertTransaction(categorizedTransaction)
        if (result == -1L) {
            // Duplicate transaction found (based on unique transactionId)
            // We return null or the transaction?
            // Returning null indicates "nothing new added"
            return@withContext null
        }

        // Notify through notification module
        val categoryName = categories.find { it.id == categorizedTransaction.categoryId }?.name ?: "Uncategorized"
        NotificationHelper.notifyNewTransaction(context, categorizedTransaction, categoryName)

        return@withContext categorizedTransaction
    }

    private suspend fun categorizeTransaction(transaction: TransactionEntity, categories: List<CategoryEntity>): TransactionEntity {
        var matchedCategoryId: Int? = null
        val party = transaction.partyName.trim().uppercase()
        val type = transaction.type.uppercase()
        val account = transaction.accountName?.trim()?.uppercase() ?: ""

        // 0. Check for learned mappings first (highest priority)
        if (party.isNotEmpty()) {
            matchedCategoryId = transactionDao.getCategoryIdForActor(party)
        }

        // 1. Tiered Keyword Matching
        if (matchedCategoryId == null) {
            // First pass: Match against Party Name and Account Number (Account is critical for Paybills)
            for (cat in categories) {
                val keywords = cat.keywords.split(",")
                for (keyword in keywords) {
                    val kw = keyword.trim().uppercase()
                    if (kw.isNotEmpty() && (party.contains(kw) || account.contains(kw))) {
                        matchedCategoryId = cat.id
                        break
                    }
                }
                if (matchedCategoryId != null) break
            }
        }

        // 2. Behavioral Learning: Recurring Pattern Detection (Rent, Salary, etc.)
        if (matchedCategoryId == null) {
            matchedCategoryId = detectBehavioralCategory(transaction)
        }

        // 3. Fallback to Type-based defaults
        if (matchedCategoryId == null) {
            if (transaction.type == "Sent" || transaction.type == "Buy Goods" || transaction.type == "Paybill") {
                 matchedCategoryId = categories.find { it.name.contains("Transfer", ignoreCase = true) }?.id
            } else if (transaction.type == "Received") {
                 matchedCategoryId = categories.find { it.name.contains("Income", ignoreCase = true) }?.id
            }
        }

        // If still null, default to "Uncategorized" category ID
        if (matchedCategoryId == null) {
            matchedCategoryId = categories.find { it.name.equals("Uncategorized", ignoreCase = true) }?.id
        }

        // Final fallback: if "Uncategorized" still not found, we use the first available category
        // or null, but we MUST avoid a hardcoded "1" that might not exist.
        return transaction.copy(categoryId = matchedCategoryId ?: categories.firstOrNull()?.id)
    }

    private suspend fun detectBehavioralCategory(transaction: TransactionEntity): Int? {
        val party = transaction.partyName.trim().uppercase()
        if (party.isEmpty()) return null

        // 1. Look for recurring patterns (e.g., Rent, Salary)
        // Check if this party appears around the same day of the month (+/- 3 days) in previous months
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = transaction.timestamp
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        
        // Look back at transactions for this party
        val previousTransactions = transactionDao.getAllTransactions().firstOrNull()?.filter {
            it.partyName.trim().uppercase() == party && it.categoryId != null
        } ?: emptyList()

        if (previousTransactions.size >= 2) {
            // Check if at least 2 previous transactions were on a similar day of the month
            val similarDayCount = previousTransactions.count { prev ->
                val prevCal = Calendar.getInstance()
                prevCal.timeInMillis = prev.timestamp
                val prevDay = prevCal.get(Calendar.DAY_OF_MONTH)
                Math.abs(prevDay - dayOfMonth) <= 3 || Math.abs(prevDay - dayOfMonth) >= 27
            }

            if (similarDayCount >= 2) {
                // Return the most frequent category for this recurring transaction
                return previousTransactions
                    .groupBy { it.categoryId }
                    .maxByOrNull { it.value.size }
                    ?.key
            }
        }

        return null
    }

    suspend fun importData(transactions: List<TransactionEntity>): Int {
        return withContext(Dispatchers.IO) {
            val categories = transactionDao.getAllCategoriesList()
            var importedCount = 0

            for (t in transactions) {
                // Try to categorize if missing
                val transactionToInsert = if (t.categoryId == null) {
                    categorizeTransaction(t, categories)
                } else {
                    t
                }

                try {
                    val result = transactionDao.insertTransaction(transactionToInsert)
                    if (result != -1L) {
                        importedCount++
                    }
                } catch (e: Exception) {
                    // Ignore duplicates
                }
            }
            importedCount
        }
    }

    suspend fun syncTransactions(): Int {
        return withContext(Dispatchers.IO) {
            // 1. Ensure categories exist
            if (transactionDao.getCategoryCount() == 0) {
                val defaults = CategoryUtils.getDefaultCategories()
                for (category in defaults) {
                    transactionDao.insertCategory(category)
                }
                cachedCategories = null // Invalidate cache after initial setup
            } else {
                // Ensure "Uncategorized" exists for existing users
                val allCats = getCategoriesCached()
                if (allCats.none { it.name.equals("Uncategorized", ignoreCase = true) }) {
                    transactionDao.insertCategory(CategoryEntity(name = "Uncategorized", keywords = "UNCATEGORIZED", colorHex = CategoryUtils.getDefaultColorHex(0)))
                    cachedCategories = null // Invalidate cache
                }
            }

            val categories = getCategoriesCached()

            // 2. Read and parse SMS incrementally using SharedPreferences
            val prefs = context.getSharedPreferences("pocketbudget_prefs", Context.MODE_PRIVATE)
            val lastSyncTime = prefs.getLong("last_sms_sync_timestamp", 0L)

            // Read messages after the last sync timestamp
            val messages = smsReader.readMpesaMessages(lastSyncTime)

            var newCount = 0
            var maxTimestamp = lastSyncTime

            for (smsData in messages) {
                // Update max timestamp seen
                if (smsData.date > maxTimestamp) {
                    maxTimestamp = smsData.date
                }

                val transaction = MpesaParser.parse(smsData.body, smsData.date)
                if (transaction != null) {
                    // Pre-fill fullSmsBody for old transactions if needed
                    val categorized = categorizeTransaction(transaction, categories)
                    try {
                        val result = transactionDao.insertTransaction(categorized)
                        if (result != -1L) {
                            newCount++
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }

            // Update the sync timestamp only if we processed messages
            if (maxTimestamp > lastSyncTime) {
                prefs.edit().putLong("last_sms_sync_timestamp", maxTimestamp).apply()
            }

            newCount
        }
    }

    /**
     * Resync ALL M-Pesa SMS from the inbox (from the very beginning, timestamp = 0).
     * Only inserts transactions that are missing from the database.
     * Existing transactions are safely skipped via the unique transactionId constraint (INSERT OR IGNORE).
     * Also updates last_sms_sync_timestamp if newer messages are found, keeping the
     * incremental dashboard sync aligned.
     */
    suspend fun resyncAllTransactions(): Int {
        return withContext(Dispatchers.IO) {
            // 1. Ensure default categories exist
            if (transactionDao.getCategoryCount() == 0) {
                val defaults = CategoryUtils.getDefaultCategories()
                for (category in defaults) {
                    transactionDao.insertCategory(category)
                }
            } else {
                // Ensure "Uncategorized" exists for existing users
                val allCats = transactionDao.getAllCategoriesList()
                if (allCats.none { it.name.equals("Uncategorized", ignoreCase = true) }) {
                    transactionDao.insertCategory(CategoryEntity(name = "Uncategorized", keywords = "UNCATEGORIZED", colorHex = CategoryUtils.getDefaultColorHex(0)))
                }
            }

            val categories = transactionDao.getAllCategoriesList()

            // 2. Read ALL M-Pesa SMS from the very beginning (afterTimestamp = 0L)
            val messages = smsReader.readMpesaMessages(0L)

            var newCount = 0
            var maxTimestamp = 0L

            for (smsData in messages) {
                // Track the latest timestamp found across all SMS
                if (smsData.date > maxTimestamp) {
                    maxTimestamp = smsData.date
                }

                val transaction = MpesaParser.parse(smsData.body, smsData.date)
                if (transaction != null) {
                    val categorized = categorizeTransaction(transaction, categories)
                    try {
                        // INSERT OR IGNORE: duplicates are silently skipped
                        val result = transactionDao.insertTransaction(categorized)
                        if (result != -1L) {
                            newCount++ // Only counts genuinely new/missing transactions
                        }
                    } catch (e: Exception) {
                        // Ignore individual failures; continue processing the rest
                    }
                }
            }

            // 3. Advance last_sms_sync_timestamp if resync found messages newer than what
            //    the incremental sync had recorded. This prevents the dashboard sync from
            //    re-scanning already-processed messages on its next run.
            val prefs = context.getSharedPreferences("pocketbudget_prefs", Context.MODE_PRIVATE)
            val lastSyncTime = prefs.getLong("last_sms_sync_timestamp", 0L)
            if (maxTimestamp > lastSyncTime) {
                prefs.edit().putLong("last_sms_sync_timestamp", maxTimestamp).apply()
            }

            newCount
        }
    }

    suspend fun insert(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }
}
