package com.ics2300.pocketbudget.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ics2300.pocketbudget.MainActivity
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.utils.AutoStartHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: MaterialButton
    private lateinit var tabLayout: TabLayout
    private lateinit var privacyPolicyLink: TextView
    private lateinit var bottomSheetContainer: View
    
    private var currentAnimator: Animator? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[android.Manifest.permission.READ_SMS] ?: false
        val receiveGranted = permissions[android.Manifest.permission.RECEIVE_SMS] ?: false
        if (readGranted && receiveGranted) {
            if (viewPager.currentItem < 3) {
                viewPager.setCurrentItem(viewPager.currentItem + 1, true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        initViews()
        setupViewPager()
        setupWindowInsets()
        animateBackground()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btn_next)
        btnSkip = findViewById(R.id.btn_skip)
        tabLayout = findViewById(R.id.tab_layout)
        privacyPolicyLink = findViewById(R.id.text_privacy_policy_link)
        bottomSheetContainer = findViewById(R.id.bottom_sheet_container)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.controls_container)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }
    }

    private fun animateBackground() {
        val backgroundView = findViewById<View>(R.id.animated_background)
        backgroundView.alpha = 0f
        backgroundView.animate()
            .alpha(1f)
            .setDuration(800)
            .start()
    }

    private fun setupViewPager() {
        viewPager.setPageTransformer(ModernPageTransformer())
        
        val pages = listOf(
            OnboardingPage(
                title = "Track Expenses",
                subtitle = "Smart & Automatic",
                description = "Automatically track your M-Pesa transactions. No manual entry needed.",
                iconRes = R.drawable.ic_money,
                gradientStart = R.color.brand_dark_green,
                gradientEnd = R.color.brand_light_green
            ),
            OnboardingPage(
                title = "Why SMS access?",
                subtitle = "Privacy First",
                description = "PocketBudget uses SMS to detect transactions. Your data is encrypted and stays locally on your device.",
                iconRes = R.drawable.ic_privacy_outline,
                gradientStart = R.color.brand_dark_green,
                gradientEnd = R.color.brand_light_green
            ),
            OnboardingPage(
                title = "Auto-Start Required",
                subtitle = "Always Tracking",
                description = "For automatic tracking to work on your device, please enable 'Auto-start' permissions.",
                iconRes = R.drawable.ic_popup_reminder,
                gradientStart = R.color.brand_dark_green,
                gradientEnd = R.color.brand_light_green
            ),
            OnboardingPage(
                title = "Set Budgets",
                subtitle = "Stay on Track",
                description = "Create monthly budgets for Food, Transport, and more to stay on top of your finances.",
                iconRes = R.drawable.ic_category,
                gradientStart = R.color.brand_dark_green,
                gradientEnd = R.color.brand_light_green
            )
        )

        val adapter = ModernOnboardingAdapter(pages)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Custom indicator behavior handled by selector
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateUIForPage(position, pages)
                animateBottomSheetBounce()
                animatePageTransition(position)
            }
        })

        updateUIForPage(0, pages)
        animateInitialEntrance()
    }

    private fun animatePageTransition(position: Int) {
        // Smooth crossfade animation for page content
        val currentPage = viewPager.getChildAt(0)
        currentPage?.animate()
            ?.alpha(0.5f)
            ?.setDuration(200)
            ?.withEndAction {
                currentPage.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            ?.start()
    }

    private fun updateUIForPage(position: Int, pages: List<OnboardingPage>) {
        val currentPage = pages[position]
        
        when {
            currentPage.title.contains("Why SMS access?") -> {
                btnNext.text = "Review & Grant"
                btnNext.setOnClickListener {
                    animateButtonClick(btnNext)
                    showProminentDisclosure()
                }
                btnSkip.isVisible = true
                privacyPolicyLink.isVisible = true
                privacyPolicyLink.setOnClickListener {
                    animateButtonClick(privacyPolicyLink)
                    showPrivacyPolicy()
                }
                animateButtonTextChange("Review & Grant")
            }
            currentPage.title.contains("Auto-Start") -> {
                btnNext.text = "Enable Auto-Start"
                btnNext.setOnClickListener {
                    animateButtonClick(btnNext)
                    if (AutoStartHelper.isAutoStartPermissionAvailable(this@OnboardingActivity)) {
                        AutoStartHelper.requestAutoStartPermission(this@OnboardingActivity)
                    } else {
                        viewPager.setCurrentItem(position + 1, true)
                    }
                }
                btnSkip.isVisible = true
                privacyPolicyLink.isVisible = false
                animateButtonTextChange("Enable Auto-Start")
            }
            position == pages.size - 1 -> {
                btnNext.text = "Get Started"
                btnNext.setOnClickListener {
                    animateButtonClick(btnNext)
                    finishOnboardingWithAnimation()
                }
                btnSkip.isVisible = false
                privacyPolicyLink.isVisible = false
                animateButtonTextChange("Get Started")
            }
            else -> {
                btnNext.text = "Next"
                btnNext.setOnClickListener {
                    animateButtonClick(btnNext)
                    viewPager.setCurrentItem(position + 1, true)
                }
                btnSkip.isVisible = true
                privacyPolicyLink.isVisible = false
                animateButtonTextChange("Next")
            }
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

    private fun animateButtonTextChange(newText: String) {
        currentAnimator?.cancel()
        
        val fadeOut = ObjectAnimator.ofFloat(btnNext, "alpha", 1f, 0f).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
        }
        
        fadeOut.doOnEnd {
            btnNext.text = newText
            val fadeIn = ObjectAnimator.ofFloat(btnNext, "alpha", 0f, 1f).apply {
                duration = 150
                interpolator = DecelerateInterpolator()
                start()
            }
            currentAnimator = fadeIn
        }
        
        fadeOut.start()
        currentAnimator = fadeOut
    }

    private fun animateBottomSheetBounce() {
        findViewById<View>(R.id.controls_container).animate()
            .translationY(-8f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                findViewById<View>(R.id.controls_container).animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun animateInitialEntrance() {
        findViewById<View>(R.id.controls_container).apply {
            translationY = 200f
            alpha = 0f
            animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        
        viewPager.apply {
            alpha = 0f
            scaleX = 0.98f
            scaleY = 0.98f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(100)
                .start()
        }
    }

    private fun showProminentDisclosure() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_prominent_disclosure, null)
        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_Pocketbudget_Dialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btn_disclosure_accept).setOnClickListener {
            animateButtonClick(it)
            dialog.dismiss()
            requestPermissions()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_disclosure_decline).setOnClickListener {
            animateButtonClick(it)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showPrivacyPolicy() {
        PrivacyPolicyFragment.newInstance().show(supportFragmentManager, PrivacyPolicyFragment.TAG)
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun finishOnboardingWithAnimation() {
        findViewById<View>(R.id.controls_container).animate()
            .translationY(300f)
            .alpha(0f)
            .setDuration(400)
            .start()
            
        viewPager.animate()
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(400)
            .withEndAction {
                finishOnboarding()
            }
            .start()
    }

    private fun finishOnboarding() {
        val prefs = getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_onboarding_completed", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        finish()
    }
}

// Modern Page Transformer with smooth animations
class ModernPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.apply {
            when {
                position < -1 -> alpha = 0f
                position <= 1 -> {
                    // Modern scale and fade effect
                    val scaleFactor = 0.85f + (1 - Math.abs(position)) * 0.15f
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    
                    // Smooth fade
                    alpha = 1 - Math.abs(position) * 0.25f
                    
                    // Subtle translation with depth
                    translationX = -position * width * 0.15f
                    translationZ = -Math.abs(position) * 10f
                }
                else -> alpha = 0f
            }
        }
    }
}

// Modern Onboarding Page Data Class
data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val iconRes: Int,
    val gradientStart: Int,
    val gradientEnd: Int
)

// Modern Adapter with Enhanced Compose UI
class ModernOnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<ModernOnboardingAdapter.ModernOnboardingViewHolder>() {

    inner class ModernOnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val composeView: ComposeView = itemView.findViewById(R.id.img_onboarding)
        val title: TextView = itemView.findViewById(R.id.text_title)
        val subtitle: TextView = itemView.findViewById(R.id.text_subtitle)
        val desc: TextView = itemView.findViewById(R.id.text_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModernOnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return ModernOnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModernOnboardingViewHolder, position: Int) {
        val page = pages[position]
        holder.title.text = page.title
        holder.subtitle.text = page.subtitle
        holder.desc.text = page.description
        
        holder.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ModernOnboardingIllustration(
                    iconRes = page.iconRes,
                    gradientStart = page.gradientStart,
                    gradientEnd = page.gradientEnd
                )
            }
        }
        
        // Modern staggered entrance animation
        listOf(holder.subtitle, holder.title, holder.desc).forEachIndexed { index, view ->
            view.apply {
                alpha = 0f
                translationY = 30f
                postDelayed({
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }, (position * 100 + index * 80).toLong())
            }
        }
    }

    override fun getItemCount(): Int = pages.size
}

@Composable
fun ModernOnboardingIllustration(
    iconRes: Int,
    gradientStart: Int,
    gradientEnd: Int
) {
    val startColor = colorResource(id = gradientStart)
    val endColor = colorResource(id = gradientEnd)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(startColor.copy(alpha = 0.08f), endColor.copy(alpha = 0.02f)),
                    radius = 400f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated floating effect
        val infiniteTransition = rememberInfiniteTransition()
        val floatY by infiniteTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        )
        
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        )
        
        Box(
            modifier = Modifier
                .offset(y = floatY.dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(startColor.copy(alpha = 0.2f), endColor.copy(alpha = 0.05f)),
                        radius = 150f
                    )
                )
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .align(Alignment.Center)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(startColor.copy(alpha = 0.1f), CircleShape),
                colorFilter = ColorFilter.tint(startColor)
            )
        }
    }
}