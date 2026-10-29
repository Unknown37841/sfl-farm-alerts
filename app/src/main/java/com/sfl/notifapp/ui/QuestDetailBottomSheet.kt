package com.sfl.notifapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sfl.notifapp.R
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.repository.QuestRepository
import com.sfl.notifapp.databinding.BottomSheetQuestDetailBinding
import java.util.Locale

class QuestDetailBottomSheet(
    private val item: FarmTimerItem
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetQuestDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetQuestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        val quest = QuestRepository.getQuest(requireContext(), item.title)

        val displayTitle = quest?.title ?: item.title
        binding.tvQuestTitle.text = displayTitle
        binding.tvQuestSlug.text = "Telegram Daily Quest · ${quest?.id ?: item.title}"

        // Status & Timer
        val remainingMs = item.remainingMs
        if (remainingMs <= 0L) {
            binding.tvQuestStatusBadge.text = "آماده انجام"
            binding.tvQuestStatusBadge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_ready)
            binding.tvQuestStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_ready_text))
            binding.tvQuestTimerText.text = "هم‌اکنون در ربات تلگرام سانفلاور لند فعال است"
        } else {
            binding.tvQuestStatusBadge.text = "در حال انتظار"
            binding.tvQuestStatusBadge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_timer)
            binding.tvQuestStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_timer_text))
            binding.tvQuestTimerText.text = "زمان تا کوئیز بعدی: ${formatDuration(remainingMs)}"
        }

        // Details
        if (quest != null) {
            binding.tvQuestDescription.text = quest.description
            binding.tvQuestChoices.text = quest.choices
            binding.tvQuestOutcomes.text = quest.outcomes
        } else {
            binding.tvQuestDescription.text = "اطلاعات متنی این کوئست در دیتابیس فعلی یافت نشد.\nعنوان کوئست در بازی: ${item.title}"
            binding.tvQuestChoices.text = "برای ثبت این کوئیز در نسخه‌های بعدی، می‌توانید اطلاعات آن را به اشتراک بگذارید."
            binding.tvQuestOutcomes.text = "جوایز و پاسخ‌های ثبت شده در دسترس نیست."
        }

        // Copy button
        binding.btnCopyQuest.setOnClickListener {
            val clipText = buildString {
                appendLine("📜 Sunflower Land Telegram Quest: $displayTitle")
                if (quest != null) {
                    appendLine()
                    appendLine("❓ Question:")
                    appendLine(quest.description)
                    appendLine()
                    appendLine("🔘 Choices:")
                    appendLine(quest.choices)
                    appendLine()
                    appendLine("🏆 Outcomes & Rewards:")
                    appendLine(quest.outcomes)
                }
            }
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("SFL Quest", clipText.trim())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "اطلاعات و پاسخ کوئست کپی شد 📋", Toast.LENGTH_SHORT).show()
        }

        // Telegram Bot button
        binding.btnOpenTelegram.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sunflower_land_bot"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "عدم امکان باز کردن تلگرام", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
