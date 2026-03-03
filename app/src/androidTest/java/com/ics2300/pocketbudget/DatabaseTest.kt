package com.ics2300.pocketbudget  

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ics2300.pocketbudget.data.AppDatabase
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.TransactionDao
import com.ics2300.pocketbudget.data.TransactionEntity
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
    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.transactionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadTransaction() = runBlocking {
        val category = CategoryEntity(id = 1, name = "Food", keywords = "lunch,dinner")
        dao.insertCategory(category)

        val transaction = TransactionEntity(
            transactionId = "TXN123",
            amount = 50.0,
            type = "Expense",
            partyName = "Restaurant",
            timestamp = System.currentTimeMillis(),
            categoryId = 1
        )
        dao.insertTransaction(transaction)

        val allTransactions = dao.getAllTransactions().first()
        assertEquals(allTransactions[0].transactionId, "TXN123")
    }

    @Test
    @Throws(Exception::class)
    fun avoidDuplicateTransactionId() = runBlocking {
        val transaction1 = TransactionEntity(
            transactionId = "DUP123",
            amount = 10.0,
            type = "Expense",
            partyName = "Shop",
            timestamp = System.currentTimeMillis(),
            categoryId = null
        )
        val transaction2 = TransactionEntity(
            transactionId = "DUP123",
            amount = 20.0,
            type = "Expense",
            partyName = "Shop",
            timestamp = System.currentTimeMillis(),
            categoryId = null
        )

        dao.insertTransaction(transaction1)
        dao.insertTransaction(transaction2)

        val allTransactions = dao.getAllTransactions().first()
        assertEquals(1, allTransactions.size)
        assertEquals(10.0, allTransactions[0].amount, 0.0)
    }

    @Test
    @Throws(Exception::class)
    fun fetchByDateRange() = runBlocking {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        
        val t1 = TransactionEntity(transactionId = "T1", amount = 10.0, type = "E", partyName = "P", timestamp = now - 2 * day, categoryId = null)
        val t2 = TransactionEntity(transactionId = "T2", amount = 20.0, type = "E", partyName = "P", timestamp = now - day, categoryId = null)
        val t3 = TransactionEntity(transactionId = "T3", amount = 30.0, type = "E", partyName = "P", timestamp = now, categoryId = null)

        dao.insertTransaction(t1)
        dao.insertTransaction(t2)
        dao.insertTransaction(t3)

        val results = dao.getTransactionsByDateRange(now - 1.5 * day.toDouble().toLong(), now).first()
        assertEquals(2, results.size)
    }
}
