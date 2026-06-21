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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: TextView          // changed: plain TextView, not MaterialButton
    private lateinit var tabLayout: TabLayout
    private lateinit var privacyPolicyLink: TextView

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
        animateInitialEntrance()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btn_next)
        btnSkip = findViewById(R.id.btn_skip)
        tabLayout = findViewById(R.id.tab_layout)
        privacyPolicyLink = findViewById(R.id.text_privacy_policy_link)
        privacyPolicyLink.setOnClickListener {
            pulseButton(it)
            showPrivacyPolicy()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.controls_container)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 16
            }
            insets
        }
    }

    private fun setupViewPager() {
        viewPager.setPageTransformer(SubtlePageTransformer())

        val pages = listOf(
            OnboardingPage(
                title = "M-Pesa spending,\nclearly tracked",
                chipLabel = "Smart tracking",
                description = "PocketBudget turns confirmed M-Pesa SMS into organized transactions without manual entry.",
                iconRes = R.drawable.ic_money,
                heroBackground = R.color.onboarding_hero_green,
                accentColor = R.color.brand_dark_green,
                chipBackground = R.color.onboarding_chip_green,
                pageType = PageType.STANDARD
            ),
            OnboardingPage(
                title = "Private by design,\nwith your consent",
                chipLabel = "SMS permission",
                description = "We only scan M-Pesa messages on this phone. Your budget data stays local and you can revoke access anytime.",
                iconRes = R.drawable.ic_privacy_outline,
                heroBackground = R.color.onboarding_hero_indigo,
                accentColor = R.color.brand_indigo,
                chipBackground = R.color.onboarding_chip_indigo,
                pageType = PageType.PERMISSIONS
            ),
            OnboardingPage(
                title = "Know your limits\nbefore you overspend",
                chipLabel = "Budget goals",
                description = "Set category budgets for food, transport, utilities, and more, then spot pressure early.",
                iconRes = R.drawable.ic_category,
                heroBackground = R.color.onboarding_hero_amber,
                accentColor = R.color.brand_amber,
                chipBackground = R.color.onboarding_chip_amber,
                pageType = PageType.STANDARD
            ),
            OnboardingPage(
                title = "Ready for a\nclearer budget",
                chipLabel = "Ready",
                description = "Start with your dashboard, sync recent transactions, and adjust categories as you learn your spending.",
                iconRes = R.drawable.ic_check_circle,
                heroBackground = R.color.brand_dark_green,
                accentColor = R.color.brand_dark_green,
                chipBackground = R.color.brand_dark_green,
                pageType = PageType.FINISH
            )
        )

        val adapter = OnboardingAdapter(pages)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateControlsForPage(position, pages)
            }
        })

        updateControlsForPage(0, pages)
    }

    private fun updateControlsForPage(position: Int, pages: List<OnboardingPage>) {
        val page = pages[position]
        val isLastPage = position == pages.size - 1

        when (page.pageType) {
            PageType.PERMISSIONS -> {
                animateButtonTo("Review SMS access", R.color.brand_indigo)
                btnNext.setOnClickListener {
                    pulseButton(btnNext)
                    showPrivacyPolicy(requestPermissionOnAccept = true)
                }
                btnSkip.setOnClickListener {
                    pulseButton(btnSkip)
                    finishOnboardingWithAnimation()
                }
                btnSkip.isVisible = true
                privacyPolicyLink.isVisible = true
            }
            PageType.FINISH -> {
                animateButtonTo("Get started", R.color.brand_dark_green)
                btnNext.setOnClickListener {
                    pulseButton(btnNext)
                    finishOnboardingWithAnimation()
                }
                btnSkip.isVisible = false
                privacyPolicyLink.isVisible = false
            }
            else -> {
                val label = if (isLastPage) "Get started" else "Next"
                animateButtonTo(label, R.color.brand_dark_green)
                btnNext.setOnClickListener {
                    pulseButton(btnNext)
                    if (isLastPage) {
                        finishOnboardingWithAnimation()
                    } else {
                        viewPager.setCurrentItem(position + 1, true)
                    }
                }
                if (!isLastPage) {
                    btnSkip.setOnClickListener {
                        pulseButton(btnSkip)
                        finishOnboardingWithAnimation()
                    }
                }
                btnSkip.isVisible = !isLastPage
                privacyPolicyLink.isVisible = false
            }
        }

    }

    private fun animateButtonTo(label: String, colorRes: Int) {
        currentAnimator?.cancel()
        val fadeOut = ObjectAnimator.ofFloat(btnNext, "alpha", 1f, 0f).apply {
            duration = 120
            interpolator = DecelerateInterpolator()
        }
        fadeOut.doOnEnd {
            btnNext.text = label
            btnNext.setBackgroundColor(getColor(colorRes))
            val fadeIn = ObjectAnimator.ofFloat(btnNext, "alpha", 0f, 1f).apply {
                duration = 120
                interpolator = DecelerateInterpolator()
            }
            fadeIn.start()
            currentAnimator = fadeIn
        }
        fadeOut.start()
        currentAnimator = fadeOut
    }

    private fun pulseButton(view: View) {
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
            .withEndAction { view.animate().scaleX(1f).scaleY(1f).setDuration(80).start() }
            .start()
    }

    private fun animateInitialEntrance() {
        val controls = findViewById<View>(R.id.controls_container)
        controls.translationY = 180f
        controls.alpha = 0f
        controls.animate().translationY(0f).alpha(1f)
            .setDuration(500).setStartDelay(80)
            .setInterpolator(DecelerateInterpolator()).start()

        viewPager.alpha = 0f
        viewPager.animate().alpha(1f).setDuration(400).start()
    }

    private fun showPrivacyPolicy(requestPermissionOnAccept: Boolean = false) {
        PrivacyPolicyFragment.newInstance().apply {
            if (requestPermissionOnAccept) {
                setOnAcceptListener { requestPermissions() }
            }
        }.show(supportFragmentManager, PrivacyPolicyFragment.TAG)
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
            .translationY(240f).alpha(0f).setDuration(350).start()
        viewPager.animate().alpha(0f).setDuration(350)
            .withEndAction { finishOnboarding() }.start()
    }

    private fun finishOnboarding() {
        getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("is_onboarding_completed", true).apply()
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

// ─── Page Transformer ──────────────────────────────────────────────────────────

class SubtlePageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        when {
            position < -1 -> view.alpha = 0f
            position <= 1 -> {
                view.alpha = 1f - (Math.abs(position) * 0.3f)
                view.translationX = -position * view.width * 0.08f
            }
            else -> view.alpha = 0f
        }
    }
}

