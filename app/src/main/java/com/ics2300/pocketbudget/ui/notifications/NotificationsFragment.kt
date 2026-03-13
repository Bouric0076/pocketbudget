package com.ics2300.pocketbudget.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.ui.theme.PocketbudgetTheme

class NotificationsFragment : Fragment() {

    private lateinit var viewModel: NotificationsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val repository = (requireActivity().application as MainApplication).notificationRepository
        val factory = NotificationsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[NotificationsViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PocketbudgetTheme(darkTheme = false) {
                    NotificationsScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            if (!findNavController().popBackStack()) {
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                        }
                    )
                }
            }
        }
    }
}
