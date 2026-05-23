package com.ics2300.pocketbudget.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.CategoryEntity
import com.ics2300.pocketbudget.databinding.FragmentSettingsBinding
import com.ics2300.pocketbudget.ui.dashboard.SyncResult
import com.ics2300.pocketbudget.ui.settings.SettingsViewModel
import com.ics2300.pocketbudget.ui.settings.SettingsViewModel.CategoryAction
import com.ics2300.pocketbudget.ui.settings.SettingsViewModel.CategoryActionResult
import com.ics2300.pocketbudget.ui.settings.SettingsViewModelFactory
import com.ics2300.pocketbudget.utils.AutoStartHelper
import com.ics2300.pocketbudget.utils.CategoryUtils
import com.ics2300.pocketbudget.utils.NotificationHelper
import com.ics2300.pocketbudget.utils.PremiumManager
import com.ics2300.pocketbudget.utils.SecurityUtils
import java.util.Calendar

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory((requireActivity().application as MainApplication).repository)
    }

    private var exportType = "pdf"
    private var exportStartDate: Long? = null
    private var exportEndDate: Long? = null
    private var suppressNotificationSwitchCallback = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            NotificationHelper.setNotificationsEnabled(requireContext(), granted)
            suppressNotificationSwitchCallback = true
            binding.switchNotifications.isChecked = granted
            suppressNotificationSwitchCallback = false

            val message = if (granted) {
                "Notifications enabled"
            } else {
                "Notification permission denied"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    showConfirmationDialog(
                        "Import Data",
                        "This will add transactions from the selected CSV file. Existing duplicate transactions will be skipped. Continue?"
                    ) {
                        viewModel.importData(requireContext(), uri)
                        Toast.makeText(context, "Import started...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    private fun updatePremiumUI() {
        val isPremium = PremiumManager.isPremium(requireContext())
        if (isPremium) {
            binding.textPremiumStatus.text = "Premium Active 👑"
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

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)

        btnBuy.setOnClickListener {
            PremiumManager.setPremiumStatus(requireContext(), true)
            updatePremiumUI()
            Toast.makeText(context, "Welcome to Premium! 🎉", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPremiumLockedDialog(feature: String) {
        val featureName = PremiumManager.getFeatureName(feature)
        showActionSheet(
            title = "Premium feature",
            message = "$featureName is available for Premium users only. Upgrade for Ksh 50/month to unlock PDF/CSV exports, imports, and advanced analytics.",
            primary = "Upgrade now",
            secondary = "Maybe later",
            onPrimary = {
                showPremiumPurchaseDialog()
            }
        )
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
        val options = listOf("All Time", "This Month", "Last Month", "This Year", "Custom Range")
        showOptionPicker("Select report period", options) { which ->
                val calendar = Calendar.getInstance()
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                when (which) {
                    0 -> {
                        exportStartDate = null
                        exportEndDate = null
                        onRangeSelected("All_Time")
                    }

                    1 -> {
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        exportStartDate = calendar.timeInMillis
                        val startStr = dateFormat.format(calendar.time)

                        calendar.add(Calendar.MONTH, 1)
                        calendar.add(Calendar.MILLISECOND, -1)
                        exportEndDate = calendar.timeInMillis
                        val endStr = dateFormat.format(calendar.time)

                        onRangeSelected("${startStr}_to_${endStr}")
                    }

                    2 -> {
                        calendar.add(Calendar.MONTH, -1)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        exportStartDate = calendar.timeInMillis
                        val startStr = dateFormat.format(calendar.time)

                        calendar.add(Calendar.MONTH, 1)
                        calendar.add(Calendar.MILLISECOND, -1)
                        exportEndDate = calendar.timeInMillis
                        val endStr = dateFormat.format(calendar.time)

                        onRangeSelected("${startStr}_to_${endStr}")
                    }

                    3 -> {
                        calendar.set(Calendar.DAY_OF_YEAR, 1)
                        exportStartDate = calendar.timeInMillis
                        val startStr = dateFormat.format(calendar.time)

                        calendar.add(Calendar.YEAR, 1)
                        calendar.add(Calendar.MILLISECOND, -1)
                        exportEndDate = calendar.timeInMillis
                        val endStr = dateFormat.format(calendar.time)

                        onRangeSelected("${startStr}_to_${endStr}")
                    }

                    4 -> {
                        showCustomDatePicker {
                            val start = java.util.Date(exportStartDate ?: 0)
                            val end = java.util.Date(exportEndDate ?: 0)
                            onRangeSelected("${dateFormat.format(start)}_to_${dateFormat.format(end)}")
                        }
                    }
                }
            }
    }

    private fun showCustomDatePicker(onRangeSelected: () -> Unit) {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select custom report range")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first
            val end = selection.second

            if (start == null || end == null || end < start) {
                Toast.makeText(context, "Select a valid date range", Toast.LENGTH_SHORT).show()
                return@addOnPositiveButtonClickListener
            }

            exportStartDate = start
            exportEndDate = end
            onRangeSelected()
        }

        picker.show(parentFragmentManager, "export_date_range_picker")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        updatePremiumUI()

        binding.btnUpgradePremium.setOnClickListener {
            showPremiumPurchaseDialog()
        }

        binding.btnManageCategories.setOnClickListener {
            showManageCategoriesDialog()
        }

        binding.btnExportData.setOnClickListener {
            if (!PremiumManager.canAccessFeature(requireContext(), PremiumManager.FEATURE_PDF_EXPORT)) {
                showPremiumLockedDialog(PremiumManager.FEATURE_PDF_EXPORT)
                return@setOnClickListener
            }

            val options = listOf("PDF Report (Recommended)", "CSV (Excel)")
            showOptionPicker("Export data", options) { which ->
                    if (which == 0) {
                        showDateRangeDialog { dateLabel ->
                            exportType = "pdf"
                            launchExportIntent("application/pdf", "PocketBudget_Report_$dateLabel.pdf")
                        }
                    } else {
                        showDateRangeDialog { dateLabel ->
                            exportType = "csv"
                            launchExportIntent("text/csv", "PocketBudget_Export_$dateLabel.csv")
                        }
                    }
                }
        }

        binding.btnImportData.setOnClickListener {
            if (!PremiumManager.canAccessFeature(requireContext(), PremiumManager.FEATURE_IMPORT_DATA)) {
                showPremiumLockedDialog(PremiumManager.FEATURE_IMPORT_DATA)
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
            }
            openDocumentLauncher.launch(intent)
        }

        binding.btnResync.setOnClickListener {
            showConfirmationDialog(
                "Resync Data",
                "This will re-scan all SMS messages to add any missing transactions. Existing data will be preserved. Continue?"
            ) {
                viewModel.resyncData()
                Toast.makeText(context, "Resync started...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBatteryOpt.setOnClickListener {
            checkAndRequestBatteryOptimization()
        }
        updateBatteryButtonState()

        if (AutoStartHelper.isAutoStartPermissionAvailable(requireContext())) {
            binding.btnAutoStart.visibility = View.VISIBLE
            binding.btnAutoStart.setOnClickListener {
                AutoStartHelper.requestAutoStartPermission(requireContext())
            }
        } else {
            binding.btnAutoStart.visibility = View.GONE
        }

        binding.btnClearData.setOnClickListener {
            showConfirmationDialog(
                "Clear Data",
                "Are you sure you want to delete all transactions and budgets? This cannot be undone."
            ) {
                viewModel.clearAllData()
                Toast.makeText(context, "All data cleared.", Toast.LENGTH_SHORT).show()
            }
        }

        setupNotificationSettings()

        setupSecuritySettings()

        viewModel.syncStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is SyncResult.Loading -> Unit

                is SyncResult.Success -> {
                    if (result.count > 0) {
                        Toast.makeText(
                            context,
                            "Sync complete. ${result.count} new transactions added.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Sync complete. No new transactions found.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                is SyncResult.Error -> {
                    Toast.makeText(context, "Sync failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.categories.observe(viewLifecycleOwner) { }

        viewModel.categoryActionStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is CategoryActionResult.Idle -> Unit

                is CategoryActionResult.Success -> {
                    Toast.makeText(
                        context,
                        buildCategoryActionMessage(result),
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.clearCategoryActionStatus()
                }

                is CategoryActionResult.Error -> {
                    Toast.makeText(
                        context,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.clearCategoryActionStatus()
                }
            }
        }

        binding.textViewVersionNumber.text = getAppVersionName()

        return root
    }

    private fun setupNotificationSettings() {
        binding.switchNotifications.isChecked = NotificationHelper.areNotificationsEnabled(requireContext())

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (suppressNotificationSwitchCallback) return@setOnCheckedChangeListener

            if (isChecked && requiresNotificationPermission()) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return@setOnCheckedChangeListener
            }

            NotificationHelper.setNotificationsEnabled(requireContext(), isChecked)
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        binding.containerNotifications.setOnClickListener {
            binding.switchNotifications.toggle()
        }
    }

    private fun requiresNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
    }

    private fun setupSecuritySettings() {
        val context = requireContext()

        binding.switchBiometric.isChecked = SecurityUtils.isSecurityEnabled(context)

        binding.switchBiometric.setOnClickListener {
            val isChecked = binding.switchBiometric.isChecked

            if (isChecked) {
                if (!SecurityUtils.hasPin(context)) {
                    showSetPinDialog()
                    binding.switchBiometric.isChecked = false
                } else {
                    SecurityUtils.setBiometricEnabled(context, true)
                    Toast.makeText(context, "App Lock Enabled", Toast.LENGTH_SHORT).show()
                }
            } else {
                showConfirmationDialog(
                    "Disable Security",
                    "This will remove the App Lock PIN. Continue?"
                ) {
                    SecurityUtils.clearSecurity(context)
                    binding.switchBiometric.isChecked = false
                    Toast.makeText(context, "App Lock Disabled", Toast.LENGTH_SHORT).show()
                }
                binding.switchBiometric.isChecked = true
            }
        }

        binding.btnChangePin.setOnClickListener {
            showSetPinDialog()
        }

        binding.switchPrivacyMode.isChecked = SecurityUtils.isPrivacyModeEnabled(context)
        binding.switchPrivacyMode.setOnCheckedChangeListener { _, isChecked ->
            SecurityUtils.setPrivacyModeEnabled(context, isChecked)
            val status = if (isChecked) "Enabled" else "Disabled"
            Toast.makeText(context, "Privacy Mode $status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildCategoryActionMessage(
        result: CategoryActionResult.Success
    ): String {
        return when (result.action) {
            CategoryAction.Added -> {
                if (result.recategorizedCount > 0) {
                    "Category added. ${result.recategorizedCount} transactions matched."
                } else {
                    "Category added. No existing transactions matched."
                }
            }

            CategoryAction.Updated -> {
                if (result.recategorizedCount > 0) {
                    "Category updated. ${result.recategorizedCount} transactions matched."
                } else {
                    "Category updated. No existing transactions matched."
                }
            }

            CategoryAction.Deleted -> {
                "Category deleted. Related transactions moved to Uncategorized."
            }
        }
    }

    private fun showSetPinDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_pin, null)
        val layoutPin = view.findViewById<TextInputLayout>(R.id.layout_pin)
        val pinInput = view.findViewById<TextInputEditText>(R.id.edit_pin)
        val saveButton = view.findViewById<MaterialButton>(R.id.btn_pin_save)
        val cancelButton = view.findViewById<MaterialButton>(R.id.btn_pin_cancel)

        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            val pin = pinInput.text?.toString().orEmpty()
            if (pin.length == 4) {
                SecurityUtils.setPin(requireContext(), pin)
                SecurityUtils.setBiometricEnabled(requireContext(), true)
                binding.switchBiometric.isChecked = true
                Toast.makeText(context, "PIN set and app lock enabled", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                layoutPin.error = "PIN must be 4 digits"
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showManageCategoriesDialog() {
        val categories = viewModel.categories.value.orEmpty()
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_manage_categories, null)
        val listContainer = view.findViewById<LinearLayout>(R.id.category_list_container)
        val addButton = view.findViewById<MaterialButton>(R.id.btn_add_category)
        val addTopButton = view.findViewById<MaterialButton>(R.id.btn_add_category_top)

        categories.forEach { category ->
            listContainer.addView(
                createCategoryRow(category) {
                    dialog.dismiss()
                    showEditCategoryDialog(category)
                }
            )
        }

        val showAddSheet = {
            dialog.dismiss()
            showAddCategoryDialog()
        }
        addButton.setOnClickListener { showAddSheet() }
        addTopButton.setOnClickListener { showAddSheet() }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showAddCategoryDialog() {
        showCategoryEditorSheet(category = null)
    }

    private fun showEditCategoryDialog(category: CategoryEntity) {
        showCategoryEditorSheet(category)
    }

    private fun showCategoryEditorSheet(category: CategoryEntity?) {
        val isEditing = category != null
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_category_editor, null)
        val title = view.findViewById<TextView>(R.id.text_category_editor_title)
        val subtitle = view.findViewById<TextView>(R.id.text_category_editor_subtitle)
        val nameInput = view.findViewById<TextInputEditText>(R.id.input_category_name)
        val keywordsInput = view.findViewById<TextInputEditText>(R.id.input_category_keywords)
        val iconContainer = view.findViewById<LinearLayout>(R.id.icon_container)
        val colorContainer = view.findViewById<LinearLayout>(R.id.color_container)
        val deleteButton = view.findViewById<MaterialButton>(R.id.btn_delete_category)
        val cancelButton = view.findViewById<MaterialButton>(R.id.btn_cancel_category)
        val saveButton = view.findViewById<MaterialButton>(R.id.btn_save_category)

        title.text = if (isEditing) "Edit ${category?.name}" else "Add category"
        subtitle.text = if (isEditing) {
            "Changes apply to future M-Pesa messages and can match existing uncategorized transactions."
        } else {
            "Keywords help PocketBudget match existing uncategorized transactions and future M-Pesa messages."
        }

        nameInput.setText(category?.name.orEmpty())
        keywordsInput.setText(category?.keywords.orEmpty())
        saveButton.text = if (isEditing) "Update" else "Save"

        var selectedIcon = category?.iconName ?: "ic_default"
        var selectedColor = category?.colorHex ?: "#0A3D2E"

        fun refreshIconSelection() {
            for (index in 0 until iconContainer.childCount) {
                val child = iconContainer.getChildAt(index)
                child.background = if (child.tag == selectedIcon) {
                    selectedCircleBackground()
                } else {
                    null
                }
            }
        }

        fun refreshColorSelection() {
            for (index in 0 until colorContainer.childCount) {
                val child = colorContainer.getChildAt(index)
                child.alpha = if (child.tag == selectedColor) 1f else 0.45f
            }
        }

        CategoryUtils.iconMap.forEach { (iconName, resId) ->
            val iconView = ImageView(requireContext()).apply {
                tag = iconName
                setImageResource(resId)
                setColorFilter(requireContext().getColor(R.color.brand_dark_green))
                setPadding(dp(12))
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                    marginEnd = dp(10)
                }
                setOnClickListener {
                    selectedIcon = iconName
                    refreshIconSelection()
                }
            }
            iconContainer.addView(iconView)
        }

        CategoryUtils.availableColors.forEach { colorHex ->
            val colorView = View(requireContext()).apply {
                tag = colorHex
                background = colorCircleBackground(colorHex)
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply {
                    marginEnd = dp(10)
                }
                setOnClickListener {
                    selectedColor = colorHex
                    refreshColorSelection()
                }
            }
            colorContainer.addView(colorView)
        }

        refreshIconSelection()
        refreshColorSelection()

        deleteButton.visibility = if (isEditing) View.VISIBLE else View.GONE
        deleteButton.setOnClickListener {
            val categoryToDelete = category ?: return@setOnClickListener
            dialog.dismiss()
            showConfirmationDialog(
                "Delete Category",
                "Delete '${categoryToDelete.name}'? Related transactions will move to Uncategorized."
            ) {
                viewModel.deleteCategory(categoryToDelete.id)
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            val name = nameInput.text?.toString()?.trim().orEmpty()
            val keywords = keywordsInput.text?.toString()?.trim()?.uppercase().orEmpty()

            if (name.isBlank()) {
                nameInput.error = "Name required"
                return@setOnClickListener
            }

            if (category != null) {
                viewModel.updateCategory(
                    category.copy(
                        name = name,
                        keywords = keywords,
                        iconName = selectedIcon,
                        colorHex = selectedColor
                    )
                )
            } else {
                viewModel.addCategory(name, keywords, selectedIcon, selectedColor)
            }

            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun createCategoryRow(
        category: CategoryEntity,
        onClick: () -> Unit
    ): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = requireContext().getDrawable(R.drawable.bg_card_white)
            setOnClickListener { onClick() }
        }

        val icon = ImageView(requireContext()).apply {
            setImageResource(CategoryUtils.getIconResId(category.iconName))
            setColorFilter(CategoryUtils.getColor(category.colorHex))
            setPadding(dp(10))
            background = selectedCircleBackground()
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        }

        val textContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            }
        }

        val title = TextView(requireContext()).apply {
            text = category.name
            setTextColor(requireContext().getColor(R.color.text_primary))
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val keywords = TextView(requireContext()).apply {
            text = category.keywords.ifBlank { "No keywords yet" }
            setTextColor(requireContext().getColor(R.color.text_secondary))
            textSize = 12f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        textContainer.addView(title)
        textContainer.addView(keywords)
        row.addView(icon)
        row.addView(textContainer)

        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(8)
        }

        return row
    }

    private fun selectedCircleBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(requireContext().getColor(R.color.onboarding_chip_green))
            setStroke(dp(1), requireContext().getColor(R.color.brand_dark_green))
        }
    }

    private fun colorCircleBackground(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(colorHex))
            setStroke(dp(2), Color.WHITE)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager =
                requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
        }

        return true
    }

    private fun checkAndRequestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = requireContext().packageName
            val powerManager =
                requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
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

    private fun getAppVersionName(): String {
        return try {
            val packageInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName,
                0
            )

            "v${packageInfo.versionName ?: "1.0"}"
        } catch (e: Exception) {
            "v1.0"
        }
    }

    private fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        showActionSheet(
            title = title,
            message = message,
            primary = "Confirm",
            secondary = "Cancel",
            onPrimary = onConfirm
        )
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
        view.findViewById<TextView>(R.id.text_prompt_title).text = title
        view.findViewById<TextView>(R.id.text_prompt_message).text = message
        view.findViewById<MaterialButton>(R.id.btn_prompt_primary).apply {
            text = primary
            setOnClickListener {
                dialog.dismiss()
                onPrimary()
            }
        }
        view.findViewById<MaterialButton>(R.id.btn_prompt_secondary).apply {
            text = secondary
            setOnClickListener { dialog.dismiss() }
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showOptionPicker(
        title: String,
        options: List<String>,
        onSelected: (Int) -> Unit
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_list_picker, null)
        val container = view.findViewById<LinearLayout>(R.id.list_picker_container)
        view.findViewById<TextView>(R.id.text_picker_title).text = title

        options.forEachIndexed { index, label ->
            val row = TextView(requireContext()).apply {
                text = label
                textSize = 15f
                setTextColor(requireContext().getColor(R.color.text_primary))
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                gravity = android.view.Gravity.CENTER_VERTICAL
                minHeight = dp(50)
                setPadding(dp(14), 0, dp(14), 0)
                background = requireContext().getDrawable(R.drawable.bg_card_white)
                setOnClickListener {
                    dialog.dismiss()
                    onSelected(index)
                }
            }
            container.addView(row)
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