// ─── Data ──────────────────────────────────────────────────────────────────────

enum class PageType { STANDARD, PERMISSIONS, FINISH }

data class OnboardingPage(
    val title: String,
    val chipLabel: String,
    val description: String,
    val iconRes: Int,
    val heroBackground: Int,     // color resource
    val accentColor: Int,        // color resource for icon tint & button
    val chipBackground: Int,     // color resource for chip bg
    val pageType: PageType = PageType.STANDARD
)

// ─── Adapter ───────────────────────────────────────────────────────────────────

class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val composeHero: ComposeView = view.findViewById(R.id.compose_hero)
        val chipText: TextView = view.findViewById(R.id.text_chip)
        val title: TextView = view.findViewById(R.id.text_title)
        val desc: TextView = view.findViewById(R.id.text_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = pages[position]

        holder.chipText.text = page.chipLabel
        holder.title.text = page.title
        holder.desc.text = page.description

        holder.composeHero.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OnboardingHero(page = page)
            }
        }

        // Staggered text entrance
        listOf(holder.chipText, holder.title, holder.desc).forEachIndexed { i, v ->
            v.alpha = 0f; v.translationY = 20f
            v.postDelayed({
                v.animate().alpha(1f).translationY(0f)
                    .setDuration(380)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }, (i * 70).toLong())
        }
    }

    override fun getItemCount() = pages.size
}

// ─── Compose Hero ──────────────────────────────────────────────────────────────

@Composable
fun OnboardingHero(page: OnboardingPage) {
    val bgColor = colorResource(id = page.heroBackground)
    val accentColor = colorResource(id = page.accentColor)

    val isFinishPage = page.pageType == PageType.FINISH
    val isPermissionsPage = page.pageType == PageType.PERMISSIONS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor.copy(alpha = if (isFinishPage) 1f else 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (isPermissionsPage) {
            PermissionExplanationVisuals(accentColor)
        } else if (isFinishPage) {
            // Finish page: white circle on solid green
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = page.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        } else {
            // Standard pages: layered circles
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = page.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionExplanationVisuals(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "flow")
    
    val time = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stage"
    )

    val currentStage = time.value

    val smsAlpha = when {
        currentStage < 0.5f -> currentStage / 0.5f
        currentStage < 2.5f -> 1f
        currentStage < 3.0f -> 1f - (currentStage - 2.5f) / 0.5f
        else -> 0f
    }
    val smsTranslationY = when {
        currentStage < 0.5f -> (-40f * (1f - (currentStage / 0.5f))).dp
        else -> 0.dp
    }

    val arrowAlpha = when {
        currentStage < 0.8f -> 0f
        currentStage < 1.3f -> (currentStage - 0.8f) / 0.5f
        currentStage < 2.5f -> 1f
        currentStage < 3.0f -> 1f - (currentStage - 2.5f) / 0.5f
        else -> 0f
    }

    val cardAlpha = when {
        currentStage < 1.5f -> 0f
        currentStage < 2.0f -> (currentStage - 1.5f) / 0.5f
        currentStage < 3.5f -> 1f
        currentStage < 4.0f -> 1f - (currentStage - 3.5f) / 0.5f
        else -> 0f
    }
    val cardTranslationY = when {
        currentStage < 1.5f -> 0.dp
        currentStage < 2.0f -> (40f * (1f - ((currentStage - 1.5f) / 0.5f))).dp
        else -> 0.dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // SMS Bubble
        Box(
            modifier = Modifier
                .offset(y = smsTranslationY)
                .alpha(smsAlpha)
                .fillMaxWidth(0.9f)
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Text(text = "✉️", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Text(
                        text = "M-PESA",
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = "Ksh 1,200 paid to Naivas Supermarket on 21/6/26.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // Connecting Arrow
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .alpha(arrowAlpha)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(text = "⬇️", fontSize = 20.sp, color = accentColor)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Categorized card in application
        Box(
            modifier = Modifier
                .offset(y = cardTranslationY)
                .alpha(cardAlpha)
                .fillMaxWidth(0.9f)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(text = "🛒", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Text(
                        text = "Groceries (Naivas)",
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Progress indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFEEEEEE))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight()
                                .background(accentColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Text(
                    text = "- Ksh 1,200",
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            }
        }
    }
}
