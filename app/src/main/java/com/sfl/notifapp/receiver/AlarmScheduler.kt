package com.sfl.notifapp.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sfl.notifapp.R
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.storage.PreferencesManager

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    const val ACTION_TRIGGER_TIMER = "com.sfl.notifapp.ACTION_TRIGGER_TIMER"

    fun scheduleTimers(context: Context, timers: List<FarmTimerItem>) {
        val prefs = PreferencesManager(context)
        if (!prefs.isNotificationsEnabled) {
            cancelAllAlarms(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        val scheduledIds = mutableSetOf<String>()

        // Check if Android 12+ has exact alarm permission
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        for (timer in timers) {
            // Only schedule if category is enabled in settings and readyAt is in the future
            if (!prefs.isCategoryEnabled(timer.category)) continue
            if (timer.readyAt <= now) continue

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_TIMER
                putExtra(AlarmReceiver.EXTRA_TIMER_ID, timer.id)
                putExtra(AlarmReceiver.EXTRA_TITLE, timer.title)
                putExtra(AlarmReceiver.EXTRA_BODY, timer.body)
                putExtra(AlarmReceiver.EXTRA_CATEGORY, timer.category.key)
                putExtra(AlarmReceiver.EXTRA_ICON_RES_ID, timer.iconResId)
                putExtra(AlarmReceiver.EXTRA_FARM_ID, prefs.farmId)
            }

            val requestCode = timer.id.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (canScheduleExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timer.readyAt,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timer.readyAt,
                        pendingIntent
                    )
                }
                scheduledIds.add(timer.id)
            } catch (e: SecurityException) {
                Log.e(TAG, "Exact alarm permission missing: ${e.message}")
            }
        }

        prefs.saveScheduledAlarmIds(scheduledIds)
        Log.i(TAG, "Scheduled ${scheduledIds.size} exact offline alarms.")
    }

    fun cancelAllAlarms(context: Context) {
        val prefs = PreferencesManager(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val scheduledIds = prefs.getScheduledAlarmIds()

        for (id in scheduledIds) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_TIMER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }

        prefs.saveScheduledAlarmIds(emptySet())
    }

    fun scheduleTestAlarm(context: Context, delaySeconds: Long = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_TIMER
            putExtra(AlarmReceiver.EXTRA_TIMER_ID, "test_timer_${System.currentTimeMillis()}")
            putExtra(AlarmReceiver.EXTRA_TITLE, "Test Farm Notification")
            putExtra(AlarmReceiver.EXTRA_BODY, "Notifications are working 100% properly! Offline alarms active.")
            putExtra(AlarmReceiver.EXTRA_CATEGORY, "crops")
            putExtra(AlarmReceiver.EXTRA_ICON_RES_ID, R.drawable.ic_crop)
            putExtra(AlarmReceiver.EXTRA_FARM_ID, PreferencesManager(context).farmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }
}
