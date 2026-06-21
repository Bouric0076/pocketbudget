package com.ics2300.pocketbudget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.ics2300.pocketbudget.utils.AutoStartHelper

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ics2300.pocketbudget.data.NotificationRepository

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var notificationRepository: NotificationRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.post {
            handleNavigationIntent(intent, navController)
        }
        
        // Prompt for Auto-Start on supported devices (Tecno, Xiaomi, etc.)
        if (AutoStartHelper.isAutoStartPermissionAvailable(this) && !AutoStartHelper.hasPrompted(this)) {
            showAutoStartPrompt {
                    AutoStartHelper.requestAutoStartPermission(this)
                    AutoStartHelper.setPrompted(this)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        handleNavigationIntent(intent, navHostFragment.navController)
    }

    private fun handleNavigationIntent(
        intent: android.content.Intent?,
        navController: NavController
    ) {
        val target = intent?.getStringExtra("navigate_to") ?: return

        when (target) {
            "notifications" -> {
                val notificationId = intent.getIntExtra("notification_id", -1)
                if (notificationId != -1) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        notificationRepository.markAsRead(notificationId)
                    }
                }
                navController.navigate(R.id.notificationsFragment)
            }

            "transaction_edit" -> {
                val transactionId = intent.getStringExtra("transaction_id") ?: return
                navController.navigate(R.id.transactionsFragment)
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("open_transaction_category_id", transactionId)
            }
        }

        intent.removeExtra("navigate_to")
        intent.removeExtra("notification_id")
        intent.removeExtra("transaction_id")
        intent.removeExtra("action_data")
    }

    private fun showAutoStartPrompt(onEnable: () -> Unit) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_action_prompt, null)
        view.findViewById<android.widget.TextView>(R.id.text_prompt_title).text = "Keep automatic tracking active"
        view.findViewById<android.widget.TextView>(R.id.text_prompt_message).text =
            "Enable Auto-start so PocketBudget can keep reading new M-Pesa messages on supported devices."
        view.findViewById<MaterialButton>(R.id.btn_prompt_primary).apply {
            text = "Enable now"
            setOnClickListener {
                dialog.dismiss()
                onEnable()
            }
        }
        view.findViewById<MaterialButton>(R.id.btn_prompt_secondary).apply {
            text = "Later"
            setOnClickListener {
                AutoStartHelper.setPrompted(this@MainActivity)
                dialog.dismiss()
            }
        }
        dialog.setContentView(view)
        dialog.show()
    }
}
