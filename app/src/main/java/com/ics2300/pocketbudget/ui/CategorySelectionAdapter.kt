package com.ics2300.pocketbudget.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.databinding.ItemCategorySelectionBinding

class CategorySelectionAdapter(
    private var selectedId: Int = -1,
    private val onCategorySelected: (CategoryEntity) -> Unit
) : ListAdapter<CategoryEntity, CategorySelectionAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    fun setSelected(id: Int) {
        selectedId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategorySelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), selectedId)
    }

    inner class CategoryViewHolder(private val binding: ItemCategorySelectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: CategoryEntity, selectedId: Int) {
            binding.root.setOnClickListener {
                onCategorySelected(category)
                setSelected(category.id)
            }
            binding.textCategoryName.text = category.name
            binding.textCategoryInitial.text = category.name.take(1).uppercase()
            
            if (category.id == selectedId) {
                binding.containerIcon.setBackgroundResource(R.drawable.bg_circle_selected)
            } else {
                binding.containerIcon.setBackgroundResource(R.drawable.bg_circle_button)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryEntity>() {
        override fun areItemsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}
