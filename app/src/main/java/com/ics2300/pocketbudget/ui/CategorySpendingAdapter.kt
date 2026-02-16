package com.ics2300.pocketbudget.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.databinding.ItemCategorySpendingBinding
import com.ics2300.pocketbudget.utils.CurrencyFormatter

data class CategorySpendingItem(
    val name: String,
    val amount: Double,
    val percentage: Int,
    val color: Int
)

class CategorySpendingAdapter : ListAdapter<CategorySpendingItem, CategorySpendingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategorySpendingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemCategorySpendingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategorySpendingItem) {
            binding.textCategoryName.text = item.name
            binding.textAmount.text = CurrencyFormatter.formatKsh(item.amount)
            binding.textPercentage.text = "${item.percentage}%"
            binding.viewColorDot.setBackgroundColor(item.color)
            binding.progressBar.progress = item.percentage
            
            // Set progress color dynamically if needed, or use default
            binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(item.color)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategorySpendingItem>() {
        override fun areItemsTheSame(oldItem: CategorySpendingItem, newItem: CategorySpendingItem) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: CategorySpendingItem, newItem: CategorySpendingItem) = oldItem == newItem
    }
}
