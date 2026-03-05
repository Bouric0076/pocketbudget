package com.ics2300.pocketbudget.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.databinding.FragmentBudgetBinding
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModel
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModelFactory
import com.ics2300.pocketbudget.utils.CurrencyFormatter

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory((requireActivity().application as MainApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val adapter = BudgetAdapter({ budgetItem ->
            showSetBudgetDialog(budgetItem)
        })
        binding.recyclerviewBudget.adapter = adapter
        binding.recyclerviewBudget.layoutManager = LinearLayoutManager(context)

        // Observe real budget progress
        viewModel.budgetProgress.observe(viewLifecycleOwner) { progressList ->
            val budgetItems = progressList.map { progress ->
                BudgetItem(
                    categoryId = progress.categoryId,
                    categoryName = progress.categoryName,
                    spentAmount = progress.totalSpent,
                    limitAmount = progress.budgetAmount,
                    iconName = progress.iconName,
                    colorHex = progress.colorHex
                )
            }
            adapter.submitList(budgetItems)
            
            // Update Summary
            val totalSpent = budgetItems.sumOf { it.spentAmount }
            val totalLimit = budgetItems.sumOf { it.limitAmount }
            
            binding.textTotalSpent.text = CurrencyFormatter.formatKsh(totalSpent)
            binding.textTotalLimit.text = " / ${CurrencyFormatter.formatKsh(totalLimit)}"
            
            val totalPercentage = if (totalLimit > 0) ((totalSpent / totalLimit) * 100).toInt() else 0
            binding.progressTotal.progress = totalPercentage.coerceIn(0, 100)
            
            val remaining = totalLimit - totalSpent
            binding.textRemaining.text = "${CurrencyFormatter.formatKsh(remaining)} remaining"
        }
        
        binding.btnAddBudget.setOnClickListener {
             Toast.makeText(context, "Tap a category card to set its budget", Toast.LENGTH_LONG).show()
        }

        return root
    }

    private fun showSetBudgetDialog(item: BudgetItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_budget, null)
        val textCategory = dialogView.findViewById<TextView>(R.id.text_dialog_category)
        val editAmount = dialogView.findViewById<EditText>(R.id.edit_amount)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

        textCategory.text = "For ${item.categoryName}"
        if (item.limitAmount > 0) {
            editAmount.setText(item.limitAmount.toInt().toString())
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Transparent background for rounded corners
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnSave.setOnClickListener {
            val amountStr = editAmount.text.toString()
            if (amountStr.isNotBlank()) {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                viewModel.setBudget(item.categoryId, amount)
                dialog.dismiss()
            } else {
                 editAmount.error = "Enter an amount"
            }
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
