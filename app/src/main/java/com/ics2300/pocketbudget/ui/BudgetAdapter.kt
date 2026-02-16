package com.ics2300.pocketbudget.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.databinding.ItemBudgetBinding
import com.ics2300.pocketbudget.utils.CurrencyFormatter

class BudgetAdapter(private val onEditClick: (BudgetItem) -> Unit) : 
    ListAdapter<BudgetItem, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetViewHolder(binding, onEditClick)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BudgetViewHolder(
        private val binding: ItemBudgetBinding,
        private val onEditClick: (BudgetItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BudgetItem) {
            binding.root.setOnClickListener { onEditClick(item) }
            
            binding.textCategoryName.text = item.categoryName
            binding.textSpentAmount.text = CurrencyFormatter.formatKsh(item.spentAmount)
            binding.textLimitAmount.text = CurrencyFormatter.formatKsh(item.limitAmount)
            
            val remaining = item.limitAmount - item.spentAmount
            binding.textRemainingCategory.text = if (remaining >= 0) {
                "${CurrencyFormatter.formatKsh(remaining)} left"
            } else {
                "Over by ${CurrencyFormatter.formatKsh(-remaining)}"
            }
            
            val percentage = if (item.limitAmount > 0) {
                ((item.spentAmount / item.limitAmount) * 100).toInt()
            } else {
                if (item.spentAmount > 0) 100 else 0
            }
            binding.textPercentage.text = "$percentage%"
            binding.progressCategory.progress = percentage.coerceIn(0, 100)
            
            // Color logic
            val context = binding.root.context
            // If over 100% OR (limit is 0 and spent > 0)
            if (percentage > 100 || (item.limitAmount == 0.0 && item.spentAmount > 0)) {
                binding.textRemainingCategory.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                binding.textPercentage.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            } else {
                binding.textRemainingCategory.setTextColor(ContextCompat.getColor(context, R.color.brand_secondary_green))
                binding.textPercentage.setTextColor(ContextCompat.getColor(context, R.color.brand_dark_green))
            }
            
            // Icon logic (Simple mapping for now)
            val iconRes = when (item.categoryName.lowercase()) {
                "food" -> android.R.drawable.ic_menu_my_calendar // Placeholder
                "transport" -> android.R.drawable.ic_menu_directions
                "shopping" -> android.R.drawable.ic_menu_gallery
                else -> android.R.drawable.ic_menu_my_calendar
            }
            binding.imgCategoryIcon.setImageResource(iconRes)
        }
    }

    class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetItem>() {
        override fun areItemsTheSame(oldItem: BudgetItem, newItem: BudgetItem): Boolean {
            return oldItem.categoryName == newItem.categoryName
        }

        override fun areContentsTheSame(oldItem: BudgetItem, newItem: BudgetItem): Boolean {
            return oldItem == newItem
        }
    }
}
