package com.ics2300.pocketbudget.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.databinding.ItemTransactionBinding
import com.ics2300.pocketbudget.utils.CategoryUtils
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.ics2300.pocketbudget.utils.SecurityUtils

class TransactionAdapter(private val onTransactionClick: (TransactionEntity) -> Unit = {}) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(TransactionDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionListItem.Header -> TYPE_HEADER
            is TransactionListItem.Transaction -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction_header, parent, false)
            HeaderViewHolder(view as TextView)
        } else {
            val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            TransactionViewHolder(binding, onTransactionClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderViewHolder -> holder.bind((item as TransactionListItem.Header).title)
            is TransactionViewHolder -> holder.bind((item as TransactionListItem.Transaction))
        }
    }

    class HeaderViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(title: String) {
            textView.text = title
        }
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private val onClick: (TransactionEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transactionItem: TransactionListItem.Transaction) {
            val transaction = transactionItem.transaction
            val category = transactionItem.category

            binding.root.setOnClickListener { onClick(transaction) }
            binding.textPartyName.text = transaction.partyName
            
            // Respect Privacy Mode
            val isPrivacyMode = SecurityUtils.isPrivacyModeEnabled(binding.root.context)
            binding.textAmount.text = CurrencyFormatter.formatKsh(transaction.amount, isPrivacyMode)
            
            val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            binding.textDate.text = sdf.format(Date(transaction.timestamp))
            binding.textType.text = transaction.type
            
            // Set icon and colors based on type
            when (transaction.type.lowercase()) {
                "received", "deposit" -> {
                    binding.imgTransactionIcon.setImageResource(android.R.drawable.stat_sys_download)
                    binding.imgTransactionIcon.setColorFilter(binding.root.context.getColor(R.color.brand_dark_green))
                    binding.textAmount.setTextColor(binding.root.context.getColor(R.color.brand_dark_green))
                }
                else -> {
                    binding.imgTransactionIcon.setImageResource(android.R.drawable.ic_menu_send)
                    binding.imgTransactionIcon.setColorFilter(binding.root.context.getColor(R.color.status_error))
                    binding.textAmount.setTextColor(binding.root.context.getColor(R.color.text_primary))
                }
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {
        override fun areItemsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
            return if (oldItem is TransactionListItem.Transaction && newItem is TransactionListItem.Transaction) {
                oldItem.transaction.id == newItem.transaction.id
            } else if (oldItem is TransactionListItem.Header && newItem is TransactionListItem.Header) {
                oldItem.title == newItem.title
            } else {
                false
            }
        }

        override fun areContentsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
            return if (oldItem is TransactionListItem.Transaction && newItem is TransactionListItem.Transaction) {
                oldItem.transaction == newItem.transaction
            } else if (oldItem is TransactionListItem.Header && newItem is TransactionListItem.Header) {
                oldItem.title == newItem.title
            } else {
                false
            }
        }
    }
}
