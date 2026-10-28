package com.sfl.notifapp.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sfl.notifapp.R
import com.sfl.notifapp.data.api.SflApiClient
import com.sfl.notifapp.data.engine.SflTimerEngine
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.model.TimerCategory
import com.sfl.notifapp.data.storage.PreferencesManager
import com.sfl.notifapp.databinding.ActivityMainBinding
import com.sfl.notifapp.receiver.AlarmScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private val apiClient = SflApiClient()
    private val adapter = TimerAdapter()

    private var allTimers: List<FarmTimerItem> = emptyList()
    private var selectedCategory: TimerCategory? = null
    private var countdownJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "Notification permission denied. Alerts may not show.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        checkPermissions()
        setupUI()
        loadCachedData()
        startCountdownTicker()

        // Fetch fresh state immediately if farmId is configured
        if (prefs.farmId.isNotBlank()) {
            syncFarm(showLoading = true)
        } else {
            showFarmIdPromptDialog()
        }
    }

    private fun checkPermissions() {
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Please allow Exact Alarms for 100% timely notifications.",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                } catch (e: Exception) {
                    // Ignore if settings intent not available
                }
            }
        }
    }

    private fun setupUI() {
        if (prefs.farmId.isBlank()) {
            binding.btnFarmId.text = "Set Farm ID"
        } else {
            binding.btnFarmId.text = "Farm #${prefs.farmId}"
        }

        binding.btnFarmId.setOnClickListener {
            showFarmIdPromptDialog()
        }

        binding.btnSettings.setOnClickListener {
            openSettingsSheet()
        }

        binding.recyclerTimers.layoutManager = LinearLayoutManager(this)
        binding.recyclerTimers.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.sunflower_gold)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card)
        binding.swipeRefresh.setOnRefreshListener {
            syncFarm(showLoading = false)
        }

        binding.btnSyncNow.setOnClickListener {
            syncFarm(showLoading = true)
        }

        // Setup filter chips
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chipCrops -> TimerCategory.CROPS
                R.id.chipFruitTrees -> TimerCategory.FRUIT_TREES
                R.id.chipTrees -> TimerCategory.TREES
                R.id.chipMining -> TimerCategory.MINING
                R.id.chipOil -> TimerCategory.OIL
                R.id.chipComposters -> TimerCategory.COMPOSTERS
                R.id.chipCooking -> TimerCategory.COOKING
                R.id.chipAnimals -> TimerCategory.ANIMALS
                R.id.chipGreenhouse -> TimerCategory.GREENHOUSE
                R.id.chipBeehives -> TimerCategory.BEEHIVES
                else -> null // All
            }
            updateFilteredList()
        }
    }

    private fun showFarmIdPromptDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter Farm ID (e.g. 1234)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.farmId)
            setPadding(48, 36, 48, 36)
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setHintTextColor(ContextCompat.getColor(context, R.color.text_muted))
            background = ContextCompat.getDrawable(context, R.drawable.bg_input)
        }

        val container = android.widget.FrameLayout(this).apply {
            setPadding(60, 20, 60, 20)
            addView(input)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("🌻 Sunflower Land Farm")
            .setMessage("Please enter your SFL Farm ID to monitor crops, fruit trees, and timers:")
            .setView(container)
            .setPositiveButton("Save & Sync") { _, _ ->
                val newId = input.text.toString().trim()
                if (newId.isNotEmpty()) {
                    prefs.farmId = newId
                    binding.btnFarmId.text = "Farm #$newId"
                    syncFarm(showLoading = true)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openSettingsSheet() {
        val sheet = SettingsBottomSheet { newFarmId ->
            binding.btnFarmId.text = "Farm #$newFarmId"
            syncFarm(showLoading = true)
        }
        sheet.show(supportFragmentManager, "SettingsBottomSheet")
    }

    private fun loadCachedData() {
        allTimers = prefs.getCachedTimers()
        updateOverviewCards()
        updateFilteredList()

        if (prefs.lastSyncTimestamp > 0L) {
            val dateStr = timeFormat.format(Date(prefs.lastSyncTimestamp))
            binding.tvSyncStatus.text = "Last synced: $dateStr"
        } else {
            binding.tvSyncStatus.text = "Not synced yet"
        }
    }

    private fun syncFarm(showLoading: Boolean) {
        if (prefs.farmId.isBlank()) {
            binding.swipeRefresh.isRefreshing = false
            openSettingsSheet()
            return
        }

        if (showLoading) {
            binding.btnSyncNow.isEnabled = false
            binding.btnSyncNow.text = "Syncing..."
            binding.tvSyncStatus.text = "Fetching farm #${prefs.farmId}..."
        }

        lifecycleScope.launch {
            val result = apiClient.fetchFarmJson(prefs.farmId)

            binding.swipeRefresh.isRefreshing = false
            binding.btnSyncNow.isEnabled = true
            binding.btnSyncNow.text = "Sync Farm"

            result.fold(
                onSuccess = { rootJson ->
                    try {
                        allTimers = SflTimerEngine.extractTimers(rootJson)
                        prefs.saveCachedTimers(allTimers)
                        prefs.lastSyncTimestamp = System.currentTimeMillis()

                        val nowStr = timeFormat.format(Date())
                        binding.tvSyncStatus.text = "Live · Synced at $nowStr"

                        // Schedule exact hardware alarms
                        AlarmScheduler.scheduleTimers(this@MainActivity, allTimers)

                        updateOverviewCards()
                        updateFilteredList()
                    } catch (e: Exception) {
                        binding.tvSyncStatus.text = "Error parsing farm: ${e.localizedMessage}"
                    }
                },
                onFailure = { error ->
                    val cachedCount = allTimers.size
                    if (cachedCount > 0) {
                        binding.tvSyncStatus.text = "Offline · Using cached ($cachedCount timers)"
                    } else {
                        binding.tvSyncStatus.text = "Sync failed: ${error.localizedMessage}"
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Network issue. Showing offline data.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun updateOverviewCards() {
        val now = System.currentTimeMillis()
        val readyCount = allTimers.count { it.readyAt <= now }
        val upcomingCount = allTimers.count { it.readyAt > now }

        binding.tvReadyCount.text = readyCount.toString()
        binding.tvUpcomingCount.text = upcomingCount.toString()
    }

    private fun updateFilteredList() {
        val filtered = if (selectedCategory == null) {
            allTimers
        } else {
            allTimers.filter { it.category == selectedCategory }
        }

        adapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerTimers.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerTimers.visibility = View.VISIBLE
        }
    }

    private fun startCountdownTicker() {
        countdownJob?.cancel()
        countdownJob = lifecycleScope.launch {
            while (isActive) {
                delay(1000L)
                // Refresh list item countdowns and overview count
                if (allTimers.isNotEmpty()) {
                    adapter.notifyDataSetChanged()
                    updateOverviewCards()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownJob?.cancel()
    }
}
