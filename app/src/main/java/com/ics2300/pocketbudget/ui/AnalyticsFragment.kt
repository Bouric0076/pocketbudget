package com.ics2300.pocketbudget.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.ui.analytics.AnalyticsViewModel
import com.ics2300.pocketbudget.ui.analytics.AnalyticsViewModelFactory
import com.ics2300.pocketbudget.ui.theme.PocketbudgetTheme

class AnalyticsFragment : Fragment() {

    private val viewModel: AnalyticsViewModel by viewModels {
        AnalyticsViewModelFactory((requireActivity().application as MainApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PocketbudgetTheme(darkTheme = false) {
                    AnalyticsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
