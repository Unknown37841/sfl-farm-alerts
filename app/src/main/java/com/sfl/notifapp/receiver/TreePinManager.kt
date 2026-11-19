package com.sfl.notifapp.receiver

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.model.productKey
import com.sfl.notifapp.data.storage.PreferencesManager
import com.sfl.notifapp.data.storage.TreePinInfo

/**
 * Per-product "keep reminding me until I harvest it" engine.
 *
 * Every product holds its own enabled flag, interval and maximum reminder
 * count. When a ready alarm fires for a pinned product we immediately show
 * reminder 1/N and arm one repeating alarm. Each repeat shows the next
 * reminder until N is reached or the sync detects the harvest (whichever
 * happens first).
 */
object TreePinManager {
    private const val TAG = "TreePinManager"
    const val EXTRA_PRODUCT_KEY = "extra_product_key"

    fun pinRequestCode(farmId: String, productKey: String): Int =
        ("sfl_pin_repeat|$farmId|$productKey").hashCode()

    private fun notificationId(farmId: String, productKey: String): Int =
        ("sfl_pin_notif|$farmId|$productKey").hashCode()

    /** Called from AlarmReceiver when a normal timer became ready. */
    fun onTimerReady(context: Context, timer: FarmTimerItem, farmId: String): Boolean {
        val prefs = PreferencesManager(context)
        val key = timer.productKey
        if (!prefs.isTreePinEnabled(farmId, key)) return false
        if (prefs.getActiveTreePins().contains("${farmId}@@$key")) {
            Log.d(TAG, "Reminder chain already active for $key")
            return false
        }

        val max = prefs.getTreePinMaxCount(farmId, key).coerceIn(1, 10)
        val interval = prefs.getTreePinInterval(farmId, key).coerceIn(1, 1440)

        prefs.addActiveTreePin(farmId, key)
        prefs.setTreePinSentCount(farmId, key, 1)
        prefs.setTreePinLastReadyAt(farmId, key, timer.readyAt)
        prefs.saveTreePinInfo(
            farmId, key,
            TreePinInfo(timer.title, timer.body, timer.category.key, timer.iconResId, timer.readyAt)
        )

        Log.i(TAG, "Starting reminder chain for $key (1/$max every $interval min)")
        showReminder(context, farmId, key, timer.title, timer.body, timer.category.key, timer.iconResId, 1, max)

        if (max > 1) {
            AlarmScheduler.scheduleTreePinRepeat(
                context, farmId, key, timer.title, timer.body,
                timer.category.key, timer.iconResId, interval
            )
        } else {
            stop(context, farmId, key)
        }
        return true
    }

    /** Called from AlarmReceiver for ACTION_TREE_PIN_REPEAT. */
    fun onRepeatFired(context: Context, intent: Intent) {
        val farmId = intent.getStringExtra(AlarmReceiver.EXTRA_FARM_ID) ?: return
        val key = intent.getStringExtra(EXTRA_PRODUCT_KEY) ?: return
        val prefs = PreferencesManager(context)

        if (!prefs.isTreePinEnabled(farmId, key)) {
            Log.i(TAG, "Reminder disabled while chain was active for $key — stopping")
            stop(context, farmId, key)
            return
        }

        val info = prefs.getTreePinInfo(farmId, key) ?: TreePinInfo(
            title = intent.getStringExtra(AlarmReceiver.EXTRA_TITLE) ?: "محصول",
            body = intent.getStringExtra(AlarmReceiver.EXTRA_BODY) ?: "آماده است",
            categoryKey = intent.getStringExtra(AlarmReceiver.EXTRA_CATEGORY) ?: "",
            iconResId = intent.getIntExtra(AlarmReceiver.EXTRA_ICON_RES_ID, 0),
            readyAt = 0L
        )
        val max = prefs.getTreePinMaxCount(farmId, key).coerceIn(1, 10)
        val interval = prefs.getTreePinInterval(farmId, key).coerceIn(1, 1440)
        val sent = prefs.getTreePinSentCount(farmId, key)

        if (sent >= max) {
            stop(context, farmId, key)
            return
        }

        val next = sent + 1
        showReminder(
            context, farmId, key, info.title, info.body,
            info.categoryKey, info.iconResId, next, max
        )
        prefs.setTreePinSentCount(farmId, key, next)

        if (next >= max) {
            stop(context, farmId, key)
        } else {
            AlarmScheduler.scheduleTreePinRepeat(
                context, farmId, key, info.title, info.body,
                info.categoryKey, info.iconResId, interval
            )
        }
    }

    private fun showReminder(
        context: Context,
        farmId: String,
        productKey: String,
        title: String,
        body: String,
        categoryKey: String,
        iconResId: Int,
        current: Int,
        max: Int
    ) {
        val text = "$body\n🔔 یادآوری $current از $max — تا برداشت کردن ادامه دارد"
        try {
            AlarmReceiver().showNotification(
                context,
                title,
                text,
                farmId,
                notificationId(farmId, productKey),
                categoryKey,
                iconResId,
                isOngoing = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show reminder: ${e.message}")
        }
    }

    fun stop(context: Context, farmId: String, productKey: String) {
        AlarmScheduler.cancelTreePinAlarm(context, farmId, productKey)
        val prefs = PreferencesManager(context)
        prefs.resetTreePin(farmId, productKey)
        prefs.clearTreePinInfo(farmId, productKey)
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(notificationId(farmId, productKey))
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear reminder notification: ${e.message}")
        }
    }

    /** Re-arm chains after reboot (pending intents do not survive a reboot). */
    fun restoreActive(context: Context) {
        val prefs = PreferencesManager(context)
        val farmId = prefs.farmId
        for (entry in prefs.getActiveTreePins()) {
            val parts = entry.split("@@", limit = 2)
            if (parts.size != 2 || parts[0] != farmId) continue
            val key = parts[1]
            val info = prefs.getTreePinInfo(farmId, key)
            if (info == null) {
                prefs.removeActiveTreePin(farmId, key)
                continue
            }
            val max = prefs.getTreePinMaxCount(farmId, key).coerceIn(1, 10)
            val sent = prefs.getTreePinSentCount(farmId, key)
            if (sent >= max) {
                stop(context, farmId, key)
                continue
            }
            AlarmScheduler.scheduleTreePinRepeat(
                context, farmId, key, info.title, info.body,
                info.categoryKey, info.iconResId,
                prefs.getTreePinInterval(farmId, key)
            )
        }
    }

    /**
     * Called after a fresh sync. If a pinned product no longer has a pending
     * timer, or its ready time moved into the future, it was harvested — so
     * the reminder chain is stopped automatically.
     */
    fun syncWithFreshTimers(
        context: Context,
        farmId: String,
        freshTimers: List<FarmTimerItem>
    ) {
        val prefs = PreferencesManager(context)
        val freshByKey = freshTimers.associateBy { it.productKey }
        for (entry in prefs.getActiveTreePins().toList()) {
            val parts = entry.split("@@", limit = 2)
            if (parts.size != 2 || parts[0] != farmId) continue
            val key = parts[1]
            val info = prefs.getTreePinInfo(farmId, key) ?: continue
            val fresh = freshByKey[key]
            if (fresh == null || fresh.readyAt > info.readyAt + 60_000L) {
                Log.i(TAG, "Harvest detected for $key — stopping reminders")
                stop(context, farmId, key)
            }
        }
    }
}
