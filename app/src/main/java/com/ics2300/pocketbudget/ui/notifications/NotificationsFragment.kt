package com.ics2300.pocketbudget.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ics2300.pocketbudget.MainApplication
import com.ics2300.pocketbudget.R

class NotificationsFragment : Fragment() {

    private lateinit var viewModel: NotificationsViewModel
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_notifications_list, container, false)
        
        // Setup Toolbar
        val toolbar = root.findViewById<Toolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val recyclerView = root.findViewById<RecyclerView>(R.id.recyclerViewNotifications)
        val emptyState = root.findViewById<TextView>(R.id.textEmptyState)

        val repository = (requireActivity().application as MainApplication).notificationRepository
        val factory = NotificationsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[NotificationsViewModel::class.java]

        adapter = NotificationsAdapter { notification ->
            if (!notification.isRead) {
                viewModel.markAsRead(notification)
            }
            // Handle navigation or action if needed
        }
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            adapter.submitList(notifications)
            if (notifications.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        setupMenu()

        return root
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // menuInflater.inflate(R.menu.notifications_menu, menu) // Create this menu if needed
                menu.add(0, 1, 0, "Mark all as read")
                menu.add(0, 2, 0, "Clear all")
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    1 -> {
                        viewModel.markAllAsRead()
                        true
                    }
                    2 -> {
                        viewModel.clearAll()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}
