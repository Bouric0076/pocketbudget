package com.ics2300.pocketbudget.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.databinding.DialogAddTransactionBinding
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModel
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class AddTransactionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by activityViewModels()
    
    private var selectedCategoryId: Int? = null
    private var availableCategories: List<CategoryEntity> = emptyList()
    private var userSelectedCategory = false
    private var isIncome = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToggle()
        setupCategories()
        setupRecurringOptions()
        setupSaveButton()
        binding.btnClose.setOnClickListener { dismiss() }
        viewModel.ensureCategoriesLoaded()
    }

    private fun setupRecurringOptions() {
        val frequencies = arrayOf("Daily", "Weekly", "Monthly", "Yearly")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencies)
        (binding.editFrequency as? AutoCompleteTextView)?.setAdapter(adapter)

        binding.switchRecurring.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutFrequency.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateSaveButtonLabel()
        }

        updateSaveButtonLabel()
    }

    private fun setupToggle() {
        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isIncome = checkedId == R.id.btn_income
                userSelectedCategory = false
                applyDefaultCategory()
                updateCategoryControls()
                updateSaveButtonLabel()
            }
        }

        binding.toggleType.check(R.id.btn_expense)
        updateCategoryControls()
    }

    private fun setupCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            availableCategories = categories
            populateCategoryChips(categories)

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories.map { it.name }
            )
            binding.editCategory.setAdapter(adapter)

            if (
                selectedCategoryId == null ||
                categories.none { it.id == selectedCategoryId }
            ) {
                applyDefaultCategory()
            }
            updateCategoryControls()
            
            binding.editCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = categories[position].id
                userSelectedCategory = true
                binding.layoutCategory.error = null
                syncCheckedCategoryChip()
            }
        }
    }

    private fun populateCategoryChips(categories: List<CategoryEntity>) {
        binding.chipGroupCategories.removeAllViews()

        categories.take(5).forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.name
                isCheckable = true
                isClickable = true
                setTextColor(requireContext().getColor(R.color.text_primary))
                chipBackgroundColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(
                        requireContext().getColor(R.color.onboarding_chip_green),
                        requireContext().getColor(R.color.white)
                    )
                )
                chipStrokeWidth = 1f
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#E1E6E2"))
                shapeAppearanceModel =
                    shapeAppearanceModel.toBuilder()
                        .setAllCornerSizes(20f)
                        .build()
                textSize = 11f
                tag = category.id
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedCategoryId = category.id
                        userSelectedCategory = true
                        binding.editCategory.setText(category.name, false)
                        binding.layoutCategory.error = null
                    }
                }
            }
            binding.chipGroupCategories.addView(chip)
        }

        syncCheckedCategoryChip()
    }

    private fun applyDefaultCategory() {
        val defaultCategory = if (isIncome) {
            availableCategories.firstOrNull {
                it.name.equals("Income", ignoreCase = true)
            }
        } else {
            availableCategories.firstOrNull {
                it.name.equals("Uncategorized", ignoreCase = true)
            }
        } ?: if (isIncome) {
            null
        } else {
            availableCategories.firstOrNull()
        }

        selectedCategoryId = defaultCategory?.id
        binding.editCategory.setText(defaultCategory?.name.orEmpty(), false)
        syncCheckedCategoryChip()
    }

    private fun updateCategoryControls() {
        binding.textCategoryLabel.visibility = if (isIncome) View.GONE else View.VISIBLE
        binding.chipGroupCategories.visibility = if (isIncome) View.GONE else View.VISIBLE
        binding.layoutCategory.visibility = if (isIncome) View.GONE else View.VISIBLE
    }

    private fun syncCheckedCategoryChip() {
        val selectedId = selectedCategoryId

        for (index in 0 until binding.chipGroupCategories.childCount) {
            val chip = binding.chipGroupCategories.getChildAt(index) as? Chip
            chip?.setOnCheckedChangeListener(null)
            chip?.isChecked = chip?.tag == selectedId
            chip?.setOnCheckedChangeListener { button, checked ->
                if (checked) {
                    val categoryId = button.tag as? Int ?: return@setOnCheckedChangeListener
                    val category = availableCategories.firstOrNull { it.id == categoryId }
                        ?: return@setOnCheckedChangeListener

                    selectedCategoryId = category.id
                    userSelectedCategory = true
                    binding.editCategory.setText(category.name, false)
                    binding.layoutCategory.error = null
                }
            }
        }
    }

    private fun updateSaveButtonLabel() {
        val isRecurring = binding.switchRecurring.isChecked
        binding.btnSaveTransaction.text = when {
            isRecurring -> "Set Recurring"
            isIncome -> "Save Income"
            else -> "Save Expense"
        }

        val color = if (isIncome) {
            R.color.brand_dark_green
        } else {
            R.color.status_error
        }
        binding.btnSaveTransaction.backgroundTintList =
            ColorStateList.valueOf(requireContext().getColor(color))
        binding.btnSaveTransaction.setTextColor(requireContext().getColor(R.color.white))

        listOf(binding.btnIncome, binding.btnExpense).forEach { button ->
            button.setTextColor(requireContext().getColor(R.color.text_primary))
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveTransaction.setOnClickListener {
            val amountStr = binding.editAmount.text.toString()
            val note = binding.editNote.text.toString()
            binding.layoutAmount.error = null
            binding.layoutCategory.error = null
            binding.layoutFrequency.error = null
            
            if (amountStr.isBlank()) {
                binding.layoutAmount.error = "Enter amount"
                return@setOnClickListener
            }
            
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            if (amount == 0.0) {
                binding.layoutAmount.error = "Amount cannot be zero"
                return@setOnClickListener
            }

            if (note.isBlank()) {
                binding.layoutNote.error = null
            }

            val categoryId = selectedCategoryId
            if (categoryId == null) {
                if (isIncome) {
                    Toast.makeText(context, "Income category is not ready yet. Try again.", Toast.LENGTH_SHORT).show()
                } else {
                    binding.layoutCategory.error = "Choose a category"
                }
                return@setOnClickListener
            }

            val isRecurring = binding.switchRecurring.isChecked
            
            if (isRecurring) {
                val frequency = binding.editFrequency.text.toString()
                if (frequency.isBlank()) {
                    binding.layoutFrequency.error = "Choose a frequency"
                    return@setOnClickListener
                }

                val description = if (note.isNotBlank()) note else if (isIncome) "Recurring Income" else "Recurring Expense"
                
                viewModel.addRecurringTransaction(
                    amount = amount,
                    description = description,
                    categoryId = categoryId,
                    type = if (isIncome) "Deposit" else "Withdrawal",
                    frequency = frequency,
                    startDate = System.currentTimeMillis()
                )
                Toast.makeText(context, "Recurring Transaction Set", Toast.LENGTH_SHORT).show()
            } else {
                val transaction = TransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    partyName = if (note.isNotBlank()) note else if (isIncome) "Manual Income" else "Manual Expense",
                    amount = amount,
                    type = if (isIncome) "Deposit" else "Withdrawal",
                    timestamp = System.currentTimeMillis(),
                    categoryId = categoryId
                )
                
                viewModel.addTransaction(transaction)
                Toast.makeText(context, "Transaction Added", Toast.LENGTH_SHORT).show()
            }
            
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddTransactionBottomSheet"
    }
}
