package com.ics2300.pocketbudget.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId")
    fun getTransactionsByCategory(categoryId: Int): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE (timestamp BETWEEN :startDate AND :endDate) AND categoryId = :categoryId")
    fun getTransactionsByDateRangeAndCategory(startDate: Long, endDate: Long, categoryId: Int): Flow<List<TransactionEntity>>
}
