package com.sfl.notifapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sfl.notifapp.R
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.databinding.ItemTimerBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimerAdapter : ListAdapter<FarmTimerItem, TimerAdapter.TimerViewHolder>(TimerDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val hourMinFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerViewHolder {
        val binding = ItemTimerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimerViewHolder(private val binding: ItemTimerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FarmTimerItem) {
            val context = binding.root.context
            binding.tvIconEmoji.text = item.iconEmoji
            binding.tvTitle.text = item.title

            val readyTimeStr = hourMinFormat.format(Date(item.readyAt))
            binding.tvSubtitle.text = "${item.category.displayName} · Ready at $readyTimeStr"

            val remainingMs = item.remainingMs
            if (remainingMs <= 0L) {
                // Ready now
                binding.badgeContainer.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_ready)
                binding.tvBadgeText.setTextColor(ContextCompat.getColor(context, R.color.badge_ready_text))
                binding.tvBadgeText.text = "READY"
            } else {
                // Countdown
                binding.badgeContainer.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_timer)
                binding.tvBadgeText.setTextColor(ContextCompat.getColor(context, R.color.badge_timer_text))
                binding.tvBadgeText.text = formatDuration(remainingMs)
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
    }

    class TimerDiffCallback : DiffUtil.ItemCallback<FarmTimerItem>() {
        override fun areItemsTheSame(oldItem: FarmTimerItem, newItem: FarmTimerItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FarmTimerItem, newItem: FarmTimerItem): Boolean {
            return oldItem == newItem
        }
    }
}
