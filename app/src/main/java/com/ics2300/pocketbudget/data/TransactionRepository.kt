package com.ics2300.pocketbudget.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ics2300.pocketbudget.utils.CategoryUtils
import com.ics2300.pocketbudget.utils.CashFlowClassifier
import com.ics2300.pocketbudget.utils.MpesaParser
import com.ics2300.pocketbudget.utils.NotificationHelper
import com.ics2300.pocketbudget.utils.SmsReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

import androidx.room.withTransaction

class TransactionRepository(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val transactionDao: TransactionDao,
    private val smsReader: SmsReader
) {

    companion object {
        private const val TAG = "TransactionRepository"
        private const val PREF_NAME = "pocketbudget_prefs"
        private const val KEY_LAST_SMS_SYNC_TIMESTAMP = "last_sms_sync_timestamp"
    }

    private val smsProcessingMutex = Mutex()
    private val syncMutex = Mutex()

    fun startSmsListener(scope: CoroutineScope) {
        // BroadcastReceiver handles SMS processing.
    }

    val transactionsWithCategory: Flow<List<TransactionWithCategory>> =
        transactionDao.getTransactionsWithCategory()

    fun getFilteredTransactionsWithCategory(
        query: String?,
        startDate: Long?,
        endDate: Long?,
        minAmount: Double?,
        maxAmount: Double?,
        actor: String?,
        filterType: String
    ): Flow<List<TransactionWithCategory>> {
        return transactionDao.getFilteredTransactionsWithCategory(
            query = query,
            startDate = startDate,
            endDate = endDate,
            minAmount = minAmount,
            maxAmount = maxAmount,
            actor = actor,
            filterType = filterType
        )
    }

    val allTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    val allCategories: Flow<List<CategoryEntity>> =
        transactionDao.getAllCategories()

    val categorySpending: Flow<List<CategorySpending>> =
        transactionDao.getCategorySpending()

    val recurringTransactions: Flow<List<RecurringTransactionEntity>> =
        transactionDao.getAllRecurringTransactions()

    private var cachedCategories: List<CategoryEntity>? = null

    suspend fun getCategoriesCached(): List<CategoryEntity> {
        return cachedCategories ?: withContext(Dispatchers.IO) {
            val categories = transactionDao.getAllCategoriesList()
            cachedCategories = categories
            categories
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

    fun getDashboardStats(
        startDate: Long?,
        endDate: Long?
    ): Flow<DashboardStats> {
        return transactionDao.getDashboardStats(startDate, endDate)
    }

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> {
        return transactionDao.searchTransactions(query)
    }

    suspend fun getTransactionsByDateRange(
        start: Long,
        end: Long
    ): List<TransactionEntity> {
        return transactionDao.getTransactionsListByDateRange(start, end)
    }

    fun getBudgetProgress(
        month: Int,
        year: Int
    ): Flow<List<CategoryBudgetProgress>> {
        return transactionDao.getCategoryBudgetProgress(month, year)
    }

    fun getTopSpendingActors(limit: Int = 5): Flow<List<ActorSpending>> {
        return transactionDao.getTopSpendingActors(limit)
    }

    fun getTopSpendingActorsByDate(
        limit: Int = 5,
        start: Long,
        end: Long
    ): Flow<List<ActorSpending>> {
        return transactionDao.getTopSpendingActorsByDate(limit, start, end)
    }

    suspend fun setBudget(
        categoryId: Int,
        amount: Double,
        month: Int,
        year: Int
    ) {
        val existing = transactionDao.getBudgetForCategory(
            categoryId,
            month,
            year
        )

        val budget = existing?.copy(amount = amount)
            ?: BudgetEntity(
                amount = amount,
                month = month,
                year = year,
                categoryId = categoryId
            )

        transactionDao.insertBudget(budget)
    }

    suspend fun updateTransactionCategory(
        transactionId: Int,
        categoryId: Int
    ) {
        withContext(Dispatchers.IO) {

            val transaction = transactionDao.getTransactionById(transactionId)
                ?: return@withContext

            val actorNames = listOf(
                normalizePartyName(transaction.partyName),
                normalizePartyName(transaction.accountName.orEmpty())
            ).filter { it.isNotBlank() }.distinct()
            val categories = ensureDefaultCategoriesExist()
            val categoryName = categories.firstOrNull { it.id == categoryId }?.name
            val cashFlowBucket = CashFlowClassifier
                .bucket(transaction, categoryName)
                .name

            transactionDao.updateCategoryAndLearnActors(
                transactionId = transactionId,
                actorNames = actorNames,
                categoryId = categoryId,
                cashFlowBucket = cashFlowBucket
            )
        }
    }

    suspend fun bulkCategorizeSimilarTransactions(
        partyName: String,
        categoryId: Int
    ) {
        withContext(Dispatchers.IO) {

            val normalizedParty = normalizePartyName(partyName)

            if (normalizedParty.isBlank()) {
                return@withContext
            }

            val category = transactionDao.getCategoryById(categoryId) ?: return@withContext
            val categoryBucket = when (category.name.lowercase(Locale.US)) {
                "savings" -> "SAVINGS"
                "transfer & cash", "transfer" -> "TRANSFER"
                else -> "EXPENSE"
            }

            transactionDao.bulkCategorizePartyAndLearn(
                normalizedParty,
                categoryId,
                categoryBucket
            )
        }
    }

    suspend fun addRecurringTransaction(
        recurring: RecurringTransactionEntity
    ) {
        transactionDao.insertRecurringTransaction(recurring)
    }

    suspend fun disableRecurringTransaction(id: Int) {
        transactionDao.disableRecurringTransaction(id)
    }

    suspend fun getUpcomingRecurringTransactions(
        start: Long,
        end: Long
    ): List<RecurringTransactionEntity> {
        return transactionDao.getUpcomingRecurringTransactions(start, end)
    }

    suspend fun processDueRecurringTransactions() {
        withContext(Dispatchers.IO) {

            val currentTime = System.currentTimeMillis()
            val categories = ensureDefaultCategoriesExist()

            val dueItems =
                transactionDao.getDueRecurringTransactions(currentTime)

            for (item in dueItems) {

                try {

                    val transaction = classifyCashFlow(TransactionEntity(
                        transactionId = "REC_${item.id}_${item.nextDueDate}",
                        amount = item.amount,
                        type = item.type,
                        partyName = item.description,
                        timestamp = item.nextDueDate,
                        categoryId = item.categoryId
                    ), categories)

                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = item.nextDueDate
                    }

                    when (item.frequency) {
                        "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                        "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        "Monthly" -> calendar.add(Calendar.MONTH, 1)
                        "Yearly" -> calendar.add(Calendar.YEAR, 1)
                    }

                    transactionDao.insertRecurringAndAdvance(
                        transaction = transaction,
                        recurringId = item.id,
                        nextDate = calendar.timeInMillis
                    )

                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Failed recurring transaction ${item.id}",
                        e
                    )
                }
            }
        }
    }

    suspend fun getAllCategoriesList(): List<CategoryEntity> {
        return withContext(Dispatchers.IO) {
            ensureDefaultCategoriesExist()
        }
    }

    suspend fun addCategory(category: CategoryEntity): Int {
        return withContext(Dispatchers.IO) {
            val normalizedCategory =
                category.copy(keywords = normalizeKeywords(category.keywords))

            val newCategoryId =
                transactionDao.insertCategory(normalizedCategory)

            cachedCategories = null

            if (newCategoryId != -1L) {
                recategorizeUncategorizedTransactions()
            } else {
                0
            }
        }
    }

    suspend fun updateCategory(category: CategoryEntity): Int {
        return withContext(Dispatchers.IO) {
            transactionDao.updateCategory(
                category.id,
                category.name.trim(),
                normalizeKeywords(category.keywords),
                category.iconName,
                category.colorHex
            )

            cachedCategories = null
            recategorizeUncategorizedTransactions()
        }
    }

    suspend fun deleteCategory(categoryId: Int) {
        withContext(Dispatchers.IO) {
            val categories = ensureDefaultCategoriesExist()
            val uncategorizedCategory =
                categories.firstOrNull {
                    it.name.equals("Uncategorized", true)
                } ?: return@withContext

            if (categoryId == uncategorizedCategory.id) {
                return@withContext
            }

            appDatabase.withTransaction {
                transactionDao.reassignTransactionsCategory(
                    oldCategoryId = categoryId,
                    newCategoryId = uncategorizedCategory.id
                )
                transactionDao.reassignRecurringTransactionsCategory(
                    oldCategoryId = categoryId,
                    newCategoryId = uncategorizedCategory.id
                )
                transactionDao.deleteBudgetsForCategory(categoryId)
                transactionDao.deleteActorMappingsForCategory(categoryId)
                transactionDao.deleteCategory(categoryId)
            }
            cachedCategories = null
        }
    }

    suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        transactionDao.deleteAllBudgets()
    }

    private suspend fun ensureDefaultCategoriesExist(): List<CategoryEntity> {

        if (transactionDao.getCategoryCount() == 0) {

            for (category in CategoryUtils.getDefaultCategories()) {
                transactionDao.insertCategory(
                    category.copy(
                        keywords = normalizeKeywords(category.keywords)
                    )
                )
            }

            cachedCategories = null

        } else {

            val categories = transactionDao.getAllCategoriesList()

            if (categories.none {
                    it.name.equals("Uncategorized", true)
                }) {

                transactionDao.insertCategory(
                    CategoryEntity(
                        name = "Uncategorized",
                        keywords = "UNCATEGORIZED",
                        colorHex = CategoryUtils.getDefaultColorHex(0)
                    )
                )

                cachedCategories = null
            }

            if (categories.none { it.name.equals("Savings", true) }) {
                transactionDao.insertCategory(
                    CategoryEntity(
                        name = "Savings",
                        keywords = "SAVINGS,INVESTMENT,MMF,CHAMA",
                        colorHex = CategoryUtils.getDefaultColorHex(10)
                    )
                )
                cachedCategories = null
            }
        }

        return transactionDao.getAllCategoriesList()
    }

    suspend fun processNewSms(
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ): TransactionEntity? {

        return withContext(Dispatchers.IO) {

            smsProcessingMutex.withLock {

                val transaction =
                    MpesaParser.parse(body, timestamp)
                        ?: return@withLock null

                val categories = ensureDefaultCategoriesExist()

                val categorized = classifyCashFlow(
                    categorizeTransaction(transaction, categories),
                    categories
                )

                if (categorized.categoryId == null) {
                    Log.e(TAG, "Transaction missing category.")
                    return@withLock null
                }

                val result =
                    transactionDao.insertTransaction(categorized)

                if (result == -1L) {
                    return@withLock null
                }

                val categoryName = categories.find {
                    it.id == categorized.categoryId
                }?.name ?: "Uncategorized"

                try {
                    NotificationHelper.notifyNewTransaction(
                        context,
                        categorized,
                        categoryName
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Notification failed.", e)
                }

                categorized
            }
        }
    }

    private suspend fun categorizeTransaction(
        transaction: TransactionEntity,
        categories: List<CategoryEntity>
    ): TransactionEntity {

        var matchedCategoryId: Int? = null

        if (transaction.type.equals("Reversal", ignoreCase = true)) {
            val prefix = "REVERSAL OF "
            val normalizedPartyName = transaction.partyName.uppercase(Locale.US)
            if (normalizedPartyName.startsWith(prefix)) {
                val originalTx = normalizedPartyName.removePrefix(prefix).trim()
                if (originalTx.isNotBlank()) {
                    val originalTransaction = transactionDao.getTransactionByTxId(originalTx)
                    if (originalTransaction != null && originalTransaction.categoryId != null) {
                        matchedCategoryId = originalTransaction.categoryId
                    }
                }
            }
        }

        val party =
            normalizePartyName(transaction.partyName)

        val account =
            normalizePartyName(transaction.accountName.orEmpty())

        val searchableText =
            listOf(party, account)
                .filter { it.isNotBlank() }
                .joinToString(" ")

        var learnedCategory: CategoryEntity? = null
        for (actor in listOf(party, account).filter { it.isNotBlank() }.distinct()) {
            val learnedCategoryId = transactionDao.getCategoryIdForActor(actor)
            val candidate = categories.firstOrNull { it.id == learnedCategoryId }
            if (candidate != null &&
                (!candidate.name.equals("Income", true) ||
                    transaction.type.equals("Received", true) ||
                    transaction.type.equals("Deposit", true))
            ) {
                learnedCategory = candidate
                break
            }
        }

        // A manually learned Income category should not turn a later
        // withdrawal/payment from the same till into income. Other explicit
        // categories, such as Savings, remain valid for both directions.
        if (learnedCategory != null) {
            matchedCategoryId = learnedCategory.id
        }

        // Incoming M-Pesa messages represent money received by default.
        // Keyword matching must not turn a normal receipt from a bank, till,
        // or merchant into an expense/transfer. A learned Savings mapping
        // above is the deliberate exception for dedicated savings actors.
        if (matchedCategoryId == null && transaction.type.equals("Received", true)) {
            matchedCategoryId = categories.firstOrNull {
                it.name.equals("Income", true)
            }?.id
        }

        if (matchedCategoryId == null) {

            for (category in categories) {

                if (category.name.equals("Income", true) &&
                    !transaction.type.equals("Received", true) &&
                    !transaction.type.equals("Deposit", true)
                ) {
                    continue
                }

                val keywords = category.keywords.split(",")

                for (keyword in keywords) {

                    val kw = normalizeKeyword(keyword)

                    if (
                        kw.isNotBlank() &&
                        keywordMatches(searchableText, kw)
                    ) {
                        matchedCategoryId = category.id
                        break
                    }
                }

                if (matchedCategoryId != null) break
            }
        }

        if (matchedCategoryId == null) {
            matchedCategoryId =
                detectBehavioralCategory(transaction)
        }

        if (matchedCategoryId == null) {

            matchedCategoryId = when (transaction.type) {

                "Sent",
                "Buy Goods",
                "Paybill" -> {
                    categories.find {
                        it.name.contains("Transfer", true)
                    }?.id
                }

                "Received" -> {
                    categories.find {
                        it.name.contains("Income", true)
                    }?.id
                }

                else -> null
            }
        }

        if (matchedCategoryId == null) {
            matchedCategoryId = categories.find {
                it.name.equals("Uncategorized", true)
            }?.id
        }

        return transaction.copy(
            categoryId = matchedCategoryId
                ?: categories.firstOrNull()?.id
        )
    }

    private fun classifyCashFlow(
        transaction: TransactionEntity,
        categories: List<CategoryEntity>
    ): TransactionEntity {
        val categoryName = categories.firstOrNull { it.id == transaction.categoryId }?.name
        return transaction.copy(
            cashFlowBucket = CashFlowClassifier
                .bucket(transaction, categoryName)
                .name
        )
    }

    private suspend fun detectBehavioralCategory(
        transaction: TransactionEntity
    ): Int? {

        val party =
            normalizePartyName(transaction.partyName)

        if (party.isBlank()) {
            return null
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = transaction.timestamp
        }

        val dayOfMonth =
            calendar.get(Calendar.DAY_OF_MONTH)

        val previousTransactions =
            transactionDao.getCategorizedTransactionsByPartyName(party)

        if (previousTransactions.size < 2) {
            return null
        }

        val similarDayCount =
            previousTransactions.count { previous ->

                val previousCalendar = Calendar.getInstance().apply {
                    timeInMillis = previous.timestamp
                }

                val previousDay =
                    previousCalendar.get(Calendar.DAY_OF_MONTH)

                abs(previousDay - dayOfMonth) <= 3 ||
                    abs(previousDay - dayOfMonth) >= 27
            }

        if (similarDayCount < 2) {
            return null
        }

        val uncategorizedCategoryId =
            transactionDao.getAllCategoriesList()
                .firstOrNull {
                    it.name.equals("Uncategorized", true)
                }
                ?.id

        return previousTransactions
            .mapNotNull { it.categoryId }
            .filter { it != uncategorizedCategoryId }
            .groupBy { it }
            .maxByOrNull { it.value.size }
            ?.key
    }

    private suspend fun recategorizeUncategorizedTransactions(): Int {
        val categories = ensureDefaultCategoriesExist()
        val uncategorizedCategory =
            categories.firstOrNull {
                it.name.equals("Uncategorized", true)
            } ?: return 0

        val candidates =
            transactionDao.getUncategorizedTransactions(uncategorizedCategory.id)

        var updatedCount = 0

        for (transaction in candidates) {
            val categorized = classifyCashFlow(
                categorizeTransaction(
                    transaction.copy(categoryId = null),
                    categories
                ),
                categories
            )

            val newCategoryId =
                categorized.categoryId ?: continue

            if (newCategoryId != transaction.categoryId) {
                transactionDao.updateTransactionCategoryAndBucket(
                    transaction.id,
                    newCategoryId,
                    categorized.cashFlowBucket
                )
                updatedCount++
            }
        }

        return updatedCount
    }

    private fun normalizePartyName(value: String): String {
        return value
            .trim()
            .uppercase(Locale.US)
    }

    private fun normalizeKeywords(value: String): String {
        return value
            .split(",")
            .map { normalizeKeyword(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(",")
    }

    private fun normalizeKeyword(value: String): String {
        return normalizePartyName(value)
    }

    private fun keywordMatches(text: String, keyword: String): Boolean {
        if (text.isBlank() || keyword.isBlank()) {
            return false
        }

        if (keyword.length > 3) {
            return text.contains(keyword)
        }

        return Regex("(^|[^A-Z0-9])${Regex.escape(keyword)}([^A-Z0-9]|$)")
            .containsMatchIn(text)
    }

    suspend fun importData(
        transactions: List<TransactionEntity>
    ): Int {

        return withContext(Dispatchers.IO) {

            val categories = ensureDefaultCategoriesExist()

            var importedCount = 0

            for (transaction in transactions) {

                val transactionToInsert =
                    if (transaction.categoryId == null) {
                        categorizeTransaction(transaction, categories)
                    } else {
                        transaction
                    }
                val classifiedTransaction = classifyCashFlow(transactionToInsert, categories)

                if (classifiedTransaction.categoryId == null) {
                    continue
                }

                try {

                    val result =
                        transactionDao.insertTransaction(
                            classifiedTransaction
                        )

                    if (result != -1L) {
                        importedCount++

                        val wasExplicitlyCategorized = transaction.categoryId != null
                        val actorNames = listOf(
                            normalizePartyName(classifiedTransaction.partyName),
                            normalizePartyName(classifiedTransaction.accountName.orEmpty())
                        ).filter { it.isNotBlank() }.distinct()
                        val catId = classifiedTransaction.categoryId
                        val uncategorizedId = categories.find { it.name.equals("Uncategorized", true) }?.id
                        if (wasExplicitlyCategorized && catId != uncategorizedId) {
                            actorNames.forEach { actorName ->
                                transactionDao.insertActorMapping(
                                    ActorCategoryMapping(actorName, catId)
                                )
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Import failed.", e)
                }
            }

            importedCount
        }
    }

    suspend fun syncTransactions(): Int {

        return withContext(Dispatchers.IO) {

            syncMutex.withLock {

                val categories = ensureDefaultCategoriesExist()

                val prefs = context.getSharedPreferences(
                    PREF_NAME,
                    Context.MODE_PRIVATE
                )

                val lastSyncTime =
                    prefs.getLong(
                        KEY_LAST_SMS_SYNC_TIMESTAMP,
                        0L
                    )

                val messages =
                    smsReader.readMpesaMessages(lastSyncTime)

                var newCount = 0
                var maxTimestamp = lastSyncTime

                for (smsData in messages) {

                    if (smsData.date > maxTimestamp) {
                        maxTimestamp = smsData.date
                    }

                    val transaction =
                        MpesaParser.parse(
                            smsData.body,
                            smsData.date
                        ) ?: continue

                    val categorized = classifyCashFlow(
                        categorizeTransaction(transaction, categories),
                        categories
                    )

                    if (categorized.categoryId == null) {
                        continue
                    }

                    try {

                        val result =
                            transactionDao.insertTransaction(
                                categorized
                            )

                        if (result != -1L) {
                            newCount++
                        }

                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Failed synced transaction.",
                            e
                        )
                    }
                }

                if (maxTimestamp > lastSyncTime) {

                    prefs.edit()
                        .putLong(
                            KEY_LAST_SMS_SYNC_TIMESTAMP,
                            maxTimestamp
                        )
                        .commit()
                }

                newCount
            }
        }
    }

    suspend fun resyncAllTransactions(): Int {

        return withContext(Dispatchers.IO) {

            syncMutex.withLock {

                val categories = ensureDefaultCategoriesExist()

                val messages =
                    smsReader.readMpesaMessages(0L)

                var newCount = 0
                var maxTimestamp = 0L

                for (smsData in messages) {

                    if (smsData.date > maxTimestamp) {
                        maxTimestamp = smsData.date
                    }

                    val transaction =
                        MpesaParser.parse(
                            smsData.body,
                            smsData.date
                        ) ?: continue

                    val categorized = classifyCashFlow(
                        categorizeTransaction(transaction, categories),
                        categories
                    )

                    if (categorized.categoryId == null) {
                        continue
                    }

                    try {

                        val result =
                            transactionDao.insertTransaction(
                                categorized
                            )

                        if (result != -1L) {
                            newCount++
                        }

                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Resync insert failed.",
                            e
                        )
                    }
                }

                val prefs = context.getSharedPreferences(
                    PREF_NAME,
                    Context.MODE_PRIVATE
                )

                val lastSyncTime =
                    prefs.getLong(
                        KEY_LAST_SMS_SYNC_TIMESTAMP,
                        0L
                    )

                if (maxTimestamp > lastSyncTime) {

                    prefs.edit()
                        .putLong(
                            KEY_LAST_SMS_SYNC_TIMESTAMP,
                            maxTimestamp
                        )
                        .commit()
                }

                newCount
            }
        }
    }

    suspend fun insert(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            val categories = ensureDefaultCategoriesExist()
            transactionDao.insertTransaction(classifyCashFlow(transaction, categories))
        }
    }

    suspend fun exportBackup(uri: Uri, password: String): Result<Boolean> {
        return com.ics2300.pocketbudget.utils.BackupManager.exportBackup(
            context,
            uri,
            appDatabase,
            password
        )
    }

    suspend fun importBackup(uri: Uri, password: String): Result<Boolean> {
        return com.ics2300.pocketbudget.utils.BackupManager.importBackup(
            context,
            uri,
            appDatabase,
            password
        )
    }
}
