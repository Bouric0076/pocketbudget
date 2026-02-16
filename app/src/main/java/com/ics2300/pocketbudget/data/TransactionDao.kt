package com.ics2300.pocketbudget.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // Transaction Methods
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTransactionsListByDateRange(startDate: Long, endDate: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE partyName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId")
    fun getTransactionsByCategory(categoryId: Int): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :transactionId")
    suspend fun updateTransactionCategory(transactionId: Int, categoryId: Int)

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity): Long

    @Query("UPDATE recurring_transactions SET isActive = 0 WHERE id = :id")
    suspend fun disableRecurringTransaction(id: Int)

    @Query("UPDATE recurring_transactions SET nextDueDate = :nextDate WHERE id = :id")
    suspend fun updateRecurringNextDueDate(id: Int, nextDate: Long)

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDate <= :currentTime")
    suspend fun getDueRecurringTransactions(currentTime: Long): List<RecurringTransactionEntity>

    // Category Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): CategoryEntity?

    @Query("UPDATE categories SET name = :name, keywords = :keywords WHERE id = :id")
    suspend fun updateCategory(id: Int, name: String, keywords: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Int)

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT c.name as categoryName, SUM(t.amount) as totalAmount FROM transactions t INNER JOIN categories c ON t.categoryId = c.id WHERE t.type NOT IN ('Received', 'Deposit') GROUP BY c.id ORDER BY totalAmount DESC")
    fun getCategorySpending(): Flow<List<CategorySpending>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    // Budget Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetForCategory(categoryId: Int, month: Int, year: Int): BudgetEntity?
    
    // Complex query to get budget progress for all categories
    // This left joins categories with their budgets for the specific month/year
    // And also joins with aggregated transaction sums
    @Query("""
        SELECT 
            c.id as categoryId, 
            c.name as categoryName, 
            COALESCE(SUM(CASE WHEN t.type NOT IN ('Received', 'Deposit') THEN t.amount ELSE 0 END), 0) as totalSpent,
            COALESCE(b.amount, 0) as budgetAmount,
            :month as month,
            :year as year
        FROM categories c
        LEFT JOIN transactions t ON c.id = t.categoryId AND strftime('%m', t.timestamp / 1000, 'unixepoch') = printf('%02d', :month) AND strftime('%Y', t.timestamp / 1000, 'unixepoch') = printf('%d', :year)
        LEFT JOIN budgets b ON c.id = b.categoryId AND b.month = :month AND b.year = :year
        WHERE c.name NOT IN ('Transfer', 'Income')
        GROUP BY c.id
    """)
    fun getCategoryBudgetProgress(month: Int, year: Int): Flow<List<CategoryBudgetProgress>>
}
