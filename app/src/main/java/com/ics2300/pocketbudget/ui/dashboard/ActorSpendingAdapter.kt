package com.ics2300.pocketbudget.ui.dashboard

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.ActorSpending
import com.ics2300.pocketbudget.databinding.ItemActorSpendingBinding
import com.ics2300.pocketbudget.utils.CurrencyFormatter

class ActorSpendingAdapter(private var isPrivacy: Boolean = false) : ListAdapter<ActorSpending, ActorSpendingAdapter.ViewHolder>(DiffCallback) {

    fun setPrivacyMode(enabled: Boolean) {
        if (isPrivacy != enabled) {
            isPrivacy = enabled
            notifyDataSetChanged()
        }
    }

    class ViewHolder(val binding: ItemActorSpendingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(actor: ActorSpending, isPrivacy: Boolean, position: Int, maxAmount: Double) {
            val context = itemView.context
            val name = actor.partyName
            val colors = listOf(
                Pair(R.color.avatar_bg_green, R.color.avatar_text_green),
                Pair(R.color.avatar_bg_indigo, R.color.avatar_text_indigo),
                Pair(R.color.avatar_bg_amber, R.color.avatar_text_amber),
                Pair(R.color.avatar_bg_red, R.color.avatar_text_red),
                Pair(R.color.avatar_bg_gray, R.color.avatar_text_gray)
            )
            val (bgColor, textColor) = colors[position % colors.size]

            binding.viewAvatarBg.backgroundTintList =
                ColorStateList.valueOf(context.getColor(bgColor))
            binding.textActorInitials.setTextColor(context.getColor(textColor))

            binding.textActorName.text = if (isPrivacy) "••••••" else name
            binding.textTotalAmount.text = CurrencyFormatter.formatKsh(actor.totalAmount, isPrivacy)

            binding.textActorInitials.text = if (isPrivacy) {
                "•"
            } else {
                name.trim().split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it[0].uppercaseChar().toString() }
                    .ifEmpty { "?" }
            }

            binding.textTxnCount.text =
                "${actor.transactionCount} txn${if (actor.transactionCount != 1) "s" else ""}"

            val pct = if (maxAmount <= 0.0) {
                0f
            } else {
                (actor.totalAmount / maxAmount).toFloat().coerceIn(0f, 1f)
            }

            binding.frameSpendBar.post {
                binding.viewSpendBar.layoutParams =
                    binding.viewSpendBar.layoutParams.apply {
                        width = (binding.frameSpendBar.width * pct).toInt()
                    }
                binding.viewSpendBar.requestLayout()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActorSpendingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val maxAmount = currentList.maxOfOrNull { it.totalAmount } ?: 1.0
        holder.bind(getItem(position), isPrivacy, position, maxAmount)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ActorSpending>() {
        override fun areItemsTheSame(oldItem: ActorSpending, newItem: ActorSpending): Boolean {
            return oldItem.partyName == newItem.partyName
        }

        override fun areContentsTheSame(oldItem: ActorSpending, newItem: ActorSpending): Boolean {
            return oldItem == newItem
        }
    }
}
