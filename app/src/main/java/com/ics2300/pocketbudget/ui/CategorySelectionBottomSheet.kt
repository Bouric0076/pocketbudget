package com.ics2300.pocketbudget.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.databinding.DialogCategorySelectionBinding

class CategorySelectionBottomSheet(
    private val categories: List<CategoryEntity>,
    private val selectedCategoryId: Int = -1,
    private val onCategorySelected: (CategoryEntity) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogCategorySelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCategorySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CategorySelectionAdapter(selectedCategoryId) { category ->
            onCategorySelected(category)
            dismiss()
        }

        binding.recyclerCategories.adapter = adapter
        adapter.submitList(categories)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CategorySelectionBottomSheet"
    }
}
