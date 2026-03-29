package com.ics2300.pocketbudget.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.data.TransactionEntity
import com.ics2300.pocketbudget.databinding.BottomSheetTransactionDetailsBinding
import com.ics2300.pocketbudget.utils.CurrencyFormatter
import com.ics2300.pocketbudget.utils.SecurityUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionDetailsBottomSheet(
    private val transaction: TransactionEntity,
    private val categoryName: String = "Uncategorized",
    private val onChangeCategory: (TransactionEntity) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTransactionDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTransactionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isPrivacyMode = SecurityUtils.isPrivacyModeEnabled(requireContext())
        
        binding.textDetailAmount.text = CurrencyFormatter.formatKsh(transaction.amount, isPrivacyMode)
        binding.textDetailTypeBadge.text = transaction.type.uppercase()
        binding.textDetailCategoryName.text = categoryName
        binding.textDetailParty.text = if (isPrivacyMode) "****" else transaction.partyName
        
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        binding.textDetailDate.text = sdf.format(Date(transaction.timestamp))
        
        binding.textDetailCost.text = if (transaction.transactionCost != null) {
            CurrencyFormatter.formatKsh(transaction.transactionCost, isPrivacyMode)
        } else "Ksh 0.00"
        
        binding.textDetailBalance.text = if (transaction.balanceAfter != null) {
            CurrencyFormatter.formatKsh(transaction.balanceAfter, isPrivacyMode)
        } else "Ksh 0.00"
        
        binding.textDetailTransactionId.text = transaction.transactionId

        binding.btnChangeCategory.setOnClickListener {
            onChangeCategory(transaction)
            dismiss()
        }
        
        // Set icon & colors based on type
        when (transaction.type.lowercase()) {
            "received", "deposit" -> {
                binding.imgDetailIcon.setImageResource(android.R.drawable.stat_sys_download)
                binding.imgDetailIcon.setColorFilter(requireContext().getColor(R.color.brand_dark_green))
                binding.textDetailTypeBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.brand_dark_green))
                binding.textDetailAmount.setTextColor(requireContext().getColor(R.color.brand_dark_green))
            }
            else -> {
                binding.imgDetailIcon.setImageResource(android.R.drawable.ic_menu_send)
                binding.imgDetailIcon.setColorFilter(requireContext().getColor(R.color.status_error))
                binding.textDetailTypeBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.status_error))
                binding.textDetailAmount.setTextColor(requireContext().getColor(R.color.text_primary))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TransactionDetailsBottomSheet"
    }
}
