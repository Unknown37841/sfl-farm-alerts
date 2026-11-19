package com.sfl.notifapp.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.model.productKey
import com.sfl.notifapp.data.storage.PreferencesManager
import com.sfl.notifapp.databinding.BottomSheetPinSettingsBinding
import com.sfl.notifapp.receiver.TreePinManager

/**
 * Per-product "remind until harvested" settings.
 * Master switch + interval (minutes) + max notification count.
 */
class PinSettingsBottomSheet(
    private val item: FarmTimerItem
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPinSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPinSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = PreferencesManager(requireContext())
        val farmId = prefs.farmId
        val key = item.productKey

        binding.ivPinIcon.setImageResource(item.iconResId)
        binding.tvPinTitle.text = item.title.substringBefore(" (x")
        binding.btnPinClose.setOnClickListener { dismiss() }
        binding.btnPinCancel.setOnClickListener { dismiss() }

        // Load current values
        binding.swPinMaster.isChecked = prefs.isTreePinEnabled(farmId, key)
        binding.etPinInterval.setText(prefs.getTreePinInterval(farmId, key).toString())
        binding.etPinMaxCount.setText(prefs.getTreePinMaxCount(farmId, key).toString())
        updateSettingsVisibility()
        updatePreview()

        binding.swPinMaster.setOnCheckedChangeListener { _, _ ->
            updateSettingsVisibility()
            updatePreview()
        }
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePreview()
            }
        }
        binding.etPinInterval.addTextChangedListener(textWatcher)
        binding.etPinMaxCount.addTextChangedListener(textWatcher)

        binding.btnPinSave.setOnClickListener {
            val interval = binding.etPinInterval.text.toString().toIntOrNull() ?: 10
            val maxCount = binding.etPinMaxCount.text.toString().toIntOrNull() ?: 3
            val clampedInterval = interval.coerceIn(1, 1440)
            val clampedMax = maxCount.coerceIn(1, 20)
            val enabled = binding.swPinMaster.isChecked

            prefs.setTreePinInterval(farmId, key, clampedInterval)
            prefs.setTreePinMaxCount(farmId, key, clampedMax)
            prefs.setTreePinEnabled(farmId, key, enabled)

            if (enabled) {
                if (item.remainingMs <= 0L) {
                    TreePinManager.onTimerReady(requireContext(), item, farmId)
                }
            } else {
                TreePinManager.stop(requireContext(), farmId, key)
            }

            Toast.makeText(
                requireContext(),
                if (enabled) "یادآوری تا برداشت فعال شد ✓" else "یادآوری غیرفعال شد",
                Toast.LENGTH_SHORT
            ).show()
            (activity as? OnPinSettingsChanged)?.onPinSettingsChanged()
            dismiss()
        }
    }

    private fun updateSettingsVisibility() {
        binding.pinSettingsContainer.visibility =
            if (binding.swPinMaster.isChecked) View.VISIBLE else View.GONE
    }

    private fun updatePreview() {
        if (!binding.swPinMaster.isChecked) {
            binding.tvPinPreview.text = ""
            return
        }
        val interval = binding.etPinInterval.text.toString().toIntOrNull()?.coerceIn(1, 1440) ?: 10
        val max = binding.etPinMaxCount.text.toString().toIntOrNull()?.coerceIn(1, 20) ?: 3
        val totalMin = interval * (max - 1)
        binding.tvPinPreview.text = when {
            max == 1 -> "فقط یک نوتیفیکیشن هنگام آماده شدن"
            else -> "مجموع $max نوتیف در ~${formatTotal(totalMin)}"
        }
    }

    private fun formatTotal(minutes: Int): String = when {
        minutes >= 60 -> "${minutes / 60} ساعت و ${minutes % 60} دقیقه"
        else -> "$minutes دقیقه"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface OnPinSettingsChanged {
        fun onPinSettingsChanged()
    }
}
