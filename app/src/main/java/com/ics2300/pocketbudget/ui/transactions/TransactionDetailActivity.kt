package com.ics2300.pocketbudget.ui.transactions

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ics2300.pocketbudget.R

class TransactionDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)

        // 1. Get references to UI
        val tvMerchant = findViewById<TextView>(R.id.detailMerchant)
        val tvAmount = findViewById<TextView>(R.id.detailAmount)
        val tvDate = findViewById<TextView>(R.id.detailDate)
        val spinner = findViewById<Spinner>(R.id.categorySpinner)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // 2. Load "Extracted Info" (Currently using dummy data for UI deliverable)
        tvMerchant.text = "Merchant: KFC"
        tvAmount.text = "Amount: KES 1200.00"
        tvDate.text = "Date: 2026-02-16"

        // 3. Setup Category Dropdown (Member 4's logic categories)
        val categories = arrayOf("Food", "Transport", "Airtime", "Rent", "School", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.adapter = adapter

        // 4. Handle Save
        btnSave.setOnClickListener {
            val selected = spinner.selectedItem.toString()
            Toast.makeText(this, "Category updated to: $selected", Toast.LENGTH_SHORT).show()
            finish() // Return to list
        }
    }
}