package com.ics2300.pocketbudget.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.databinding.FragmentTransactionsBinding
import com.ics2300.pocketbudget.ui.transactions.TransactionsViewModel
import com.ics2300.pocketbudget.ui.transactions.SortType
import com.ics2300.pocketbudget.ui.dashboard.TransactionFilter
import com.ics2300.pocketbudget.ui.budget.BudgetViewModel
import com.ics2300.pocketbudget.utils.TransactionGrouper

import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels()

    private val budgetViewModel: BudgetViewModel by viewModels()
    private var pendingCategoryTransactionId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        val adapter = TransactionAdapter { transaction ->
            showTransactionDetails(transaction)
        }
        binding.recyclerviewTransactions.adapter = adapter
        binding.recyclerviewTransactions.layoutManager = LinearLayoutManager(context)

        viewModel.transactionsWithCategory.observe(viewLifecycleOwner) { transactions ->
            transactions?.let { 
                val groupedList = TransactionGrouper.groupTransactions(it)
                adapter.submitList(groupedList) 
                handlePendingNotificationAction(it.map { item -> item.transaction })
                
                if (it.isEmpty()) {
                    binding.recyclerviewTransactions.visibility = View.GONE
                    binding.layoutEmptyState.root.visibility = View.VISIBLE
                    
                    val query = binding.editSearch.text.toString()
                    if (query.isNotBlank()) {
                         binding.layoutEmptyState.textEmptyTitle.text = "No Results Found"
                         binding.layoutEmptyState.textEmptyMessage.text = "We couldn't find any transactions matching '$query'."
                         binding.layoutEmptyState.btnEmptyAction.visibility = View.GONE
                    } else {
                         binding.layoutEmptyState.textEmptyTitle.text = "No Transactions"
                         binding.layoutEmptyState.textEmptyMessage.text = "Sync SMS or add transactions manually."
                         binding.layoutEmptyState.btnEmptyAction.visibility = View.GONE
                    }
                } else {
                    binding.recyclerviewTransactions.visibility = View.VISIBLE
                    binding.layoutEmptyState.root.visibility = View.GONE
                }
            }
        }
        
        setupSearch()
        setupChips()
        setupDateFilter()
        setupAdvancedFilter()
        setupSort()

        // Observe Categories from budgetViewModel to ensure they are loaded for details view
        budgetViewModel.categories.observe(viewLifecycleOwner) { /* Ensure LiveData is active */ }
        observeNotificationNavigation()

        return root
    }

    private fun observeNotificationNavigation() {
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("open_transaction_category_id")
            ?.observe(viewLifecycleOwner) { transactionId ->
                pendingCategoryTransactionId = transactionId
                val currentTransactions = viewModel.transactionsWithCategory.value
                    ?.map { it.transaction }
                    .orEmpty()
                handlePendingNotificationAction(currentTransactions)
            }
    }

    private fun handlePendingNotificationAction(transactions: List<TransactionEntity>) {
        val transactionId = pendingCategoryTransactionId ?: return
        val transaction = transactions.firstOrNull { it.transactionId == transactionId } ?: return

        pendingCategoryTransactionId = null
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.remove<String>("open_transaction_category_id")
        showCategorySelectionDialog(transaction)
    }

    private fun setupAdvancedFilter() {
        binding.btnFilterAdvanced.setOnClickListener {
            val bottomSheet = AdvancedFilterBottomSheet(
                initialMin = viewModel.minAmountFilter.value,
                initialMax = viewModel.maxAmountFilter.value,
                initialActor = viewModel.selectedActorFilter.value,
                onApply = { min, max, actor ->
                    viewModel.setAdvancedFilters(min, max, actor)
                    // Visual feedback: Change color if filters are active
                    if (min != null || max != null || actor != null) {
                        binding.btnFilterAdvanced.setColorFilter(requireContext().getColor(R.color.brand_light_green))
                    } else {
                        binding.btnFilterAdvanced.setColorFilter(requireContext().getColor(R.color.white))
                    }
                },
                onReset = {
                    viewModel.clearAdvancedFilters()
                    binding.btnFilterAdvanced.setColorFilter(requireContext().getColor(R.color.white))
                }
            )
            bottomSheet.show(parentFragmentManager, AdvancedFilterBottomSheet.TAG)
        }

        // Long press to clear
        binding.btnFilterAdvanced.setOnLongClickListener {
            viewModel.clearAdvancedFilters()
            binding.btnFilterAdvanced.setColorFilter(requireContext().getColor(R.color.white))
            android.widget.Toast.makeText(context, "Advanced filters cleared", android.widget.Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showTransactionDetails(transaction: TransactionEntity) {
        val categoryName = budgetViewModel.categories.value?.find { it.id == transaction.categoryId }?.name ?: "Uncategorized"
        val bottomSheet = TransactionDetailsBottomSheet(transaction, categoryName) {
            showCategorySelectionDialog(transaction)
        }
        bottomSheet.show(parentFragmentManager, TransactionDetailsBottomSheet.TAG)
    }

    private fun setupDateFilter() {
        binding.btnFilterDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .setSelection(
                    androidx.core.util.Pair(
                        MaterialDatePicker.thisMonthInUtcMilliseconds(),
                        MaterialDatePicker.todayInUtcMilliseconds()
                    )
                )
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val startDate = selection.first
                val endDate = selection.second
                
                if (startDate != null && endDate != null) {
                    viewModel.setDateRangeFilter(startDate, endDate)
                    
                    // Visual feedback
                    binding.btnFilterDate.setColorFilter(requireContext().getColor(R.color.brand_light_green))
                    
                    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                    val rangeText = "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"
                    android.widget.Toast.makeText(context, "Filtered: $rangeText", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            // Allow clearing filter on long press
            binding.btnFilterDate.setOnLongClickListener {
                viewModel.clearDateRangeFilter()
                binding.btnFilterDate.setColorFilter(requireContext().getColor(R.color.white)) // Reset tint
                android.widget.Toast.makeText(context, "Date filter cleared", android.widget.Toast.LENGTH_SHORT).show()
                true
            }

            datePicker.show(parentFragmentManager, "date_range_picker")
        }
        
        // Check initial state for visual feedback
        // Note: This would require observing the dateRangeFilter, but for now we'll just handle it in setup
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_all
            val filter = when (checkedId) {
                R.id.chip_all -> TransactionFilter.ALL
                R.id.chip_income -> TransactionFilter.INCOME
                R.id.chip_expense -> TransactionFilter.EXPENSE
                R.id.chip_uncategorized -> TransactionFilter.UNCATEGORIZED
                else -> TransactionFilter.ALL
            }
            viewModel.setFilterType(filter)
        }
    }

    private fun setupSort() {
        binding.btnSort.setOnClickListener {
            val sortTypes = listOf(
                "Date (Newest First)",
                "Date (Oldest First)",
                "Amount (High to Low)",
                "Amount (Low to High)"
            )
            val dialog = BottomSheetDialog(requireContext())
            val view = layoutInflater.inflate(R.layout.bottom_sheet_list_picker, null)
            view.findViewById<android.widget.TextView>(R.id.text_picker_title).text = "Sort transactions"
            val container = view.findViewById<android.widget.LinearLayout>(R.id.list_picker_container)

            sortTypes.forEachIndexed { index, label ->
                val row = android.widget.TextView(requireContext()).apply {
                    text = label
                    textSize = 15f
                    setTextColor(requireContext().getColor(R.color.text_primary))
                    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    minHeight = (50 * resources.displayMetrics.density).toInt()
                    setPadding(
                        (14 * resources.displayMetrics.density).toInt(),
                        0,
                        (14 * resources.displayMetrics.density).toInt(),
                        0
                    )
                    background = requireContext().getDrawable(R.drawable.bg_card_white)
                    setOnClickListener {
                        val sortType = when (index) {
                            0 -> SortType.DATE_DESC
                            1 -> SortType.DATE_ASC
                            2 -> SortType.AMOUNT_DESC
                            3 -> SortType.AMOUNT_ASC
                            else -> SortType.DATE_DESC
                        }
                        viewModel.setSortType(sortType)
                        dialog.dismiss()
                    }
                }
                container.addView(row)
                row.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * resources.displayMetrics.density).toInt()
                }
            }

            dialog.setContentView(view)
            dialog.show()
        }
    }

    private fun showCategorySelectionDialog(transaction: TransactionEntity) {
        val categories = budgetViewModel.categories.value ?: return
        
        val bottomSheet = CategorySelectionBottomSheet(
            categories,
            transaction.categoryId ?: -1,
            transaction.partyName
        ) { selectedCategory, isBulk ->
            if (isBulk) {
                viewModel.bulkCategorizeSimilarTransactions(transaction.partyName, selectedCategory.id)
            } else {
                viewModel.updateTransactionCategory(transaction.id, selectedCategory.id)
            }
        }
        bottomSheet.show(parentFragmentManager, CategorySelectionBottomSheet.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
