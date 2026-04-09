package com.ics2300.pocketbudget.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.databinding.FragmentSettingsBinding
import com.ics2300.pocketbudget.ui.settings.SettingsViewModel
import com.ics2300.pocketbudget.ui.settings.SettingsViewModelFactory
import com.ics2300.pocketbudget.ui.dashboard.SyncResult

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.ics2300.pocketbudget.R

import com.ics2300.pocketbudget.utils.AutoStartHelper

import androidx.activity.result.contract.ActivityResultContracts

import com.ics2300.pocketbudget.utils.SecurityUtils
import com.ics2300.pocketbudget.utils.CategoryUtils
import android.graphics.Color
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.HorizontalScrollView
import androidx.core.view.setPadding

import java.util.Calendar

import com.ics2300.pocketbudget.utils.PremiumManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory((requireActivity().application as MainApplication).repository)
    }

    private var exportType = "pdf"
    private var exportStartDate: Long? = null
    private var exportEndDate: Long? = null

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                if (exportType == "csv") {
                    viewModel.exportData(requireContext(), uri)
                } else {
                    viewModel.exportPdf(requireContext(), uri, exportStartDate, exportEndDate)
                }
            }
        }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                showConfirmationDialog("Import Data", "This will add transactions from the selected CSV file. Existing duplicate transactions will be skipped. Continue?") {
                    viewModel.importData(requireContext(), uri)
                    Toast.makeText(context, "Import started...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updatePremiumUI() {
        val isPremium = PremiumManager.isPremium(requireContext())
        if (isPremium) {
            binding.textPremiumStatus.text = "Premium Active \uD83D\uDC51"
            binding.textPremiumStatus.setTextColor(requireContext().getColor(R.color.brand_light_green))
            binding.btnUpgradePremium.visibility = View.GONE
        } else {
            binding.textPremiumStatus.text = "Free Plan"
            binding.textPremiumStatus.setTextColor(Color.GRAY)
            binding.btnUpgradePremium.visibility = View.VISIBLE
        }
    }

    private fun showPremiumPurchaseDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_premium_purchase, null)
        val btnBuy = dialogView.findViewById<android.widget.Button>(R.id.btn_buy_premium)
        val btnCancel = dialogView.findViewById<android.widget.TextView>(R.id.text_cancel)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))

        btnBuy.setOnClickListener {
            // Mock Purchase Success
            PremiumManager.setPremiumStatus(requireContext(), true)
            updatePremiumUI()
            Toast.makeText(context, "Welcome to Premium! \uD83C\uDF89", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPremiumLockedDialog(feature: String) {
        val featureName = PremiumManager.getFeatureName(feature)
        AlertDialog.Builder(requireContext())
            .setTitle("Premium Feature")
            .setMessage("$featureName is available for Premium users only. Upgrade for just Ksh 50/month to unlock PDF/CSV exports, Import Data, and Advanced Analytics.")
            .setPositiveButton("Upgrade Now") { _, _ ->
                showPremiumPurchaseDialog()
            }
            .setNegativeButton("Maybe Later", null)
            .show()
    }

    private fun launchExportIntent(mimeType: String, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        createDocumentLauncher.launch(intent)
    }

    private fun showDateRangeDialog(onRangeSelected: (String) -> Unit) {
        val options = arrayOf("All Time", "This Month", "Last Month", "This Year", "Custom Range")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Report Period")
            .setItems(options) { _, which ->
                val calendar = Calendar.getInstance()
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                
                // Clear time components for clean comparisons
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                var dateLabel = ""

                when (which) {
                    0 -> { // All Time
                        exportStartDate = null
                        exportEndDate = null
                        dateLabel = "All_Time"
                        onRangeSelected(dateLabel)
                    }
                    1 -> { // This Month
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        exportStartDate = calendar.timeInMillis
                        val startStr = dateFormat.format(calendar.time)
                        
                        calendar.add(Calendar.MONTH, 1)
                        calendar.add(Calendar.MILLISECOND, -1)
                        exportEndDate = calendar.timeInMillis
                        val endStr = dateFormat.format(calendar.time)
                        
                        dateLabel = "${startStr}_to_${endStr}"
                        onRangeSelected(dateLabel)
                    }
                    2 -> { // Last Month
                        calendar.add(Calendar.MONTH, -1)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        exportStartDate = calendar.timeInMillis
                        val startStr = dateFormat.format(calendar.time)
                        
                        calendar.add(Calendar.MONTH, 1)
                        calendar.add(Calendar.MILLISECOND, -1)
                        exportEndDate = calendar.timeInMillis
                        val endStr = dateFormat.format(calendar.time)
                        
                        dateLabel = "${startStr}_to_${endStr}"
                        onRangeSelected(dateLabel)
                    }
                    3 -> { // This Year
                        calendar.set(Calendar.DAY_OF_YEAR, 1)
                        exportStartDate = calendar.timeInMillis
                        val startStr = dateFormat.format(calendar.time)
                        
                        calendar.add(Calendar.YEAR, 1)
                        calendar.add(Calendar.MILLISECOND, -1)
                        exportEndDate = calendar.timeInMillis
                        val endStr = dateFormat.format(calendar.time)
                        
                        dateLabel = "${startStr}_to_${endStr}"
                        onRangeSelected(dateLabel)
                    }
                    4 -> { // Custom Range
                        showCustomDatePicker {
                            val start = java.util.Date(exportStartDate ?: 0)
                            val end = java.util.Date(exportEndDate ?: 0)
                            dateLabel = "${dateFormat.format(start)}_to_${dateFormat.format(end)}"
                            onRangeSelected(dateLabel)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomDatePicker(onRangeSelected: () -> Unit) {
        // Simple implementation: Pick Start Date, then Pick End Date
        val calendar = Calendar.getInstance()
        
        android.app.DatePickerDialog(requireContext(), { _, year, month, day ->
            val startCal = Calendar.getInstance()
            startCal.set(year, month, day, 0, 0, 0)
            exportStartDate = startCal.timeInMillis
            
            // Show End Date Picker immediately after
            android.app.DatePickerDialog(requireContext(), { _, endYear, endMonth, endDay ->
                val endCal = Calendar.getInstance()
                endCal.set(endYear, endMonth, endDay, 23, 59, 59)
                exportEndDate = endCal.timeInMillis
                
                if (exportEndDate!! < exportStartDate!!) {
                    Toast.makeText(context, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
                } else {
                    onRangeSelected()
                }
            }, year, month, day).apply {
                setTitle("Select End Date")
                show()
            }
            
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("Select Start Date")
            show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Premium Status UI
        updatePremiumUI()
        
        binding.btnUpgradePremium.setOnClickListener {
            // Simulate Purchase
            showPremiumPurchaseDialog()
        }

        binding.btnManageCategories.setOnClickListener {
            // Show dialog to add/edit categories
            showManageCategoriesDialog()
        }
        
        binding.btnExportData.setOnClickListener {
            if (!PremiumManager.canAccessFeature(requireContext(), PremiumManager.FEATURE_PDF_EXPORT)) {
                showPremiumLockedDialog(PremiumManager.FEATURE_PDF_EXPORT)
                return@setOnClickListener
            }
            
            val options = arrayOf("PDF Report (Recommended)", "CSV (Excel)")
            AlertDialog.Builder(requireContext())
                .setTitle("Export Data")
                .setItems(options) { _, which ->
                    if (which == 0) {
                        // PDF - Show Date Range Picker
                        showDateRangeDialog { dateLabel ->
                            exportType = "pdf"
                            launchExportIntent("application/pdf", "PocketBudget_Report_$dateLabel.pdf")
                        }
                    } else {
                        // CSV - Default behavior (or add date range too if requested later)
                        // For consistency, let's also show date range picker for CSV
                        showDateRangeDialog { dateLabel ->
                             exportType = "csv"
                             launchExportIntent("text/csv", "PocketBudget_Export_$dateLabel.csv")
                        }
                    }
                }
                .show()
        }

        binding.btnImportData.setOnClickListener {
            if (!PremiumManager.canAccessFeature(requireContext(), PremiumManager.FEATURE_IMPORT_DATA)) {
                showPremiumLockedDialog(PremiumManager.FEATURE_IMPORT_DATA)
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv" // Or "*/*" if CSV mime type varies
            }
            openDocumentLauncher.launch(intent)
        }

        binding.btnResync.setOnClickListener {
            showConfirmationDialog("Resync Data", "This will re-scan all SMS messages to add any missing transactions. Existing data will be preserved. Continue?") {
                viewModel.resyncData()
                Toast.makeText(context, "Resync started...", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Battery Optimization Logic
        binding.btnBatteryOpt.setOnClickListener {
            checkAndRequestBatteryOptimization()
        }
        updateBatteryButtonState()
        
        // Auto-Start Logic (Manufacturer Specific)
        if (AutoStartHelper.isAutoStartPermissionAvailable(requireContext())) {
            binding.btnAutoStart.visibility = View.VISIBLE
            binding.btnAutoStart.setOnClickListener {
                AutoStartHelper.requestAutoStartPermission(requireContext())
            }
        } else {
            binding.btnAutoStart.visibility = View.GONE
        }

        binding.btnClearData.setOnClickListener {
            showConfirmationDialog("Clear Data", "Are you sure you want to delete all transactions and budgets? This cannot be undone.") {
                viewModel.clearAllData()
                Toast.makeText(context, "All data cleared.", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
             // Placeholder for notification logic
             val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
             Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        // Security Logic
        setupSecuritySettings()

        viewModel.syncStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is SyncResult.Loading -> {
                    // Toast.makeText(context, "Resyncing...", Toast.LENGTH_SHORT).show()
                }
                is SyncResult.Success -> {
                    if (result.count > 0) {
                        Toast.makeText(context, "Sync complete. ${result.count} new transactions added.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Sync complete. No new transactions found.", Toast.LENGTH_SHORT).show()
                    }
                }
                is SyncResult.Error -> {
                    Toast.makeText(context, "Sync failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observe Categories to ensure they are loaded for the management dialog
        viewModel.categories.observe(viewLifecycleOwner) { /* Keep LiveData active */ }

        // Set App Version Dynamically from BuildConfig
        binding.textViewVersionNumber.text = "v${BuildConfig.VERSION_NAME}"

        return root
    }

    private fun setupSecuritySettings() {
        val context = requireContext()
        
        // Initial State
        binding.switchBiometric.isChecked = SecurityUtils.isSecurityEnabled(context)
        
        // Listener
        binding.switchBiometric.setOnClickListener {
            val isChecked = binding.switchBiometric.isChecked
            if (isChecked) {
                if (!SecurityUtils.hasPin(context)) {
                    // Need to set PIN first
                    showSetPinDialog()
                    binding.switchBiometric.isChecked = false 
                } else {
                    SecurityUtils.setBiometricEnabled(context, true)
                    Toast.makeText(context, "App Lock Enabled", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Confirm disable
                showConfirmationDialog("Disable Security", "This will remove the App Lock PIN. Continue?") {
                    SecurityUtils.clearSecurity(context)
                    binding.switchBiometric.isChecked = false
                    Toast.makeText(context, "App Lock Disabled", Toast.LENGTH_SHORT).show()
                }
                // Keep it checked until confirmed
                binding.switchBiometric.isChecked = true
            }
        }

        binding.btnChangePin.setOnClickListener {
            showSetPinDialog()
        }
        
        // Privacy Mode
        binding.switchPrivacyMode.isChecked = SecurityUtils.isPrivacyModeEnabled(context)
        binding.switchPrivacyMode.setOnCheckedChangeListener { _, isChecked ->
            SecurityUtils.setPrivacyModeEnabled(context, isChecked)
            // Inform user
            val status = if (isChecked) "Enabled" else "Disabled"
            Toast.makeText(context, "Privacy Mode $status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSetPinDialog() {
        val inputLayout = android.widget.LinearLayout(context)
        inputLayout.orientation = android.widget.LinearLayout.VERTICAL
        inputLayout.setPadding(50, 40, 50, 10)

        val pinInput = android.widget.EditText(context)
        pinInput.hint = "Enter 4-digit PIN"
        pinInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        pinInput.filters = arrayOf(android.text.InputFilter.LengthFilter(4))
        inputLayout.addView(pinInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Set App Lock PIN")
            .setView(inputLayout)
            .setPositiveButton("Save") { _, _ ->
                val pin = pinInput.text.toString()
                if (pin.length == 4) {
                    SecurityUtils.setPin(requireContext(), pin)
                    SecurityUtils.setBiometricEnabled(requireContext(), true) // Enable flag
                    binding.switchBiometric.isChecked = true
                    Toast.makeText(context, "PIN Set & Locked Enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManageCategoriesDialog() {
        val categories = viewModel.categories.value ?: emptyList()
        val categoryNames = categories.map { it.name }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Manage Categories")
            .setItems(categoryNames) { _, which ->
                val selectedCategory = categories[which]
                showEditCategoryDialog(selectedCategory)
            }
            .setPositiveButton("Add New") { _, _ ->
                showAddCategoryDialog()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAddCategoryDialog() {
        val inputLayout = LinearLayout(context)
        inputLayout.orientation = LinearLayout.VERTICAL
        inputLayout.setPadding(50, 40, 50, 10)

        val nameInput = android.widget.EditText(context)
        nameInput.hint = "Category Name (e.g. Pets)"
        inputLayout.addView(nameInput)

        val keywordsInput = android.widget.EditText(context)
        keywordsInput.hint = "Keywords (comma separated, e.g. VET,FOOD)"
        inputLayout.addView(keywordsInput)
        
        // Icon Picker
        val iconLabel = android.widget.TextView(context)
        iconLabel.text = "Select Icon:"
        iconLabel.setPadding(0, 20, 0, 10)
        inputLayout.addView(iconLabel)
        
        val iconScroll = HorizontalScrollView(context)
        val iconContainer = LinearLayout(context)
        iconContainer.orientation = LinearLayout.HORIZONTAL
        var selectedIcon = "ic_default"
        
        CategoryUtils.iconMap.forEach { (name, resId) ->
            val iconView = ImageView(context)
            iconView.setImageResource(resId)
            iconView.layoutParams = LinearLayout.LayoutParams(100, 100).apply { marginEnd = 16 }
            iconView.setPadding(16)
            iconView.setColorFilter(requireContext().getColor(R.color.brand_dark_green))
            
            if (name == selectedIcon) iconView.setBackgroundResource(R.drawable.bg_circle_button) // Highlight
            
            iconView.setOnClickListener {
                selectedIcon = name
                // Reset backgrounds
                for (i in 0 until iconContainer.childCount) {
                    iconContainer.getChildAt(i).background = null
                }
                iconView.setBackgroundResource(R.drawable.bg_circle_button)
            }
            iconContainer.addView(iconView)
        }
        iconScroll.addView(iconContainer)
        inputLayout.addView(iconScroll)

        // Color Picker
        val colorLabel = android.widget.TextView(context)
        colorLabel.text = "Select Color:"
        colorLabel.setPadding(0, 20, 0, 10)
        inputLayout.addView(colorLabel)
        
        val colorScroll = HorizontalScrollView(context)
        val colorContainer = LinearLayout(context)
        colorContainer.orientation = LinearLayout.HORIZONTAL
        var selectedColor = "#0A3D2E"
        
        CategoryUtils.availableColors.forEach { colorHex ->
            val colorView = android.view.View(context)
            colorView.layoutParams = LinearLayout.LayoutParams(80, 80).apply { marginEnd = 16 }
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(Color.parseColor(colorHex))
            drawable.setStroke(2, Color.GRAY)
            colorView.background = drawable
            
            colorView.setOnClickListener {
                selectedColor = colorHex
                // Visual feedback (simple alpha change or border)
                for (i in 0 until colorContainer.childCount) {
                    colorContainer.getChildAt(i).alpha = 0.5f
                }
                colorView.alpha = 1.0f
            }
            // Init selection state
            if (colorHex == selectedColor) colorView.alpha = 1.0f else colorView.alpha = 0.5f
            
            colorContainer.addView(colorView)
        }
        colorScroll.addView(colorContainer)
        inputLayout.addView(colorScroll)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Category")
            .setView(inputLayout)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val keywords = keywordsInput.text.toString().trim().uppercase()
                if (name.isNotEmpty()) {
                    viewModel.addCategory(name, keywords, selectedIcon, selectedColor)
                    Toast.makeText(context, "Category added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditCategoryDialog(category: com.ics2300.pocketbudget.data.CategoryEntity) {
        val inputLayout = LinearLayout(context)
        inputLayout.orientation = LinearLayout.VERTICAL
        inputLayout.setPadding(50, 40, 50, 10)

        val nameInput = android.widget.EditText(context)
        nameInput.setText(category.name)
        nameInput.hint = "Category Name"
        inputLayout.addView(nameInput)

        val keywordsInput = android.widget.EditText(context)
        keywordsInput.setText(category.keywords)
        keywordsInput.hint = "Keywords (comma separated)"
        inputLayout.addView(keywordsInput)
        
        // Icon Picker
        val iconLabel = android.widget.TextView(context)
        iconLabel.text = "Select Icon:"
        iconLabel.setPadding(0, 20, 0, 10)
        inputLayout.addView(iconLabel)
        
        val iconScroll = HorizontalScrollView(context)
        val iconContainer = LinearLayout(context)
        iconContainer.orientation = LinearLayout.HORIZONTAL
        var selectedIcon = category.iconName
        
        CategoryUtils.iconMap.forEach { (name, resId) ->
            val iconView = ImageView(context)
            iconView.setImageResource(resId)
            iconView.layoutParams = LinearLayout.LayoutParams(100, 100).apply { marginEnd = 16 }
            iconView.setPadding(16)
            iconView.setColorFilter(requireContext().getColor(R.color.brand_dark_green))
            
            // Initial Selection
            if (name == selectedIcon) {
                iconView.setBackgroundResource(R.drawable.bg_circle_button)
            } else {
                iconView.background = null
            }
            
            iconView.setOnClickListener {
                selectedIcon = name
                for (i in 0 until iconContainer.childCount) {
                    iconContainer.getChildAt(i).background = null
                }
                iconView.setBackgroundResource(R.drawable.bg_circle_button)
            }
            iconContainer.addView(iconView)
        }
        iconScroll.addView(iconContainer)
        inputLayout.addView(iconScroll)

        // Color Picker
        val colorLabel = android.widget.TextView(context)
        colorLabel.text = "Select Color:"
        colorLabel.setPadding(0, 20, 0, 10)
        inputLayout.addView(colorLabel)
        
        val colorScroll = HorizontalScrollView(context)
        val colorContainer = LinearLayout(context)
        colorContainer.orientation = LinearLayout.HORIZONTAL
        var selectedColor = category.colorHex
        
        CategoryUtils.availableColors.forEach { colorHex ->
            val colorView = android.view.View(context)
            colorView.layoutParams = LinearLayout.LayoutParams(80, 80).apply { marginEnd = 16 }
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(Color.parseColor(colorHex))
            drawable.setStroke(2, Color.GRAY)
            colorView.background = drawable
            
            // Initial Selection
            if (colorHex == selectedColor) colorView.alpha = 1.0f else colorView.alpha = 0.5f
            
            colorView.setOnClickListener {
                selectedColor = colorHex
                for (i in 0 until colorContainer.childCount) {
                    colorContainer.getChildAt(i).alpha = 0.5f
                }
                colorView.alpha = 1.0f
            }
            colorContainer.addView(colorView)
        }
        colorScroll.addView(colorContainer)
        inputLayout.addView(colorScroll)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit ${category.name}")
            .setView(inputLayout)
            .setPositiveButton("Update") { _, _ ->
                val name = nameInput.text.toString().trim()
                val keywords = keywordsInput.text.toString().trim().uppercase()
                if (name.isNotEmpty()) {
                    viewModel.updateCategory(category.copy(name = name, keywords = keywords, iconName = selectedIcon, colorHex = selectedColor))
                    Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                showConfirmationDialog("Delete Category", "Are you sure you want to delete '${category.name}'? Transactions will become uncategorized.") {
                    viewModel.deleteCategory(category.id)
                    Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
        }
        return true
    }

    private fun checkAndRequestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = requireContext().packageName
            val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent()
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback if permission is missing or activity not found
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(context, "Optimization already disabled", Toast.LENGTH_SHORT).show()
            }
        } else {
             Toast.makeText(context, "Not needed on this Android version", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateBatteryButtonState() {
        if (isBatteryOptimizationIgnored()) {
            binding.btnBatteryOpt.text = "Battery Optimization: Disabled (Good)"
            binding.btnBatteryOpt.setTextColor(requireContext().getColor(R.color.brand_secondary_green))
        } else {
            binding.btnBatteryOpt.text = "Disable Battery Optimization"
            binding.btnBatteryOpt.setTextColor(requireContext().getColor(R.color.text_primary))
        }
    }

    override fun onResume() {
        super.onResume()
        updateBatteryButtonState()
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
