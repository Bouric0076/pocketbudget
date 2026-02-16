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
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import com.ics2300.pocketbudget.utils.TransactionGrouper

import com.google.android.material.snackbar.Snackbar
import com.ics2300.pocketbudget.ui.dashboard.SyncResult

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
