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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.ics2300.pocketbudget.R

import com.ics2300.pocketbudget.utils.AutoStartHelper

import androidx.activity.result.contract.ActivityResultContracts

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory((requireActivity().application as MainApplication).repository)
    }

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.exportData(requireContext(), uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.btnManageCategories.setOnClickListener {
            // Show dialog to add/edit categories
            showManageCategoriesDialog()
        }
        
        binding.btnExportData.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, "pocketbudget_export_${System.currentTimeMillis()}.csv")
            }
            createDocumentLauncher.launch(intent)
        }

        binding.btnResync.setOnClickListener {
            showConfirmationDialog("Resync Data", "This will clear current data and re-read all SMS messages. Continue?") {
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

        return root
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
        val inputLayout = android.widget.LinearLayout(context)
        inputLayout.orientation = android.widget.LinearLayout.VERTICAL
        inputLayout.setPadding(50, 40, 50, 10)

        val nameInput = android.widget.EditText(context)
        nameInput.hint = "Category Name (e.g. Pets)"
        inputLayout.addView(nameInput)

        val keywordsInput = android.widget.EditText(context)
        keywordsInput.hint = "Keywords (comma separated, e.g. VET,FOOD)"
        inputLayout.addView(keywordsInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Category")
            .setView(inputLayout)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val keywords = keywordsInput.text.toString().trim().uppercase()
                if (name.isNotEmpty()) {
                    viewModel.addCategory(name, keywords)
                    Toast.makeText(context, "Category added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditCategoryDialog(category: com.ics2300.pocketbudget.data.CategoryEntity) {
        val inputLayout = android.widget.LinearLayout(context)
        inputLayout.orientation = android.widget.LinearLayout.VERTICAL
        inputLayout.setPadding(50, 40, 50, 10)

        val nameInput = android.widget.EditText(context)
        nameInput.setText(category.name)
        nameInput.hint = "Category Name"
        inputLayout.addView(nameInput)

        val keywordsInput = android.widget.EditText(context)
        keywordsInput.setText(category.keywords)
        keywordsInput.hint = "Keywords (comma separated)"
        inputLayout.addView(keywordsInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit ${category.name}")
            .setView(inputLayout)
            .setPositiveButton("Update") { _, _ ->
                val name = nameInput.text.toString().trim()
                val keywords = keywordsInput.text.toString().trim().uppercase()
                if (name.isNotEmpty()) {
                    viewModel.updateCategory(category.copy(name = name, keywords = keywords))
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
