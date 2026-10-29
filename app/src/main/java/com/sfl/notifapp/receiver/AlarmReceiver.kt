package com.sfl.notifapp.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sfl.notifapp.R
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.model.TimerCategory
import com.sfl.notifapp.data.model.resolveItemIcon
import com.sfl.notifapp.data.storage.PreferencesManager
import com.sfl.notifapp.ui.MainActivity
import com.sfl.notifapp.worker.FarmSyncWorker
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val CHANNEL_ID = "sfl_farm_alerts_channel"
        const val CHANNEL_NAME = "Sunflower Land Farm Alerts"
        const val EXTRA_TIMER_ID = "extra_timer_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_FARM_ID = "extra_farm_id"
        const val EXTRA_ICON_RES_ID = "extra_icon_res_id"
        const val EXTRA_READY_AT = "extra_ready_at"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "sfl:AlarmReceiverWakeLock"
        )
        wakeLock?.acquire(10_000L) // Hold wake lock for max 10 seconds

        try {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "Sunflower Land"
            val body = intent.getStringExtra(EXTRA_BODY) ?: "Items are ready to collect!"
            val categoryKey = intent.getStringExtra(EXTRA_CATEGORY) ?: ""
            val iconResId = intent.getIntExtra(EXTRA_ICON_RES_ID, 0)
            val farmId = intent.getStringExtra(EXTRA_FARM_ID) ?: ""
            val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: System.currentTimeMillis().toString()
            val readyAt = intent.getLongExtra(EXTRA_READY_AT, 0L)
            val now = System.currentTimeMillis()

            val prefs = PreferencesManager(context)

            // 1. If readyAt was passed and is still far in the future (>60s), ignore stale/premature alarm
            if (readyAt > now + 60_000L) {
                Log.w(TAG, "Ignoring premature alarm for '$title': readyAt is in the future ($readyAt > now $now)")
                return
            }

            // 2. Cross-check against cached timers to protect against ghost alarms from older versions
            val cachedTimers = prefs.getCachedTimers()
            val matchingTimer = cachedTimers.firstOrNull { it.id == timerId || it.title == title }
            if (matchingTimer != null && matchingTimer.readyAt > now + 60_000L) {
                Log.w(TAG, "Ignoring stale ghost alarm for '$title': cached timer not ready yet (${matchingTimer.readyAt} > now $now)")
                return
            }

            // 3. Debounce: if this item was already notified within the last 5 minutes, skip
            val notifKey = "${farmId}_${categoryKey}_${title}"
            val lastNotified = prefs.getLastNotifiedTime(notifKey)
            if (now - lastNotified < 5 * 60_000L) {
                Log.i(TAG, "Debouncing repeated notification for $notifKey")
                return
            }
            prefs.saveLastNotifiedTime(notifKey, now)

            // 4. Stable notification ID per item so it updates in-place rather than creating duplicate spam
            val notifId = (farmId + categoryKey + title).hashCode()
            showNotification(context, title, body, farmId, notifId, categoryKey, iconResId)

            // Auto-schedule background farm sync after 5 minutes so when the user harvests
            // and replants, the app fetches new farm timers automatically without opening the app!
            schedulePostAlarmSync(context)
        } finally {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
        }
    }

    private fun schedulePostAlarmSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val postSyncWork = OneTimeWorkRequestBuilder<FarmSyncWorker>()
                .setInitialDelay(5, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sfl_post_alarm_sync",
                ExistingWorkPolicy.REPLACE,
                postSyncWork
            )
            Log.i(TAG, "Enqueued post-alarm auto-sync (5 min delay).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue post-alarm sync: ${e.message}")
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        body: String,
        farmId: String,
        notificationId: Int,
        categoryKey: String = "",
        explicitIconResId: Int = 0
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel with Sound & Vibration for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications for ready farm items"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap opens MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_FARM_ID", farmId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullTitle = if (farmId.isNotBlank()) "🌻 Farm #$farmId · $title" else "🌻 $title"

        // Resolve specific icon (Wood -> Tree, Crops, Mining, etc.)
        val targetIconRes: Int = if (explicitIconResId != 0) {
            explicitIconResId
        } else {
            val category = TimerCategory.fromKey(categoryKey)
            resolveItemIcon(title, category) ?: category.iconResId
        }

        val largeIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, targetIconRes)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sunflower)
            .setContentTitle(fullTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        val notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }
}
