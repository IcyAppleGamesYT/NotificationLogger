package com.discordnotificationlogger.ui.home

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.discordnotificationlogger.R
import com.discordnotificationlogger.databinding.FragmentHomeBinding
import com.discordnotificationlogger.service.NotificationLoggerService
import com.discordnotificationlogger.viewmodel.NotificationViewModel
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by activityViewModels()
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                prefs.edit().putString("folder_uri", uri.toString()).apply()
                updateFolderStatus()
                Toast.makeText(context, "📁 Folder selected!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireContext().getSharedPreferences("nl_prefs", Context.MODE_PRIVATE)

        setupPermissionButton()
        setupKeywords()
        setupFolderSettings()
        setupStats()
        updatePermissionStatus()
        updateFolderStatus()
        loadKeywords()
    }

    private fun setupPermissionButton() {
        binding.btnGrantPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
    }

    private fun setupKeywords() {
        binding.btnAddKeyword.setOnClickListener {
            val input = binding.etKeyword.text.toString().trim()
            if (input.isNotEmpty()) {
                addKeyword(input)
                binding.etKeyword.text?.clear()
            } else {
                Toast.makeText(context, "Enter a keyword first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFolderSettings() {
        binding.etFolderName.setText(prefs.getString("folder_name", "NotificationLogs"))

        binding.btnSaveFolderName.setOnClickListener {
            val name = binding.etFolderName.text.toString().trim()
            if (name.isNotEmpty()) {
                prefs.edit().putString("folder_name", name).apply()
                Toast.makeText(context, "✅ Folder name saved: $name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Enter a folder name", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSelectFolder.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
            folderPickerLauncher.launch(intent)
        }

        binding.btnClearLogs.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear All Logs")
                .setMessage("Are you sure you want to delete all logged notifications from the database? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteAll()
                    Toast.makeText(context, "🗑 All logs cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupStats() {
        viewModel.notificationCount.observe(viewLifecycleOwner) { count ->
            binding.tvTotalCount.text = count.toString()
        }
        viewModel.keywordMatchCount.observe(viewLifecycleOwner) { count ->
            binding.tvKeywordCount.text = count.toString()
        }
    }

    private fun updatePermissionStatus() {
        val enabled = isNotificationListenerEnabled()
        if (enabled) {
            binding.tvPermissionStatus.text = "✅ Notification Access Granted"
            binding.tvPermissionStatus.setTextColor(resources.getColor(R.color.discord_green, null))
            binding.btnGrantPermission.text = "Manage Notification Access"
        } else {
            binding.tvPermissionStatus.text = "⚠️ Notification Access Required"
            binding.tvPermissionStatus.setTextColor(resources.getColor(R.color.discord_yellow, null))
            binding.btnGrantPermission.text = "Grant Notification Access"
        }
    }

    private fun updateFolderStatus() {
        val uri = prefs.getString("folder_uri", null)
        val name = prefs.getString("folder_name", "NotificationLogs")
        if (uri != null) {
            binding.tvFolderStatus.text = "📁 Saving to: /$name/"
            binding.tvFolderStatus.setTextColor(resources.getColor(R.color.discord_green, null))
        } else {
            binding.tvFolderStatus.text = "⚠️ No folder selected yet"
            binding.tvFolderStatus.setTextColor(resources.getColor(R.color.discord_text_muted, null))
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val pkgName = requireContext().packageName
        val flat = Settings.Secure.getString(
            requireContext().contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(pkgName)
    }

    private fun addKeyword(keyword: String) {
        val keywords = getKeywords().toMutableList()
        val lower = keyword.lowercase()
        if (keywords.contains(lower)) {
            Toast.makeText(context, "Keyword already exists", Toast.LENGTH_SHORT).show()
            return
        }
        keywords.add(lower)
        saveKeywords(keywords)
        addChipToGroup(keyword)
    }

    private fun addChipToGroup(keyword: String) {
        val chip = Chip(requireContext()).apply {
            text = keyword
            isCloseIconVisible = true
            setChipBackgroundColorResource(R.color.discord_surface)
            setTextColor(resources.getColor(R.color.discord_text_primary, null))
            setCloseIconTintResource(R.color.discord_text_muted)
            setOnCloseIconClickListener {
                removeKeyword(keyword)
                binding.chipGroupKeywords.removeView(this)
            }
        }
        binding.chipGroupKeywords.addView(chip)
    }

    private fun loadKeywords() {
        binding.chipGroupKeywords.removeAllViews()
        getKeywords().forEach { addChipToGroup(it) }
    }

    private fun removeKeyword(keyword: String) {
        val keywords = getKeywords().toMutableList()
        keywords.remove(keyword.lowercase())
        saveKeywords(keywords)
        Toast.makeText(context, "Removed: $keyword", Toast.LENGTH_SHORT).show()
    }

    private fun getKeywords(): List<String> {
        val json = prefs.getString("keywords", "[]") ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    private fun saveKeywords(keywords: List<String>) {
        prefs.edit().putString("keywords", gson.toJson(keywords)).apply()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateFolderStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
