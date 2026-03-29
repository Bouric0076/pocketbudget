package com.ics2300.pocketbudget.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.databinding.ItemBudgetBinding
import com.ics2300.pocketbudget.utils.AnalyticsUtils
import java.util.Calendar

import com.ics2300.pocketbudget.utils.CategoryUtils
import com.ics2300.pocketbudget.utils.CurrencyFormatter

class BudgetAdapter(
    private val onEditClick: (BudgetItem) -> Unit,
    private val onBudgetSet: (BudgetItem) -> Unit = {} // If we want to support setting budget from adapter
) : ListAdapter<BudgetItem, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

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
            
            if (item.limitAmount > 0) {
                binding.textLimitAmount.text = "/ ${CurrencyFormatter.formatKsh(item.limitAmount)}"
                
                val percentage = ((item.spentAmount / item.limitAmount) * 100).toInt()
                binding.textPercentage.text = "$percentage%"
                binding.progressCategory.progress = percentage.coerceIn(0, 100)
                
                val remaining = item.limitAmount - item.spentAmount
                if (remaining >= 0) {
                    binding.textRemainingCategory.text = "${CurrencyFormatter.formatKsh(remaining)} remaining"
                    binding.textRemainingCategory.setTextColor(CategoryUtils.getColor(item.colorHex))
                    binding.textPercentage.setTextColor(CategoryUtils.getColor(item.colorHex))
                    binding.progressCategory.progressTintList = android.content.res.ColorStateList.valueOf(CategoryUtils.getColor(item.colorHex))
                } else {
                    binding.textRemainingCategory.text = "Over by ${CurrencyFormatter.formatKsh(-remaining)}"
                    binding.textRemainingCategory.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark))
                    binding.textPercentage.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark))
                    binding.progressCategory.progressTintList = ContextCompat.getColorStateList(binding.root.context, android.R.color.holo_red_dark)
                }
                
                // Set "Spent vs. Remaining" text explicitly
                binding.textSpentAmount.text = "${CurrencyFormatter.formatKsh(item.spentAmount)} spent"
                binding.textLimitAmount.text = " of ${CurrencyFormatter.formatKsh(item.limitAmount)}"
                
                // Forecast Check
                val calendar = Calendar.getInstance()
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                
                val forecast = AnalyticsUtils.forecastEndOfMonth(item.spentAmount, day, totalDays, item.limitAmount)
                
                if (forecast.isOverBudget && remaining > 0) {
                    // It's not over budget YET, but predicted to be
                    binding.textForecastAlert.visibility = android.view.View.VISIBLE
                    binding.textForecastAlert.text = "⚠️ On track to overspend by ${CurrencyFormatter.formatKsh(forecast.predictedTotal - item.limitAmount)}"
                } else {
                    binding.textForecastAlert.visibility = android.view.View.GONE
                }

            } else {
                // No Budget Set
                binding.textSpentAmount.text = CurrencyFormatter.formatKsh(item.spentAmount)
                binding.textLimitAmount.text = " / No Limit"
                binding.textPercentage.text = ""
                binding.progressCategory.progress = 0
                binding.textRemainingCategory.text = "Tap to set budget"
                binding.textRemainingCategory.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_secondary))
                binding.textForecastAlert.visibility = android.view.View.GONE
            }
            
            // Icon logic
            binding.imgCategoryIcon.setImageResource(CategoryUtils.getIconResId(item.iconName))
            binding.imgCategoryIcon.setColorFilter(CategoryUtils.getColor(item.colorHex))
            // Optional: set background tint if it's a shape
            // binding.imgCategoryIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(CategoryUtils.getColor(item.colorHex))
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
