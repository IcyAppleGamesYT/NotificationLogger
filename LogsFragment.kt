package com.discordnotificationlogger.ui.logs

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.discordnotificationlogger.R
import com.discordnotificationlogger.database.NotificationEntity
import com.discordnotificationlogger.databinding.FragmentLogsBinding
import com.discordnotificationlogger.viewmodel.NotificationViewModel

class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by activityViewModels()
    private lateinit var adapter: NotificationAdapter

    // Randomized title phrases for the keyword section
    private val keywordSectionTitles = listOf(
        "You might want to look at this.",
        "Hey, this looks important.",
        "These caught our attention.",
        "Worth checking out.",
        "Flagged for your review.",
        "Something here for you.",
        "Looks like this matters.",
        "A heads up for you.",
        "You'll want to see this.",
        "These seem relevant to you.",
        "Heads up — take a look.",
        "These look like they matter.",
        "Might be worth your time.",
        "These stood out for you.",
        "Consider checking these out."
    )

    private var sectionTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pick a random title once per fragment lifecycle
        sectionTitle = keywordSectionTitles.random()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { entity ->
            showDeleteDialog(entity)
        }
        binding.recyclerViewLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LogsFragment.adapter
            setHasFixedSize(false)
        }
    }

    private fun observeNotifications() {
        // Observe both LiveData sources for real-time updates
        var allNotifications = emptyList<NotificationEntity>()
        var keywordNotifications = emptyList<NotificationEntity>()

        viewModel.allNotificationsLive.observe(viewLifecycleOwner) { all ->
            allNotifications = all ?: emptyList()
            rebuildList(keywordNotifications, allNotifications)
        }

        viewModel.keywordNotificationsLive.observe(viewLifecycleOwner) { keyword ->
            keywordNotifications = keyword ?: emptyList()
            rebuildList(keywordNotifications, allNotifications)
        }
    }

    private fun rebuildList(
        keywordNotifs: List<NotificationEntity>,
        allNotifs: List<NotificationEntity>
    ) {
        val items = mutableListOf<LogListItem>()

        // === KEYWORD SECTION ===
        items.add(
            LogListItem.Header(
                title = sectionTitle,
                subtitle = "these are messages with your keyword inside",
                isKeywordSection = true
            )
        )

        if (keywordNotifs.isEmpty()) {
            items.add(LogListItem.EmptyKeyword)
        } else {
            keywordNotifs.take(20).forEach { notif ->
                items.add(LogListItem.NotificationItem(notif))
            }
        }

        // Divider
        items.add(LogListItem.Divider)

        // === ALL NOTIFICATIONS SECTION ===
        val nonKeywordNotifs = allNotifs
        items.add(
            LogListItem.Header(
                title = "All Notifications",
                subtitle = "${nonKeywordNotifs.size} total logged",
                isKeywordSection = false
            )
        )

        if (nonKeywordNotifs.isEmpty()) {
            items.add(LogListItem.EmptyKeyword)
        } else {
            nonKeywordNotifs.forEach { notif ->
                items.add(LogListItem.NotificationItem(notif))
            }
        }

        adapter.submitItems(items)

        // Update empty state
        if (allNotifs.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.recyclerViewLogs.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.recyclerViewLogs.visibility = View.VISIBLE
        }
    }

    private fun showDeleteDialog(entity: NotificationEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Notification")
            .setMessage("Remove this log entry?\n\n${entity.appName}: ${entity.title}")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.delete(entity)
                Toast.makeText(context, "Entry removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
