package com.ics2300.pocketbudget.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.databinding.FragmentDashboardBinding
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModel
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModelFactory
import com.ics2300.pocketbudget.ui.dashboard.TimeRange
import com.ics2300.pocketbudget.ui.dashboard.SyncResult
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import com.ics2300.pocketbudget.utils.TransactionGrouper

import com.google.android.material.snackbar.Snackbar
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory((requireActivity().application as MainApplication).repository)
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readGranted = permissions[android.Manifest.permission.READ_SMS] ?: false
            val receiveGranted = permissions[android.Manifest.permission.RECEIVE_SMS] ?: false
            // Check POST_NOTIFICATIONS only on T+
            val notificationGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissions[android.Manifest.permission.POST_NOTIFICATIONS] ?: false
            } else {
                true
            }
            
            if (readGranted && receiveGranted && notificationGranted) {
                syncSms()
            } else {
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.READ_SMS) ||
                    shouldShowRequestPermissionRationale(android.Manifest.permission.RECEIVE_SMS)) {
                    showPermissionRationaleDialog()
                } else {
                    showSettingsDialog()
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Auto-check permissions to ensure SmsReceiver works
        checkPermissionsOnStart()
        
        // Observe Sync Status
        viewModel.syncStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is SyncResult.Loading -> {
                    Toast.makeText(context, "Syncing transactions...", Toast.LENGTH_SHORT).show()
                }
                is SyncResult.Success -> {
                    val message = if (result.count > 0) 
                        "Found ${result.count} new transactions" 
                    else 
                        "All transactions are up to date"
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                }
                is SyncResult.Error -> {
                    Toast.makeText(context, "Sync failed: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkPermissionsOnStart() {
        val permissions = mutableListOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS
        )
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (!allGranted) {
            // Request permissions if not granted (enables Auto-Sync/Receiver)
            // The launcher callback will trigger a sync if granted.
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupHeader()
        setupTimeRangeTabs()
        setupQuickActions()
        setupRecentTransactions()
        setupCharts()
        setupSummaryObservers()

        // Observe Stats
        viewModel.dashboardStats.observe(viewLifecycleOwner) { stats ->
            binding.textIncomeAmount.text = CurrencyFormatter.formatKsh(stats.totalIncome)
            binding.textExpenseAmount.text = CurrencyFormatter.formatKsh(stats.totalExpense)
            
            val netFlow = stats.totalIncome - stats.totalExpense
            binding.textBalanceSummary.text = "Net: ${CurrencyFormatter.formatKsh(netFlow)}"
        }

        return root
    }

    private fun setupHeader() {
        binding.iconNotification.setOnClickListener {
            Toast.makeText(context, "No new notifications", Toast.LENGTH_SHORT).show()
        }
        // Removed profile click listener as there are no user profiles
    }

    private fun setupTimeRangeTabs() {
        binding.tabDay.setOnClickListener { selectTimeRange(TimeRange.DAY) }
        binding.tabWeek.setOnClickListener { selectTimeRange(TimeRange.WEEK) }
        binding.tabMonth.setOnClickListener { selectTimeRange(TimeRange.MONTH) }
        binding.tabYear.setOnClickListener { selectTimeRange(TimeRange.YEAR) }
        
        // Initial state
        updateTabStyles(TimeRange.DAY)
    }

    private fun selectTimeRange(range: TimeRange) {
        viewModel.setTimeRange(range)
        updateTabStyles(range)
    }

    private fun updateTabStyles(selected: TimeRange) {
        val tabs = mapOf(
            TimeRange.DAY to binding.tabDay,
            TimeRange.WEEK to binding.tabWeek,
            TimeRange.MONTH to binding.tabMonth,
            TimeRange.YEAR to binding.tabYear
        )

        tabs.forEach { (range, view) ->
            if (range == selected) {
                view.setBackgroundResource(R.drawable.bg_button_primary)
                view.setTextColor(requireContext().getColor(R.color.brand_dark_green))
                view.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                view.setBackgroundResource(R.drawable.bg_button_secondary)
                view.setTextColor(requireContext().getColor(R.color.text_on_dark))
                view.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    private fun setupQuickActions() {
        binding.actionSync.setOnClickListener { checkPermissionAndSync() }
        binding.actionAdd.setOnClickListener { 
            val bottomSheet = AddTransactionBottomSheet()
            bottomSheet.show(parentFragmentManager, AddTransactionBottomSheet.TAG)
        }
        binding.actionBudget.setOnClickListener { 
            findNavController().navigate(R.id.budgetFragment)
        }
        binding.actionAnalytics.setOnClickListener { 
            findNavController().navigate(R.id.analyticsFragment)
        }
    }

    private fun setupRecentTransactions() {
        val adapter = TransactionAdapter()
        binding.recyclerRecentTransactions.adapter = adapter
        binding.recyclerRecentTransactions.layoutManager = LinearLayoutManager(context)
        
        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            val groupedList = TransactionGrouper.groupTransactions(transactions)
            adapter.submitList(groupedList)
            
            if (transactions.isEmpty()) {
                binding.recyclerRecentTransactions.visibility = View.GONE
                binding.layoutEmptyState.root.visibility = View.VISIBLE
                binding.layoutEmptyState.textEmptyTitle.text = "No Recent Transactions"
                binding.layoutEmptyState.textEmptyMessage.text = "Add a transaction manually or sync from SMS."
                binding.layoutEmptyState.btnEmptyAction.visibility = View.VISIBLE
                binding.layoutEmptyState.btnEmptyAction.setOnClickListener {
                    val bottomSheet = AddTransactionBottomSheet()
                    bottomSheet.show(parentFragmentManager, AddTransactionBottomSheet.TAG)
                }
            } else {
                binding.recyclerRecentTransactions.visibility = View.VISIBLE
                binding.layoutEmptyState.root.visibility = View.GONE
            }
        }
        
        binding.btnSeeAll.setOnClickListener {
            // Navigate to transactions tab? For now just toast
            Toast.makeText(context, "View all in Transactions Tab", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionAndSync() {
        val permissions = mutableListOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allGranted) {
            syncSms()
        } else {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }
    
    private fun syncSms() {
        viewModel.syncSms()
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("PocketBudget needs SMS and Notification permissions to automatically track your M-Pesa transactions. This saves you from manual entry.")
            .setPositiveButton("Grant") { _, _ ->
                val permissions = mutableListOf(
                    android.Manifest.permission.READ_SMS, 
                    android.Manifest.permission.RECEIVE_SMS
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                requestPermissionsLauncher.launch(permissions.toTypedArray())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissions Denied")
            .setMessage("SMS permissions are permanently denied. Please enable them in Settings to use the Sync feature.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupCharts() {
        setupWeeklyChart()
        setupMonthlyChart()
    }

    private fun setupWeeklyChart() {
        val weeklyChart = binding.chartWeekly
        weeklyChart.description.isEnabled = false
        weeklyChart.legend.isEnabled = true
        weeklyChart.setTouchEnabled(true)
        weeklyChart.setDragEnabled(true)
        weeklyChart.setScaleEnabled(true)
        
        viewModel.weeklyBreakdown.observe(viewLifecycleOwner) { chartDataList ->
            val entries = chartDataList.mapIndexed { index, data ->
                BarEntry(index.toFloat(), data.value.toFloat())
            }
            
            val dataSet = BarDataSet(entries, "Spending").apply {
                color = requireContext().getColor(R.color.brand_dark_green)
                valueTextSize = 10f
            }
            
            val barData = BarData(dataSet)
            weeklyChart.data = barData
            
            val xAxis = weeklyChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(
                chartDataList.map { it.label }
            )
            
            weeklyChart.invalidate()
        }
    }

    private fun setupMonthlyChart() {
        val monthlyChart = binding.chartMonthly
        monthlyChart.description.isEnabled = false
        monthlyChart.legend.isEnabled = true
        monthlyChart.setTouchEnabled(true)
        monthlyChart.setDragEnabled(true)
        monthlyChart.setScaleEnabled(true)
        
        viewModel.monthlyBreakdown.observe(viewLifecycleOwner) { chartDataList ->
            val entries = chartDataList.mapIndexed { index, data ->
                Entry(index.toFloat(), data.value.toFloat())
            }
            
            val dataSet = LineDataSet(entries, "Monthly Spending").apply {
                color = requireContext().getColor(R.color.brand_light_green)
                setCircleColor(requireContext().getColor(R.color.brand_dark_green))
                lineWidth = 2f
                setDrawCircles(true)
                valueTextSize = 10f
            }
            
            val lineData = LineData(dataSet)
            monthlyChart.data = lineData
            
            val xAxis = monthlyChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(
                chartDataList.map { it.label }
            )
            
            monthlyChart.invalidate()
        }
    }

    private fun setupSummaryObservers() {
        // Total Spent
        viewModel.totalSpent.observe(viewLifecycleOwner) { amount ->
            binding.textTotalSpent.text = CurrencyFormatter.formatKsh(amount)
            val progress = (amount / 10000.0 * 100).coerceIn(0.0, 100.0).toInt()
            binding.progressSpent.progress = progress
        }

        // Total Received
        viewModel.totalReceived.observe(viewLifecycleOwner) { amount ->
            binding.textTotalReceived.text = CurrencyFormatter.formatKsh(amount)
            val progress = (amount / 15000.0 * 100).coerceIn(0.0, 100.0).toInt()
            binding.progressReceived.progress = progress
        }

        // Top Category
        viewModel.topCategory.observe(viewLifecycleOwner) { topCat ->
            if (topCat != null) {
                binding.textTopCategoryName.text = topCat.name
                binding.textTopCategoryAmount.text = CurrencyFormatter.formatKsh(topCat.amount)
                binding.textTopCategoryCount.text = "${topCat.count} transactions"
            } else {
                binding.textTopCategoryName.text = "No Data"
                binding.textTopCategoryAmount.text = "Ksh 0"
                binding.textTopCategoryCount.text = "0 transactions"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
