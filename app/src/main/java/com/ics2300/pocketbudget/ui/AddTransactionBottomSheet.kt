package com.ics2300.pocketbudget.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.databinding.DialogAddTransactionBinding
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModel
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModelFactory
import androidx.fragment.app.activityViewModels
import java.util.UUID

class AddTransactionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by activityViewModels {
        DashboardViewModelFactory((requireActivity().application as MainApplication).repository)
    }
    
    private var selectedCategoryId: Int = 1 // Default Uncategorized
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
    }

    private fun setupRecurringOptions() {
        val frequencies = arrayOf("Daily", "Weekly", "Monthly", "Yearly")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencies)
        (binding.editFrequency as? AutoCompleteTextView)?.setAdapter(adapter)

        binding.switchRecurring.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutFrequency.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupToggle() {
        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isIncome = checkedId == R.id.btn_income
            }
        }
    }

    private fun setupCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories.map { it.name })
            binding.editCategory.setAdapter(adapter)
            
            binding.editCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = categories[position].id
            }
        }
        viewModel.loadCategories()
    }

    private fun setupSaveButton() {
        binding.btnSaveTransaction.setOnClickListener {
            val amountStr = binding.editAmount.text.toString()
            val note = binding.editNote.text.toString()
            
            if (amountStr.isBlank()) {
                binding.layoutAmount.error = "Enter amount"
                return@setOnClickListener
            }
            
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val isRecurring = binding.switchRecurring.isChecked
            
            if (isRecurring) {
                val frequency = binding.editFrequency.text.toString()
                val description = if (note.isNotBlank()) note else if (isIncome) "Recurring Income" else "Recurring Expense"
                
                viewModel.addRecurringTransaction(
                    amount = amount,
                    description = description,
                    categoryId = selectedCategoryId,
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
                    categoryId = selectedCategoryId
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
