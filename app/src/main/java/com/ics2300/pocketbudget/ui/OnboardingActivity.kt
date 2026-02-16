package com.ics2300.pocketbudget.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ics2300.pocketbudget.MainActivity
import com.ics2300.pocketbudget.R
import com.ics2300.pocketbudget.utils.AutoStartHelper

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: View
    private lateinit var tabLayout: TabLayout

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[android.Manifest.permission.READ_SMS] ?: false
        val receiveGranted = permissions[android.Manifest.permission.RECEIVE_SMS] ?: false
        if (readGranted && receiveGranted) {
            // Automatically move to next page if granted
            if (viewPager.currentItem < 2) { // Assuming permissions is page 1 or 2
                viewPager.currentItem += 1
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btn_next)
        btnSkip = findViewById(R.id.btn_skip)
        tabLayout = findViewById(R.id.tab_layout)

        // Set up Page Transformer for modern animation
        viewPager.setPageTransformer(ZoomOutPageTransformer())

        val pages = listOf(
            OnboardingPage(
                "Track Expenses",
                "Automatically track your M-Pesa transactions. No manual entry needed.",
                R.drawable.ic_launcher_foreground 
            ),
            OnboardingPage(
                "Permissions Needed",
                "PocketBudget needs SMS access to detect transactions and Notifications to alert you.",
                R.drawable.ic_launcher_foreground
            ),
            OnboardingPage(
                "Auto-Start Required",
                "For automatic tracking to work on your device, please enable 'Auto-start' permissions.",
                R.drawable.ic_launcher_foreground
            ),
            OnboardingPage(
                "Set Budgets",
                "Create monthly budgets for Food, Transport, and more to stay on track.",
                R.drawable.ic_launcher_foreground
            )
        )

        val adapter = OnboardingAdapter(pages)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val currentPage = pages[position]
                
                when {
                    currentPage.title.contains("Permissions") -> {
                        btnNext.text = "Grant Access"
                        btnNext.setOnClickListener {
                            requestPermissions()
                        }
                        btnSkip.visibility = View.VISIBLE
                    }
                    currentPage.title.contains("Auto-Start") -> {
                        btnNext.text = "Enable Auto-Start"
                        btnNext.setOnClickListener {
                             if (AutoStartHelper.isAutoStartPermissionAvailable(this@OnboardingActivity)) {
                                AutoStartHelper.requestAutoStartPermission(this@OnboardingActivity)
                            } else {
                                viewPager.currentItem += 1
                            }
                        }
                        btnSkip.visibility = View.VISIBLE
                    }
                    position == pages.size - 1 -> {
                        btnNext.text = "Get Started"
                        btnNext.setOnClickListener { finishOnboarding() }
                        btnSkip.visibility = View.INVISIBLE
                    }
                    else -> {
                        btnNext.text = "Next"
                        btnNext.setOnClickListener { viewPager.currentItem += 1 }
                        btnSkip.visibility = View.VISIBLE
                    }
                }
            }
        })

        // Initial click listener (will be updated by callback)
        btnNext.setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) {
                viewPager.currentItem += 1
            }
        }

        btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun finishOnboarding() {
        val prefs = getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_onboarding_completed", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

// Transformer for Animations
class ZoomOutPageTransformer : ViewPager2.PageTransformer {
    private val minScale = 0.85f
    private val minAlpha = 0.5f

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            val pageHeight = height
            when {
                position < -1 -> alpha = 0f
                position <= 1 -> { // [-1,1]
                    val scaleFactor = Math.max(minScale, 1 - Math.abs(position))
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horzMargin = pageWidth * (1 - scaleFactor) / 2
                    
                    translationX = if (position < 0) {
                        horzMargin - vertMargin / 2
                    } else {
                        -horzMargin + vertMargin / 2
                    }

                    scaleX = scaleFactor
                    scaleY = scaleFactor

                    alpha = (minAlpha + (((scaleFactor - minScale) / (1 - minScale)) * (1 - minAlpha)))
                }
                else -> alpha = 0f
            }
        }
    }
}

data class OnboardingPage(val title: String, val desc: String, val imageRes: Int)

class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img_onboarding)
        val title: TextView = itemView.findViewById(R.id.text_title)
        val desc: TextView = itemView.findViewById(R.id.text_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val page = pages[position]
        holder.title.text = page.title
        holder.desc.text = page.desc
        holder.img.setImageResource(page.imageRes)
        
        // Tint placeholder icons if needed
        holder.img.setColorFilter(holder.itemView.context.getColor(R.color.brand_dark_green))
    }

    override fun getItemCount(): Int = pages.size
}