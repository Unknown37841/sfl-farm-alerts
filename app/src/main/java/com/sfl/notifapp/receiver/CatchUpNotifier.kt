package com.sfl.notifapp.receiver

import android.content.Context
import android.util.Log
import com.sfl.notifapp.data.model.productKey
import com.sfl.notifapp.data.storage.PreferencesManager

/**
 * Safety net for lost notifications.
 *
 * Exact alarms can silently disappear: Android batches setExactAndAllowWhileIdle
 * calls in Doze (first one fires, the rest are deferred ~9 min each), the
 * post-alarm sync cancels and re-registers alarms, and pending alarms do not
 * survive a reboot. Any timer whose readyAt already passed but which never got
 * a notification would otherwise be lost forever, because scheduleTimers skips
 * due timers (readyAt <= now).
 *
 * After every sync, every fired alarm and every boot we walk the cached timers
 * and notify each due-but-unnotified product immediately.
 */
object CatchUpNotifier {
    private const val TAG = "CatchUpNotifier"

    /** Never spam notifications for items that have been ready for many hours. */
    private const val MAX_CATCH_UP_AGE_MS = 6 * 3600_000L

    private const val DEBOUNCE_MS = 5 * 60_000L

    /**
     * Stable debounce key that ignores the "(xN)" ready-count suffix, so a
     * growing ready cluster ("Wood (x3)" -> "Wood (x4)") can't bypass it.
     */
    fun notifKey(farmId: String, categoryKey: String, title: String): String =
        "${farmId}_${categoryKey}_${title.substringBefore(" (x")}"

    /**
     * Shows notifications for every cached timer that is due but never
     * notified. [source] is only used for logging ("sync", "alarm", "boot").
     */
    fun notifyDueTimers(context: Context, source: String) {
        try {
            val prefs = PreferencesManager(context)
            if (!prefs.isNotificationsEnabled) return
            val farmId = prefs.farmId
            if (farmId.isBlank()) return

            val now = System.currentTimeMillis()
            val due = prefs.getCachedTimers().filter {
                it.readyAt <= now &&
                    it.readyAt >= now - MAX_CATCH_UP_AGE_MS &&
                    prefs.isTimerAllowed(it.category, it.title)
            }
            if (due.isEmpty()) return

            for (timer in due) {
                val key = notifKey(farmId, timer.category.key, timer.title)
                val lastNotified = prefs.getLastNotifiedTime(key)
                if (now - lastNotified < DEBOUNCE_MS) continue

                // An active "remind until harvested" chain already owns this
                // product — its repeat alarm keeps reminding, don't duplicate.
                if (prefs.getActiveTreePins()
                        .contains("${farmId}@@${timer.productKey}")
                ) {
                    continue
                }

                // Pinned product: start/continue its reminder chain instead of
                // a plain notification (same hand-off as AlarmReceiver).
                if (TreePinManager.onTimerReady(context, timer, farmId)) {
                    prefs.saveLastNotifiedTime(key, now)
                    continue
                }

                prefs.saveLastNotifiedTime(key, now)
                val notifId = (farmId + timer.category.key + timer.title).hashCode()
                AlarmReceiver().showNotification(
                    context,
                    timer.title,
                    timer.body,
                    farmId,
                    notifId,
                    timer.category.key,
                    timer.iconResId
                )
                Log.i(
                    TAG,
                    "[$source] Catch-up notification for '${timer.title}' " +
                        "(ready ${(now - timer.readyAt) / 60_000L} min ago)"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Catch-up pass failed: ${e.message}")
        }
    }
}
