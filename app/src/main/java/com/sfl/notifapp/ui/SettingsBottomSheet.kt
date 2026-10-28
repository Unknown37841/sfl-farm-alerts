package com.sfl.notifapp.ui

import android.content.Context
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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sfl.notifapp.data.model.TimerCategory
import com.sfl.notifapp.data.storage.PreferencesManager
import com.sfl.notifapp.databinding.BottomSheetSettingsBinding
import com.sfl.notifapp.receiver.AlarmScheduler

class SettingsBottomSheet(
    private val onFarmIdChanged: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: PreferencesManager

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

        // Setup Farm ID
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

        // Master Switch
        binding.switchMasterNotifs.isChecked = prefs.isNotificationsEnabled
        binding.switchMasterNotifs.setOnCheckedChangeListener { _, isChecked ->
            prefs.isNotificationsEnabled = isChecked
            if (!isChecked) {
                AlarmScheduler.cancelAllAlarms(context)
                Toast.makeText(context, "All alarms disabled", Toast.LENGTH_SHORT).show()
            } else {
                val cached = prefs.getCachedTimers()
                AlarmScheduler.scheduleTimers(context, cached)
                Toast.makeText(context, "Alarms active", Toast.LENGTH_SHORT).show()
            }
        }

        // Category Switches
        setupCategorySwitch(binding.switchCrops, TimerCategory.CROPS)
        setupCategorySwitch(binding.switchFruitTrees, TimerCategory.FRUIT_TREES)
        setupCategorySwitch(binding.switchTrees, TimerCategory.TREES)
        setupCategorySwitch(binding.switchMining, TimerCategory.MINING)
        setupCategorySwitch(binding.switchOil, TimerCategory.OIL)
        setupCategorySwitch(binding.switchComposters, TimerCategory.COMPOSTERS)
        setupCategorySwitch(binding.switchCooking, TimerCategory.COOKING)
        setupCategorySwitch(binding.switchAnimals, TimerCategory.ANIMALS)
        setupCategorySwitch(binding.switchGreenhouse, TimerCategory.GREENHOUSE)
        setupCategorySwitch(binding.switchBeehives, TimerCategory.BEEHIVES)

        // Test Notification Button
        binding.btnTestNotification.setOnClickListener {
            AlarmScheduler.scheduleTestAlarm(context, delaySeconds = 5)
            Toast.makeText(
                context,
                "Test alarm scheduled! Lock phone or wait 5s...",
                Toast.LENGTH_LONG
            ).show()
        }

        // Battery Optimization Whitelist Button
        binding.btnBatteryOptimization.setOnClickListener {
            requestIgnoreBatteryOptimization(context)
        }
    }

    private fun setupCategorySwitch(
        switchView: com.google.android.material.materialswitch.MaterialSwitch,
        category: TimerCategory
    ) {
        switchView.isChecked = prefs.isCategoryEnabled(category)
        switchView.setOnCheckedChangeListener { _, isChecked ->
            prefs.setCategoryEnabled(category, isChecked)
            val cached = prefs.getCachedTimers()
            AlarmScheduler.scheduleTimers(requireContext(), cached)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
