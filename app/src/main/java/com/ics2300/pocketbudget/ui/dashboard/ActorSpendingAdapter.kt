package com.ics2300.pocketbudget.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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

    class ViewHolder(private val binding: ItemActorSpendingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(actor: ActorSpending, isPrivacy: Boolean) {
            val name = actor.partyName
            binding.textActorName.text = if (isPrivacy) "****" else name
            binding.textTotalAmount.text = CurrencyFormatter.formatKsh(actor.totalAmount, isPrivacy)
            
            // Extract initials
            val initials = if (isPrivacy) {
                "**"
            } else {
                name.trim().split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .map { it[0].uppercaseChar() }
                    .joinToString("")
            }
            binding.textActorInitials.text = if (initials.isEmpty()) "?" else initials
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActorSpendingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), isPrivacy)
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
