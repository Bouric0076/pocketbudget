package com.ics2300.pocketbudget.ui.transactions

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.TransactionEntity

class TransactionsFragment : Fragment(R.layout.fragment_transactions) {

    private lateinit var adapter: TransactionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvTransactions)
        
        // 1. Dummy data for Member 5 Deliverable
        val dummyData = listOf(
            TransactionEntity(id=1, transactionId="MP12345", amount=150.0, dateTime="2026-02-16", senderOrReceiver="KFC", type="Paybill", category="Food", rawSms=""),
            TransactionEntity(id=2, transactionId="MP67890", amount=2000.0, dateTime="2026-02-15", senderOrReceiver="Petrol Station", type="Buy Goods", category="Transport", rawSms=""),
            TransactionEntity(id=3, transactionId="MP11223", amount=500.0, dateTime="2026-02-14", senderOrReceiver="Zuku", type="Paybill", category="Utilities", rawSms="")
        )

        // 2. Setup Adapter with the click listener to open Detail Activity
        adapter = TransactionAdapter(dummyData) { transaction ->
            // This logic allows clicking a list item to go to the detail screen
            val intent = Intent(requireContext(), TransactionDetailActivity::class.java)
            // You can pass the merchant name or ID to the next screen if you want:
            intent.putExtra("MERCHANT_NAME", transaction.senderOrReceiver)
            intent.putExtra("AMOUNT", transaction.amount)
            startActivity(intent)
        }

        // 3. Set LayoutManager and Adapter
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
    }
}