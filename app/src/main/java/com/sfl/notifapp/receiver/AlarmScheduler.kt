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
    const val ACTION_PERIODIC_SYNC = "com.sfl.notifapp.ACTION_PERIODIC_SYNC"
    const val ACTION_TREE_PIN = "com.sfl.notifapp.ACTION_TREE_PIN"
    const val ACTION_TREE_PIN_REPEAT = "com.sfl.notifapp.ACTION_TREE_PIN_REPEAT"
    const val ACTION_TREE_PIN_STOP = "com.sfl.notifapp.ACTION_TREE_PIN_STOP"
    const val ACTION_TREE_NEXT = "com.sfl.notifapp.ACTION_TREE_NEXT"

    fun scheduleTimers(context: Context, timers: List<FarmTimerItem>) {
        val prefs = PreferencesManager(context)
        if (!prefs.isNotificationsEnabled) {
            cancelAllAlarms(context)
            return
        }

        // Cancel previously recorded scheduled alarms first so old/stale alarms don't remain
        cancelAllAlarms(context)

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
                putExtra(AlarmReceiver.EXTRA_READY_AT, timer.readyAt)
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

        // Ensure a repeating sync alarm is scheduled (every 15 min) to keep farm data fresh in background
        scheduleRepeatingSyncAlarm(context, 15)
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
                pendingIntent.cancel()
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

    fun scheduleRepeatingSyncAlarm(context: Context, delayMinutes: Long = 15) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = System.currentTimeMillis() + (delayMinutes * 60_000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PERIODIC_SYNC
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            888888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    /**
     * One repeating alarm per product. Request code uses farmId + productKey,
     * so enabling reminders for two products never cancels the other's alarm.
     */
    fun scheduleTreePinRepeat(
        context: Context,
        farmId: String,
        productKey: String,
        title: String,
        body: String,
        categoryKey: String,
        iconResId: Int,
        intervalMinutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = System.currentTimeMillis() + intervalMinutes.coerceIn(1, 1440) * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TREE_PIN_REPEAT
            putExtra(AlarmReceiver.EXTRA_FARM_ID, farmId)
            putExtra(AlarmReceiver.EXTRA_PRODUCT_KEY, productKey)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_BODY, body)
            putExtra(AlarmReceiver.EXTRA_CATEGORY, categoryKey)
            putExtra(AlarmReceiver.EXTRA_ICON_RES_ID, iconResId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TreePinManager.pinRequestCode(farmId, productKey),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            Log.d(TAG, "Harvest repeat scheduled: $productKey in $intervalMinutes min")
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm permission missing for harvest repeat: ${e.message}")
        }
    }

    fun cancelTreePinAlarm(context: Context, farmId: String, productKey: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TREE_PIN_REPEAT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TreePinManager.pinRequestCode(farmId, productKey),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
