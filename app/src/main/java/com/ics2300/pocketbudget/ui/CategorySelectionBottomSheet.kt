package com.ics2300.pocketbudget.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.databinding.DialogCategorySelectionBinding

class CategorySelectionBottomSheet(
    private val categories: List<CategoryEntity>,
    private val selectedCategoryId: Int = -1,
    private val partyName: String? = null,
    private val onCategorySelected: (CategoryEntity, Boolean) -> Unit
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
            if (partyName != null) {
                showBulkConfirmDialog(category)
            } else {
                onCategorySelected(category, false)
                dismiss()
            }
        }

        binding.recyclerCategories.adapter = adapter
        adapter.submitList(categories)
    }

    private fun showBulkConfirmDialog(category: CategoryEntity) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_bulk_categorize_confirm, null)
            
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
            
        dialogView.findViewById<TextView>(R.id.text_bulk_message).text = 
            "We found similar transactions from '$partyName'. Would you like to categorize all of them as '${category.name}'?"
            
        dialogView.findViewById<View>(R.id.btn_just_one).setOnClickListener {
            onCategorySelected(category, false)
            dialog.dismiss()
            dismiss()
        }
        
        dialogView.findViewById<View>(R.id.btn_bulk_all).setOnClickListener {
            onCategorySelected(category, true)
            dialog.dismiss()
            dismiss()
        }
        
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CategorySelectionBottomSheet"
    }
}
