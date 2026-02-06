package com.ics2300.pocketbudget

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ics2300.pocketbudget.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseFactory(context, AppDatabase::class.java).build()
        transactionDao = db.transactionDao()
        categoryDao = db.categoryDao()
        budgetDao = db.budgetDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeTransactionAndReadInList() = runBlocking {
        val transaction = TransactionEntity(
            transactionId = "TXN123",
            amount = 100.0,
            type = "Expense",
            partyName = "Supermarket",
            timestamp = System.currentTimeMillis(),
            categoryId = 1
        )
        transactionDao.insertTransaction(transaction)
        val allTransactions = transactionDao.getAllTransactions().first()
        assertEquals(allTransactions[0].transactionId, "TXN123")
    }

    @Test
    @Throws(Exception::class)
    fun avoidDuplicateTransactions() = runBlocking {
        val transaction1 = TransactionEntity(
            transactionId = "TXN_DUP",
            amount = 50.0,
            type = "Expense",
            partyName = "Store A",
            timestamp = System.currentTimeMillis(),
            categoryId = 1
        )
        val transaction2 = TransactionEntity(
            transactionId = "TXN_DUP",
            amount = 60.0,
            type = "Expense",
            partyName = "Store B",
            timestamp = System.currentTimeMillis(),
            categoryId = 1
        )
        transactionDao.insertTransaction(transaction1)
        transactionDao.insertTransaction(transaction2) // Should be ignored due to unique constraint and OnConflictStrategy.IGNORE
        
        val allTransactions = transactionDao.getAllTransactions().first()
        assertEquals(1, allTransactions.size)
        assertEquals(50.0, allTransactions[0].amount, 0.0)
    }

    @Test
    @Throws(Exception::class)
    fun fetchByDateRange() = runBlocking {
        val now = System.currentTimeMillis()
        val t1 = TransactionEntity(transactionId = "T1", amount = 10.0, type = "E", partyName = "P", timestamp = now - 10000, categoryId = 1)
        val t2 = TransactionEntity(transactionId = "T2", amount = 20.0, type = "E", partyName = "P", timestamp = now, categoryId = 1)
        val t3 = TransactionEntity(transactionId = "T3", amount = 30.0, type = "E", partyName = "P", timestamp = now + 10000, categoryId = 1)
        
        transactionDao.insertTransaction(t1)
        transactionDao.insertTransaction(t2)
        transactionDao.insertTransaction(t3)
        
        val rangeTransactions = transactionDao.getTransactionsByDateRange(now - 5000, now + 5000).first()
        assertEquals(1, rangeTransactions.size)
        assertEquals("T2", rangeTransactions[0].transactionId)
    }
}
