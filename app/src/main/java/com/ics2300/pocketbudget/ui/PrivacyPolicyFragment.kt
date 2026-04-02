package com.ics2300.pocketbudget.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.ics2300.pocketbudget.R

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

class PrivacyPolicyFragment : BottomSheetDialogFragment() {

    private var onAcceptListener: (() -> Unit)? = null

    private val imageGetter = Html.ImageGetter { source ->
        val resId = when (source) {
            "ic_data" -> R.drawable.ic_category
            "ic_storage" -> R.drawable.ic_shield_modern
            "ic_security" -> R.drawable.ic_privacy_outline
            "ic_permissions" -> R.drawable.ic_popup_reminder
            "ic_contact" -> R.drawable.ic_note
            "ic_check" -> R.drawable.ic_arrow_right
            else -> null
        }
        
        resId?.let {
            val drawable = ContextCompat.getDrawable(requireContext(), it)?.mutate()
            drawable?.let { d ->
                val size = (20 * resources.displayMetrics.density).toInt()
                d.setBounds(0, 0, size, size)
                d.setTint(ContextCompat.getColor(requireContext(), R.color.brand_dark_green))
                d
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_privacy_policy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Configure bottom sheet behavior
        setupBottomSheet()
        
        val contentTextView = view.findViewById<TextView>(R.id.text_privacy_policy_content)
        val closeButton = view.findViewById<View>(R.id.btn_privacy_policy_close)
        val acceptButton = view.findViewById<MaterialButton>(R.id.btn_privacy_policy_accept)
        
        // Format policy content with modern styling
        val policyContent = getFormattedPolicyContent()
        contentTextView.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(policyContent, Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(policyContent, imageGetter, null)
        }

        // Animate content entrance
        animateContentEntrance(contentTextView)
        
        closeButton.setOnClickListener {
            animateButtonClick(it)
            dismiss()
        }
        
        acceptButton.setOnClickListener {
            animateButtonClick(it)
            onAcceptListener?.invoke()
            dismiss()
        }
    }
    
    private fun setupBottomSheet() {
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.isHideable = true
                behavior.skipCollapsed = true
                
                // Add elevation and background
                it.background = null
                it.setBackgroundResource(android.R.color.transparent)
            }
        }
    }
    
    private fun getFormattedPolicyContent(): String {
        val colorPrimary = "#2E7D32"
        val colorSecondary = "#666666"
        
        return """
            <br>
            <b><font color="$colorPrimary">Data Collection</font></b><br>
            PocketBudget only accesses SMS messages related to M-Pesa transactions. We do not read personal messages or share any data with third parties.<br><br>
            
            <b><font color="$colorPrimary">Data Storage</font></b><br>
            All your financial data is stored locally on your device. We never upload your transaction history to any external servers.<br><br>
            
            <b><font color="$colorPrimary">Security</font></b><br>
            Your data is encrypted using Android's built-in security features. No one, including us, can access your information without your permission.<br><br>
            
            <font color="$colorSecondary"><b>What we track:</b></font> M-Pesa transaction amounts, dates, and descriptions for budgeting purposes.<br>
            <font color="$colorSecondary"><b>What we don't track:</b></font> Personal messages, contacts, location, or any other sensitive information.<br><br>
            
            <b><font color="$colorPrimary">Permissions</font></b><br>
            We only request SMS permissions to provide automatic expense tracking. You can revoke these permissions at any time from your device settings.<br><br>
            
            <b><font color="$colorPrimary">Contact Us</font></b><br>
            If you have questions about your privacy, please contact us at: <b>privacy@pocketbudget.com</b><br>
        """.trimIndent()
    }
    
    private fun animateContentEntrance(view: View) {
        view.apply {
            alpha = 0f
            translationY = 20f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
    
    private fun animateButtonClick(button: View) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    fun setOnAcceptListener(listener: () -> Unit) {
        onAcceptListener = listener
    }

    companion object {
        const val TAG = "PrivacyPolicyFragment"
        fun newInstance() = PrivacyPolicyFragment()
    }
}