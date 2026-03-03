package com.ics2300.pocketbudget.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.TransactionEntity

class TransactionAdapter(
    private var transactions: List<TransactionEntity>,
    private val onItemClick: (TransactionEntity) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val merchant: TextView = view.findViewById(R.id.tvMerchantName)
        val amount: TextView = view.findViewById(R.id.tvAmount)
        val date: TextView = view.findViewById(R.id.tvDate)
        val category: TextView = view.findViewById(R.id.tvCategoryLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = transactions[position]
        holder.merchant.text = item.senderOrReceiver
        holder.amount.text = "KES ${item.amount}"
        holder.date.text = item.dateTime
        holder.category.text = item.category ?: "Uncategorized"
        
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = transactions.size

    fun updateList(newList: List<TransactionEntity>) {
        transactions = newList
        notifyDataSetChanged()
    }
}