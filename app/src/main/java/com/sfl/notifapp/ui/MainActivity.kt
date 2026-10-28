package com.sfl.notifapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.sfl.notifapp.R
import com.sfl.notifapp.data.api.SflApiClient
import com.sfl.notifapp.data.engine.SflTimerEngine
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.model.TimerCategory
import com.sfl.notifapp.data.storage.PreferencesManager
import com.sfl.notifapp.databinding.ActivityMainBinding
import com.sfl.notifapp.receiver.AlarmScheduler
import com.sfl.notifapp.util.BackgroundPermissionHelper
import com.sfl.notifapp.worker.FarmSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var timerAdapter: TimerAdapter
    private val apiClient = SflApiClient()

    private var allTimers: List<FarmTimerItem> = emptyList()
    private var selectedCategory: TimerCategory? = null
    private var currentCategories: List<TimerCategory?> = listOf(null)

    // Handler for periodic 1-second countdown ticker
    private val tickerHandler = Handler(Looper.getMainLooper())
    private val tickerRunnable = object : Runnable {
        override fun run() {
            timerAdapter.notifyDataSetChanged()
            updateOverviewCounters()
            tickerHandler.postDelayed(this, 1000L)
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
                FarmSyncWorker.schedulePeriodicSync(this)
            } else {
                Toast.makeText(this, "Notification permission required for alerts", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        setupUI()
        setupRecyclerView()
        setupSwipeGestures()
        checkPermissions()

        // Load cached timers immediately for offline instant start
        loadCachedData()

        // Background worker periodic sync registration
        FarmSyncWorker.schedulePeriodicSync(this)

        // Perform initial network sync
        syncFarm(showLoading = true)
    }

    override fun onResume() {
        super.onResume()
        tickerHandler.post(tickerRunnable)
    }

    override fun onPause() {
        super.onPause()
        tickerHandler.removeCallbacks(tickerRunnable)
    }

    private fun setupUI() {
        val farmId = prefs.farmId
        binding.btnFarmId.text = "Farm #$farmId"

        binding.btnFarmId.setOnClickListener {
            openSettingsSheet()
        }

        binding.btnSettings.setOnClickListener {
            openSettingsSheet()
        }

        binding.btnSyncNow.setOnClickListener {
            syncFarm(showLoading = true)
        }

        binding.swipeRefresh.setColorSchemeResources(R.color.sunflower_gold)
        binding.swipeRefresh.setOnRefreshListener {
            syncFarm(showLoading = false)
        }
    }

    private fun setupRecyclerView() {
        timerAdapter = TimerAdapter()
        binding.recyclerTimers.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = timerAdapter
            setHasFixedSize(true)
        }
    }

    private fun openSettingsSheet() {
        val sheet = SettingsBottomSheet(
            onFarmIdChanged = { newFarmId ->
                binding.btnFarmId.text = "Farm #$newFarmId"
                syncFarm(showLoading = true)
            },
            onSettingsDismissed = {
                updateCategoryChips()
                updateFilteredList()
            }
        )
        sheet.show(supportFragmentManager, "SettingsBottomSheet")
    }

    private fun checkPermissions() {
        // Notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Exact Alarm permission check on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        }

        // Battery optimization exemption check
        if (!BackgroundPermissionHelper.isIgnoringBatteryOptimizations(this)) {
            if (!prefs.hasSeenBatteryPrompt) {
                prefs.hasSeenBatteryPrompt = true
                BackgroundPermissionHelper.showBackgroundGuideDialog(this)
            }
        }
    }

    private fun loadCachedData() {
        val cached = prefs.getCachedTimers()
        if (cached.isNotEmpty()) {
            allTimers = cached.sortedBy { it.readyAt }
            updateCategoryChips()
            updateFilteredList()
            updateOverviewCounters()
            val lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(prefs.lastSyncTime))
            binding.tvSyncStatus.text = "Last synced: $lastSyncTime"
        } else {
            binding.tvSyncStatus.text = "Tap Sync to fetch farm data"
        }
    }

    private fun syncFarm(showLoading: Boolean) {
        val farmId = prefs.farmId
        if (showLoading) {
            binding.swipeRefresh.isRefreshing = true
        }
        binding.tvSyncStatus.text = "Syncing Farm #$farmId..."

        lifecycleScope.launch {
            try {
                val fetchResult = apiClient.fetchFarmJson(farmId)

                if (fetchResult.isSuccess) {
                    val farmData = fetchResult.getOrThrow()
                    val timers = withContext(Dispatchers.Default) {
                        SflTimerEngine.extractTimers(farmData)
                    }

                    allTimers = timers.sortedBy { it.readyAt }
                    prefs.saveCachedTimers(allTimers)
                    prefs.lastSyncTime = System.currentTimeMillis()

                    // Reschedule hardware RTC alarms for enabled categories
                    if (prefs.isNotificationsEnabled) {
                        AlarmScheduler.scheduleTimers(this@MainActivity, allTimers)
                    }

                    updateCategoryChips()
                    updateFilteredList()
                    updateOverviewCounters()

                    val syncTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    binding.tvSyncStatus.text = "Synced: $syncTime (${allTimers.size} timers)"
                } else {
                    val errorMsg = fetchResult.exceptionOrNull()?.message ?: "Unknown error"
                    binding.tvSyncStatus.text = "Sync failed: $errorMsg"
                    Toast.makeText(this@MainActivity, "Farm API: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.tvSyncStatus.text = "Connection error: ${e.localizedMessage ?: "timeout"}"
                Toast.makeText(this@MainActivity, "Sync error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateCategoryChips() {
        val distinctCategories = allTimers.map { it.category }.distinctBy { it.key }.sortedBy { it.displayName }
        currentCategories = listOf(null) + distinctCategories

        binding.chipGroupFilters.removeAllViews()

        // "All" Chip
        val chipAll = Chip(this).apply {
            id = View.generateViewId()
            text = "All (${allTimers.size})"
            isCheckable = true
            isChecked = (selectedCategory == null)
            setOnClickListener {
                selectCategory(null, animated = true)
            }
        }
        binding.chipGroupFilters.addView(chipAll)

        // Dynamic chips for categories found on farm
        for (category in distinctCategories) {
            val count = allTimers.count { it.category == category }
            val chip = Chip(this).apply {
                id = View.generateViewId()
                text = "${category.emoji} ${category.displayName} ($count)"
                isCheckable = true
                isChecked = (selectedCategory == category)
                setOnClickListener {
                    selectCategory(category, animated = true)
                }
            }
            binding.chipGroupFilters.addView(chip)
        }
    }

    private fun navigateToNextCategory() {
        val currentIndex = currentCategories.indexOf(selectedCategory).coerceAtLeast(0)
        if (currentIndex < currentCategories.size - 1) {
            selectCategory(currentCategories[currentIndex + 1], animated = true, slideLeft = true)
        }
    }

    private fun navigateToPrevCategory() {
        val currentIndex = currentCategories.indexOf(selectedCategory).coerceAtLeast(0)
        if (currentIndex > 0) {
            selectCategory(currentCategories[currentIndex - 1], animated = true, slideLeft = false)
        }
    }

    private fun selectCategory(cat: TimerCategory?, animated: Boolean = true, slideLeft: Boolean = true) {
        if (selectedCategory == cat && animated) return
        val oldIndex = currentCategories.indexOf(selectedCategory).coerceAtLeast(0)
        selectedCategory = cat
        val targetIndex = currentCategories.indexOf(cat).coerceAtLeast(0)

        // 1. Update Chip UI & smooth scroll into view
        updateChipSelection(targetIndex)

        // 2. Animate list transition if container is ready
        if (animated && binding.recyclerTimers.width > 0) {
            val direction = if (targetIndex != oldIndex) targetIndex > oldIndex else slideLeft
            val slideOutX = if (direction) -binding.recyclerTimers.width.toFloat() * 0.35f else binding.recyclerTimers.width.toFloat() * 0.35f
            val slideInX = -slideOutX

            binding.recyclerTimers.animate()
                .translationX(slideOutX)
                .alpha(0.1f)
                .setDuration(110)
                .withEndAction {
                    updateFilteredList()
                    binding.recyclerTimers.translationX = slideInX
                    binding.recyclerTimers.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(130)
                        .start()
                }
                .start()
        } else {
            updateFilteredList()
        }
    }

    private fun updateChipSelection(targetIndex: Int) {
        val group = binding.chipGroupFilters
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip ?: continue
            val isTarget = (i == targetIndex)
            if (chip.isChecked != isTarget) {
                chip.isChecked = isTarget
            }
            if (isTarget) {
                binding.scrollFilters.post {
                    val scrollX = chip.left - (binding.scrollFilters.width / 2) + (chip.width / 2)
                    binding.scrollFilters.smoothScrollTo(scrollX.coerceAtLeast(0), 0)
                }
            }
        }
    }

    private fun setupSwipeGestures() {
        val density = resources.displayMetrics.density
        val swipeThreshold = (50 * density).toInt()
        val swipeVelocityThreshold = (100 * density).toInt()

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY) * 1.3f &&
                    kotlin.math.abs(diffX) > swipeThreshold &&
                    kotlin.math.abs(velocityX) > swipeVelocityThreshold
                ) {
                    if (diffX < 0) {
                        navigateToNextCategory()
                    } else {
                        navigateToPrevCategory()
                    }
                    return true
                }
                return false
            }
        })

        var startX = 0f
        var startY = 0f
        var isHorizontalSwipe = false

        fun handleTouch(event: MotionEvent): Boolean {
            gestureDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isHorizontalSwipe = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - startX
                    val dy = event.y - startY
                    if (!isHorizontalSwipe && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.5f && kotlin.math.abs(dx) > swipeThreshold) {
                        isHorizontalSwipe = true
                        binding.swipeRefresh.isEnabled = false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.swipeRefresh.isEnabled = true
                    if (isHorizontalSwipe) {
                        val dx = event.x - startX
                        if (dx < -swipeThreshold) {
                            navigateToNextCategory()
                        } else if (dx > swipeThreshold) {
                            navigateToPrevCategory()
                        }
                        isHorizontalSwipe = false
                        return true
                    }
                }
            }
            return false
        }

        binding.recyclerTimers.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                handleTouch(e)
                return isHorizontalSwipe
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                handleTouch(e)
            }
        })

        binding.emptyView.setOnTouchListener { _, event ->
            handleTouch(event)
            true
        }
    }

    private fun updateFilteredList() {
        val filtered = if (selectedCategory == null) {
            allTimers
        } else {
            allTimers.filter { it.category == selectedCategory }
        }

        timerAdapter.submitList(filtered)
        val isEmpty = filtered.isEmpty()
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerTimers.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateOverviewCounters() {
        val now = System.currentTimeMillis()
        var readyCount = 0
        var upcomingCount = 0

        for (timer in allTimers) {
            if (timer.readyAt <= now) {
                readyCount++
            } else {
                upcomingCount++
            }
        }

        binding.tvReadyCount.text = readyCount.toString()
        binding.tvUpcomingCount.text = upcomingCount.toString()
    }
}
