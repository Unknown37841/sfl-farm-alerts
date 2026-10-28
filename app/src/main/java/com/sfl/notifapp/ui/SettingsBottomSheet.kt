package com.sfl.notifapp.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sfl.notifapp.data.model.SFL_OVERVIEW_CATEGORIES
import com.sfl.notifapp.data.storage.PreferencesManager
import com.sfl.notifapp.databinding.BottomSheetSettingsBinding
import com.sfl.notifapp.receiver.AlarmScheduler

class SettingsBottomSheet(
    private val onFarmIdChanged: (String) -> Unit,
    private val onSettingsDismissed: () -> Unit = {}
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: PreferencesManager
    private lateinit var categoryAdapter: CategorySettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        prefs = PreferencesManager(context)

        // 1. Setup Farm ID
        binding.etFarmId.setText(prefs.farmId)
        binding.btnSaveFarmId.setOnClickListener {
            val newId = binding.etFarmId.text.toString().trim()
            if (newId.isNotEmpty()) {
                prefs.farmId = newId
                Toast.makeText(context, "Farm ID updated to #$newId", Toast.LENGTH_SHORT).show()
                onFarmIdChanged(newId)
                dismiss()
            }
        }

        // 2. Master Switch
        binding.switchMasterNotifs.isChecked = prefs.isNotificationsEnabled
        binding.switchMasterNotifs.setOnCheckedChangeListener { _, isChecked ->
            prefs.isNotificationsEnabled = isChecked
            if (!isChecked) {
                AlarmScheduler.cancelAllAlarms(context)
                Toast.makeText(context, "All alarms disabled", Toast.LENGTH_SHORT).show()
            } else {
                rescheduleAlarms()
                Toast.makeText(context, "Alarms active", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Setup 29 Categories RecyclerView
        categoryAdapter = CategorySettingsAdapter(prefs) { _, _ ->
            updateCountText()
            rescheduleAlarms()
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }
        updateCountText()

        // 4. Select All / Deselect All
        binding.btnSelectAllCategories.setOnClickListener {
            prefs.setAllCategoriesEnabled(true)
            categoryAdapter.notifyDataSetChanged()
            updateCountText()
            rescheduleAlarms()
            Toast.makeText(context, "All 29 categories enabled", Toast.LENGTH_SHORT).show()
        }

        binding.btnDeselectAllCategories.setOnClickListener {
            prefs.setAllCategoriesEnabled(false)
            categoryAdapter.notifyDataSetChanged()
            updateCountText()
            rescheduleAlarms()
            Toast.makeText(context, "All categories disabled", Toast.LENGTH_SHORT).show()
        }

        // 5. Battery Optimization Whitelist Button
        binding.btnBatteryOptimization.setOnClickListener {
            requestIgnoreBatteryOptimization(context)
        }

        // 6. Test Notification Button
        binding.btnTestNotification.setOnClickListener {
            AlarmScheduler.scheduleTestAlarm(context, delaySeconds = 5)
            Toast.makeText(
                context,
                "Test alarm scheduled! Lock phone or wait 5s...",
                Toast.LENGTH_LONG
            ).show()
        }

        // 7. Close / Apply Button
        binding.btnCloseSettings.setOnClickListener {
            dismiss()
        }
    }

    private fun updateCountText() {
        val enabled = prefs.getEnabledCategoriesCount()
        val total = SFL_OVERVIEW_CATEGORIES.size
        binding.tvCategoriesCount.text = "$enabled/$total Active"
    }

    private fun rescheduleAlarms() {
        val context = context ?: return
        if (prefs.isNotificationsEnabled) {
            val cached = prefs.getCachedTimers()
            AlarmScheduler.scheduleTimers(context, cached)
        }
    }

    private fun requestIgnoreBatteryOptimization(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val packageName = context.packageName

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(context, "Battery saving is already unrestricted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onSettingsDismissed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
