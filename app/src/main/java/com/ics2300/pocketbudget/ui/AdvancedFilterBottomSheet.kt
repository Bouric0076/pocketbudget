package com.ics2300.pocketbudget.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ics2300.pocketbudget.databinding.BottomSheetAdvancedFilterBinding

class AdvancedFilterBottomSheet(
    private val initialMin: Double? = null,
    private val initialMax: Double? = null,
    private val initialActor: String? = null,
    private val onApply: (Double?, Double?, String?) -> Unit,
    private val onReset: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAdvancedFilterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAdvancedFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set initial values
        initialMin?.let { binding.editMinAmount.setText(it.toString()) }
        initialMax?.let { binding.editMaxAmount.setText(it.toString()) }
        initialActor?.let { binding.editActorName.setText(it) }

        binding.btnApplyFilters.setOnClickListener {
            val min = binding.editMinAmount.text.toString().toDoubleOrNull()
            val max = binding.editMaxAmount.text.toString().toDoubleOrNull()
            val actor = binding.editActorName.text.toString().takeIf { it.isNotBlank() }
            
            onApply(min, max, actor)
            dismiss()
        }

        binding.btnResetFilters.setOnClickListener {
            onReset()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AdvancedFilterBottomSheet"
    }
}
