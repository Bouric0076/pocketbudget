package com.ics2300.pocketbudget.ui

import android.graphics.Typeface
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.databinding.FragmentDashboardBinding
import com.ics2300.pocketbudget.ui.dashboard.ActorSpendingAdapter
import com.ics2300.pocketbudget.data.DashboardStats
import com.ics2300.pocketbudget.ui.dashboard.DashboardViewModel
import com.ics2300.pocketbudget.ui.dashboard.TimeRange
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import com.ics2300.pocketbudget.utils.TransactionGrouper
import com.ics2300.pocketbudget.ui.TransactionListItem

import com.google.android.material.snackbar.Snackbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ics2300.pocketbudget.ui.dashboard.SyncResult
import com.ics2300.pocketbudget.utils.AnalyticsUtils
import com.ics2300.pocketbudget.utils.SecurityUtils
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private var latestRecentTransactions: List<TransactionEntity> = emptyList()

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

        adjustDashboardBottomInset()
        
        // Update Privacy Eye State
        updatePrivacyEyeState()
        
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
                    
                    val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                    activity?.findViewById<View>(R.id.bottom_navigation)?.let { bottomNav ->
                        if (bottomNav.visibility == View.VISIBLE) {
                            snackbar.anchorView = bottomNav
                        }
                    }
                    snackbar.show()
                }
                is SyncResult.Error -> {
                    Toast.makeText(context, "Sync failed: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
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
        setupTopRecipients()

        // Observe Stats
        viewModel.dashboardStats.observe(viewLifecycleOwner) { stats ->
            updateDashboardStats(stats)
        }

        viewModel.categories.observe(viewLifecycleOwner) {
            bindRecentTransactions(latestRecentTransactions)
        }

        return root
    }

    private fun setupHeader() {
        binding.iconNotification.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        
        binding.iconPrivacyEye.setOnClickListener {
            val context = requireContext()
            val isEnabled = SecurityUtils.isPrivacyModeEnabled(context)
            SecurityUtils.setPrivacyModeEnabled(context, !isEnabled)
            
            updatePrivacyEyeState()
            
            // Refresh dashboard data to apply mask
            // Trigger a refresh by re-setting time range or just observing again (automatically happens if LiveData updates)
            // But formatting happens in observer, so we need to re-trigger the observer.
            // The simplest way is to just call selectTimeRange again or notify adapter.
            val currentRange = viewModel.dashboardStats.value
            // We just need to refresh the UI elements that use CurrencyFormatter
            // We can do this by forcing a re-bind of the current stats if available
            viewModel.dashboardStats.value?.let { stats ->
                updateDashboardStats(stats)
            }
            
            // Also refresh recycler views
            (binding.recyclerRecentTransactions.adapter as? TransactionAdapter)?.notifyDataSetChanged()
            (binding.recyclerTopRecipients.adapter as? ActorSpendingAdapter)?.setPrivacyMode(!isEnabled)
        }
    }
    
    private fun updatePrivacyEyeState() {
        val isPrivacyMode = SecurityUtils.isPrivacyModeEnabled(requireContext())
        if (isPrivacyMode) {
            binding.iconPrivacyEye.setImageResource(android.R.drawable.ic_menu_view) // Actually "view" usually means open eye, maybe use a closed eye icon if available
            // Standard android icons: ic_menu_view is usually an eye. 
            // Let's use ic_menu_view for "Visible" (Privacy OFF) and something else for "Hidden".
            // Wait, if privacy mode is ON, we are HIDING data. So maybe show a "closed eye" or "crossed out eye".
            // Since we don't have custom assets yet, let's tint it differently or use alpha.
            binding.iconPrivacyEye.alpha = 0.5f
            binding.iconPrivacyEye.setColorFilter(requireContext().getColor(R.color.text_secondary))
        } else {
            binding.iconPrivacyEye.setImageResource(android.R.drawable.ic_menu_view)
            binding.iconPrivacyEye.alpha = 1.0f
            binding.iconPrivacyEye.setColorFilter(requireContext().getColor(R.color.brand_light_green))
        }
    }

    private fun updateDashboardStats(stats: DashboardStats) {
        val context = requireContext()
        val isPrivacy = SecurityUtils.isPrivacyModeEnabled(context)
        binding.textIncomeAmount.text = CurrencyFormatter.formatKsh(stats.totalIncome, isPrivacy)
        binding.textExpenseAmount.text = CurrencyFormatter.formatKsh(stats.totalExpense, isPrivacy)
        binding.textTxnCount.text = if (isPrivacy) {
            "••"
        } else {
            stats.transactionCount.toString()
        }
        
        val netFlow = stats.totalIncome - stats.totalExpense
        binding.textBalanceSummary.text = "Net: ${CurrencyFormatter.formatKsh(netFlow, isPrivacy)}"
        
        // Spending Velocity Indicator
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Use income as a rough budget proxy if no budget set, or ideally fetch total budget
        // For now, let's compare against income or a fixed target if budget unavailable
        val estimatedBudget = if (stats.totalIncome > 0) stats.totalIncome else 30000.0 // Fallback
        
        val velocity = AnalyticsUtils.calculateVelocity(
            stats.totalExpense, 
            estimatedBudget, 
            dayOfMonth, 
            totalDays
        )
        
        val velocityText = if (netFlow < 0) {
            "Overspending! \u26A0\uFE0F"
        } else {
            when(velocity) {
                AnalyticsUtils.VelocityStatus.FAST -> "Spending Fast! \uD83D\uDD25"
                AnalyticsUtils.VelocityStatus.SLOW -> "Saving Well \uD83D\uDC4D"
                AnalyticsUtils.VelocityStatus.NORMAL -> "On Track \uD83C\uDFAF"
            }
        }
        
        // Append to balance summary or a new view
        binding.textBalanceSummary.text = "Net: ${CurrencyFormatter.formatKsh(netFlow, isPrivacy)} \u2022 $velocityText"
    }

    private fun setupTimeRangeTabs() {
        binding.tabDay.setOnClickListener { selectTimeRange(TimeRange.DAY) }
        binding.tabWeek.setOnClickListener { selectTimeRange(TimeRange.WEEK) }
        binding.tabMonth.setOnClickListener { selectTimeRange(TimeRange.MONTH) }
        binding.tabYear.setOnClickListener { selectTimeRange(TimeRange.YEAR) }
        
        // Initial state
        updateTabStyles(TimeRange.DAY)
        updatePeriodLabel(TimeRange.DAY)
    }

    private fun selectTimeRange(range: TimeRange) {
        viewModel.setTimeRange(range)
        updateTabStyles(range)
        updatePeriodLabel(range)
    }

    private fun updatePeriodLabel(range: TimeRange) {
        val calendar = Calendar.getInstance()
        binding.textCurrentPeriod.text = when (range) {
            TimeRange.DAY -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(calendar.time)
            TimeRange.WEEK -> {
                val start = calendar.clone() as Calendar
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                val end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_WEEK, 6)
                val dayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                "${dayFormat.format(start.time)} - ${dayFormat.format(end.time)}"
            }
            TimeRange.MONTH -> SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(calendar.time)
            TimeRange.YEAR -> calendar.get(Calendar.YEAR).toString()
            TimeRange.ALL -> "All time"
        }
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
                view.setBackgroundResource(R.drawable.bg_tab_selected)
                view.setTextColor(requireContext().getColor(R.color.white))
                view.setTypeface(null, Typeface.BOLD)
            } else {
                view.setBackgroundResource(R.drawable.bg_tab_default)
                view.setTextColor(requireContext().getColor(R.color.text_secondary))
                view.setTypeface(null, Typeface.NORMAL)
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
        val adapter = TransactionAdapter { transaction ->
            showTransactionDetails(transaction)
        }
        binding.recyclerRecentTransactions.adapter = adapter
        binding.recyclerRecentTransactions.layoutManager = LinearLayoutManager(context)
        
        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            latestRecentTransactions = transactions
            bindRecentTransactions(transactions)
        }
        
        binding.btnSeeAll.setOnClickListener {
            // Navigate to transactions tab? For now just toast
            Toast.makeText(context, "View all in Transactions Tab", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindRecentTransactions(transactions: List<TransactionEntity>) {
        val adapter = binding.recyclerRecentTransactions.adapter as? TransactionAdapter ?: return
        val categories = viewModel.categories.value.orEmpty()
        val items = transactions.map { transaction ->
            val category = categories.find { it.id == transaction.categoryId }
            TransactionListItem.Transaction(transaction, category)
        }
        adapter.submitList(items)

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

    private fun showTransactionDetails(transaction: TransactionEntity) {
        val categories = viewModel.categories.value
        val categoryName = categories?.find { it.id == transaction.categoryId }?.name 
            ?: if (categories == null) "Loading..." else "Uncategorized"
            
        val bottomSheet = TransactionDetailsBottomSheet(transaction, categoryName) {
            showCategorySelectionDialog(transaction)
        }
        bottomSheet.show(parentFragmentManager, TransactionDetailsBottomSheet.TAG)
    }

    private fun showCategorySelectionDialog(transaction: TransactionEntity) {
        val categories = viewModel.categories.value ?: return
        
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

    private fun setupTopRecipients() {
        val isPrivacy = SecurityUtils.isPrivacyModeEnabled(requireContext())
        val adapter = ActorSpendingAdapter(isPrivacy)
        binding.recyclerTopRecipients.adapter = adapter
        binding.recyclerTopRecipients.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        viewModel.topSpendingActors.observe(viewLifecycleOwner) { actors ->
            binding.layoutTopRecipients.visibility = if (actors.isEmpty()) View.GONE else View.VISIBLE
            adapter.submitList(actors)
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
            showPermissionRationaleDialog()
        }
    }
    
    private fun syncSms() {
        viewModel.syncSms()
    }

    private fun showPermissionRationaleDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_action_prompt, null)
        
        view.findViewById<android.widget.TextView>(R.id.text_prompt_title).text = "Allow automatic tracking"
        view.findViewById<android.widget.TextView>(R.id.text_prompt_message).text = 
            "PocketBudget needs SMS access to read M-Pesa messages and notification permission for alerts. You can still add transactions manually."
        
        val linearLayout = view as android.widget.LinearLayout
        val composeView = androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PermissionExplanationVisuals(
                    accentColor = androidx.compose.ui.graphics.Color(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brand_indigo)
                    )
                )
            }
        }
        
        linearLayout.addView(composeView, 3)

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_prompt_primary).apply {
            text = "Grant permissions"
            setOnClickListener {
                dialog.dismiss()
                val permissions = mutableListOf(
                    android.Manifest.permission.READ_SMS, 
                    android.Manifest.permission.RECEIVE_SMS
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                requestPermissionsLauncher.launch(permissions.toTypedArray())
            }
        }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_prompt_secondary).apply {
            text = "Not now"
            setOnClickListener { dialog.dismiss() }
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showSettingsDialog() {
        showActionSheet(
            title = "Enable permissions in Settings",
            message = "SMS permission is blocked. Open Android Settings and enable SMS access to use Sync.",
            primary = "Open Settings",
            secondary = "Cancel",
            onPrimary = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        )
    }

    private fun adjustDashboardBottomInset() {
        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val currentBottomPadding = binding.dashboardScrollView.paddingBottom

        bottomNavigationView.post {
            val bottomMargin = (bottomNavigationView.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
            val extraSpacing = (resources.displayMetrics.density * 16).toInt()

            binding.dashboardScrollView.setPadding(
                binding.dashboardScrollView.paddingLeft,
                binding.dashboardScrollView.paddingTop,
                binding.dashboardScrollView.paddingRight,
                currentBottomPadding + bottomNavigationView.height + bottomMargin + extraSpacing
            )
        }
    }

    private fun showActionSheet(
        title: String,
        message: String,
        primary: String,
        secondary: String,
        onPrimary: () -> Unit
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_action_prompt, null)
        view.findViewById<android.widget.TextView>(R.id.text_prompt_title).text = title
        view.findViewById<android.widget.TextView>(R.id.text_prompt_message).text = message
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_prompt_primary).apply {
            text = primary
            setOnClickListener {
                dialog.dismiss()
                onPrimary()
            }
        }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_prompt_secondary).apply {
            text = secondary
            setOnClickListener { dialog.dismiss() }
        }
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
