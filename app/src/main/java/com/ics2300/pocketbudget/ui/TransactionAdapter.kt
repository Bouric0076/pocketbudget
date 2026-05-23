package com.ics2300.pocketbudget.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.databinding.ItemTransactionBinding
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import com.ics2300.pocketbudget.utils.SecurityUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val onTransactionClick: (TransactionEntity) -> Unit = {}
) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(TransactionDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM   = 1
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is TransactionListItem.Header      -> TYPE_HEADER
        is TransactionListItem.Transaction -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction_header, parent, false)
            HeaderViewHolder(view as TextView)
        } else {
            val binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            TransactionViewHolder(binding, onTransactionClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder      -> holder.bind((getItem(position) as TransactionListItem.Header).title)
            is TransactionViewHolder -> holder.bind(getItem(position) as TransactionListItem.Transaction)
        }
    }

    // ── Header ────────────────────────────────────────────────────────────────

    class HeaderViewHolder(private val tv: TextView) : RecyclerView.ViewHolder(tv) {
        fun bind(title: String) { tv.text = title }
    }

    // ── Transaction row ───────────────────────────────────────────────────────

    class TransactionViewHolder(
        private val b: ItemTransactionBinding,
        private val onClick: (TransactionEntity) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {

        private val ctx get() = b.root.context

        fun bind(item: TransactionListItem.Transaction) {
            val tx       = item.transaction
            val category = item.category
            val isPrivacy = SecurityUtils.isPrivacyModeEnabled(ctx)

            b.root.setOnClickListener { onClick(tx) }

            // ── Party name ────────────────────────────────────────────────
            b.textPartyName.text = if (isPrivacy) "••••••••" else tx.partyName

            // ── Date ──────────────────────────────────────────────────────
            b.textDate.text = formatTimestamp(tx.timestamp)

            // ── Amount ────────────────────────────────────────────────────
            val isIncome = tx.type.lowercase() in listOf("received", "deposit")
            val prefix   = if (isIncome) "+ " else "- "
            b.textAmount.text = prefix + CurrencyFormatter.formatKsh(tx.amount, isPrivacy)
            b.textAmount.setTextColor(
                ctx.getColor(if (isIncome) R.color.brand_dark_green else R.color.status_error)
            )

            // ── Icon background + tint ────────────────────────────────────
            val (iconRes, iconBgColor, iconTintColor) = when {
                isIncome -> Triple(
                    R.drawable.ic_arrow_down,
                    R.color.avatar_bg_green,
                    R.color.brand_dark_green
                )
                category == null -> Triple(
                    R.drawable.ic_help,
                    R.color.avatar_bg_gray,
                    R.color.text_tertiary
                )
                else -> Triple(
                    R.drawable.ic_arrow_up,
                    R.color.avatar_bg_red,
                    R.color.status_error
                )
            }
            b.imgTransactionIcon.setImageResource(iconRes)
            b.imgTransactionIcon.backgroundTintList =
                ColorStateList.valueOf(ctx.getColor(iconBgColor))
            b.imgTransactionIcon.setColorFilter(ctx.getColor(iconTintColor))

            // ── Category pill (text_type repurposed) ──────────────────────
            when {
                isPrivacy -> {
                    b.textType.text = "••••"
                    b.textType.backgroundTintList =
                        ColorStateList.valueOf(ctx.getColor(R.color.surface_page))
                    b.textType.setTextColor(ctx.getColor(R.color.text_tertiary))
                }
                category != null -> {
                    b.textType.text = category.name
                    val (pillBg, pillText) = categoryPillColors(category.name)
                    b.textType.backgroundTintList =
                        ColorStateList.valueOf(ctx.getColor(pillBg))
                    b.textType.setTextColor(ctx.getColor(pillText))
                }
                else -> {
                    // Uncategorized — amber nudge
                    b.textType.text = "Tap to tag"
                    b.textType.backgroundTintList =
                        ColorStateList.valueOf(ctx.getColor(R.color.onboarding_chip_amber))
                    b.textType.setTextColor(ctx.getColor(R.color.brand_amber))
                }
            }
        }

        // ── Helpers ───────────────────────────────────────────────────────

        private fun formatTimestamp(ts: Long): String {
            val now     = System.currentTimeMillis()
            val diff    = now - ts
            val oneDay  = 86_400_000L
            return when {
                diff < oneDay -> {
                    // Today — show time only
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
                        .let { "Today, $it" }
                }
                diff < 2 * oneDay -> {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
                        .let { "Yesterday, $it" }
                }
                diff < 7 * oneDay -> {
                    SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(ts))
                }
                else -> {
                    SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(ts))
                }
            }
        }

        private fun categoryPillColors(name: String): Pair<Int, Int> = when (name.lowercase()) {
            "food", "groceries", "eating out" ->
                Pair(R.color.onboarding_chip_green,  R.color.brand_dark_green)
            "transport", "uber", "matatu" ->
                Pair(R.color.onboarding_chip_indigo, R.color.brand_indigo)
            "income", "salary" ->
                Pair(R.color.onboarding_chip_green,  R.color.brand_dark_green)
            "utilities", "electricity", "water" ->
                Pair(R.color.onboarding_chip_amber,  R.color.brand_amber)
            "entertainment", "subscriptions" ->
                Pair(R.color.avatar_bg_red,          R.color.status_error)
            else ->
                Pair(R.color.surface_page,           R.color.text_secondary)
        }
    }

    // ── DiffCallback ──────────────────────────────────────────────────────────

    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {
        override fun areItemsTheSame(old: TransactionListItem, new: TransactionListItem): Boolean {
            return when {
                old is TransactionListItem.Transaction && new is TransactionListItem.Transaction ->
                    old.transaction.id == new.transaction.id
                old is TransactionListItem.Header && new is TransactionListItem.Header ->
                    old.title == new.title
                else -> false
            }
        }

        override fun areContentsTheSame(old: TransactionListItem, new: TransactionListItem): Boolean {
            return when {
                old is TransactionListItem.Transaction && new is TransactionListItem.Transaction ->
                    old.transaction == new.transaction && old.category == new.category
                old is TransactionListItem.Header && new is TransactionListItem.Header ->
                    old.title == new.title
                else -> false
            }
        }
    }
}