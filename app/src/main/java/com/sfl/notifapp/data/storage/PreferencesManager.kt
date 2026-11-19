package com.sfl.notifapp.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.model.TimerCategory
import com.sfl.notifapp.data.model.ResourceType

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "sfl_notif_prefs"
        private const val KEY_FARM_ID = "pref_farm_id"
        private const val KEY_NOTIFS_ENABLED = "pref_notifs_enabled"
        private const val KEY_CATEGORY_PREFIX = "pref_cat_"
        private const val KEY_RESOURCE_PREFIX = "pref_res_"
        private const val KEY_LAST_SYNC = "pref_last_sync"
        private const val KEY_CACHED_TIMERS = "pref_cached_timers"
        private const val KEY_AUTO_SYNC_MINS = "pref_auto_sync_mins"
        private const val KEY_SCHEDULED_ALARMS = "pref_scheduled_alarms"
        private const val KEY_BATTERY_PROMPT_SEEN = "pref_battery_prompt_seen"
        private const val KEY_NOTIF_CLICK_ACTION = "pref_notif_click_action"
        const val ACTION_OPEN_GAME = "GAME"
        const val ACTION_OPEN_APP = "APP"

        // --- Per-product harvest reminders (farmId + stable product key) ---
        private const val KEY_TREE_PIN_ENABLED = "pref_tree_pin_enabled"
        private const val KEY_TREE_PIN_INTERVAL = "pref_tree_pin_interval"
        private const val KEY_TREE_PIN_MAX = "pref_tree_pin_max_count"
        private const val KEY_TREE_PIN_SENT = "pref_tree_pin_sent"
        private const val KEY_TREE_PIN_LAST_READY = "pref_tree_pin_last_ready"
        private const val KEY_ACTIVE_TREE_PINS = "pref_active_tree_pins"
        private const val KEY_TREE_PIN_TITLE = "pref_tree_pin_title_"
        private const val KEY_TREE_PIN_BODY = "pref_tree_pin_body_"
        private const val KEY_TREE_PIN_CATEGORY = "pref_tree_pin_category_"
        private const val KEY_TREE_PIN_ICON = "pref_tree_pin_icon_"
        private const val KEY_TREE_PIN_READY_AT = "pref_tree_pin_ready_"
    }

    var notifClickAction: String
        get() = prefs.getString(KEY_NOTIF_CLICK_ACTION, ACTION_OPEN_GAME) ?: ACTION_OPEN_GAME
        set(value) = prefs.edit().putString(KEY_NOTIF_CLICK_ACTION, value).apply()

    var hasSeenBatteryPrompt: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_PROMPT_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_PROMPT_SEEN, value).apply()

    var farmId: String
        get() = prefs.getString(KEY_FARM_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FARM_ID, value.trim()).apply()

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFS_ENABLED, value).apply()

    fun isCategoryEnabled(category: TimerCategory): Boolean {
        return prefs.getBoolean(KEY_CATEGORY_PREFIX + category.key, true)
    }

    fun setCategoryEnabled(category: TimerCategory, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CATEGORY_PREFIX + category.key, enabled).apply()
    }

    /** True when the timer should fire: category on AND (for Resources) sub-resource on. */
    fun isTimerAllowed(category: TimerCategory, title: String): Boolean {
        if (!isCategoryEnabled(category)) return false
        if (category == TimerCategory.RESOURCES) {
            val resource = ResourceType.fromTitle(title) ?: return true
            return isResourceEnabled(resource)
        }
        return true
    }

    fun isResourceEnabled(resource: ResourceType): Boolean {
        return prefs.getBoolean(KEY_RESOURCE_PREFIX + resource.key, true)
    }

    fun setResourceEnabled(resource: ResourceType, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RESOURCE_PREFIX + resource.key, enabled).apply()
    }

    fun setAllResourcesEnabled(enabled: Boolean) {
        val editor = prefs.edit()
        ResourceType.entries.forEach { editor.putBoolean(KEY_RESOURCE_PREFIX + it.key, enabled) }
        editor.apply()
    }

    fun getEnabledResourcesCount(): Int {
        return ResourceType.entries.count { isResourceEnabled(it) }
    }

    fun setAllCategoriesEnabled(enabled: Boolean) {
        val editor = prefs.edit()
        TimerCategory.SFL_OVERVIEW_CATEGORIES.forEach { cat ->
            editor.putBoolean(KEY_CATEGORY_PREFIX + cat.key, enabled)
        }
        editor.apply()
    }

    fun getEnabledCategoriesCount(): Int {
        return TimerCategory.SFL_OVERVIEW_CATEGORIES.count { isCategoryEnabled(it) }
    }

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var lastSyncTime: Long
        get() = lastSyncTimestamp
        set(value) { lastSyncTimestamp = value }

    var autoSyncIntervalMinutes: Int
        get() = prefs.getInt(KEY_AUTO_SYNC_MINS, 15)
        set(value) = prefs.edit().putInt(KEY_AUTO_SYNC_MINS, value).apply()

    fun saveCachedTimers(timers: List<FarmTimerItem>) {
        val json = gson.toJson(timers)
        prefs.edit().putString(KEY_CACHED_TIMERS, json).apply()
    }

    fun getCachedTimers(): List<FarmTimerItem> {
        val json = prefs.getString(KEY_CACHED_TIMERS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<FarmTimerItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveScheduledAlarmIds(alarmIds: Set<String>) {
        prefs.edit().putStringSet(KEY_SCHEDULED_ALARMS, alarmIds).apply()
    }

    fun getScheduledAlarmIds(): Set<String> {
        return prefs.getStringSet(KEY_SCHEDULED_ALARMS, emptySet()) ?: emptySet()
    }

    fun getLastNotifiedTime(key: String): Long {
        return prefs.getLong("last_notif_$key", 0L)
    }

    fun saveLastNotifiedTime(key: String, timestamp: Long) {
        prefs.edit().putLong("last_notif_$key", timestamp).apply()
    }

    // ============ Per-product harvest reminders ============

    private fun pinKey(farmId: String, productKey: String) = "${farmId}@@${productKey}"

    fun isTreePinEnabled(farmId: String, productKey: String): Boolean =
        prefs.getBoolean(KEY_TREE_PIN_ENABLED + "_" + pinKey(farmId, productKey), false)

    fun setTreePinEnabled(farmId: String, productKey: String, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TREE_PIN_ENABLED + "_" + pinKey(farmId, productKey), enabled).apply()
    }

    /** 1..1440 minutes. */
    fun getTreePinInterval(farmId: String, productKey: String): Int =
        prefs.getInt(KEY_TREE_PIN_INTERVAL + "_" + pinKey(farmId, productKey), 10).coerceIn(1, 1440)

    fun setTreePinInterval(farmId: String, productKey: String, minutes: Int) {
        prefs.edit().putInt(
            KEY_TREE_PIN_INTERVAL + "_" + pinKey(farmId, productKey),
            minutes.coerceIn(1, 1440)
        ).apply()
    }

    /** 1..20 notifications for a single ready cycle. */
    fun getTreePinMaxCount(farmId: String, productKey: String): Int =
        prefs.getInt(KEY_TREE_PIN_MAX + "_" + pinKey(farmId, productKey), 3).coerceIn(1, 20)

    fun setTreePinMaxCount(farmId: String, productKey: String, count: Int) {
        prefs.edit().putInt(
            KEY_TREE_PIN_MAX + "_" + pinKey(farmId, productKey),
            count.coerceIn(1, 20)
        ).apply()
    }

    fun getTreePinSentCount(farmId: String, productKey: String): Int =
        prefs.getInt(KEY_TREE_PIN_SENT + "_" + pinKey(farmId, productKey), 0).coerceAtLeast(0)

    fun setTreePinSentCount(farmId: String, productKey: String, count: Int) {
        prefs.edit().putInt(KEY_TREE_PIN_SENT + "_" + pinKey(farmId, productKey), count).apply()
    }

    /** readyAt of the cycle the current reminder chain belongs to. */
    fun getTreePinLastReadyAt(farmId: String, productKey: String): Long =
        prefs.getLong(KEY_TREE_PIN_LAST_READY + "_" + pinKey(farmId, productKey), 0L)

    fun setTreePinLastReadyAt(farmId: String, productKey: String, readyAt: Long) {
        prefs.edit().putLong(KEY_TREE_PIN_LAST_READY + "_" + pinKey(farmId, productKey), readyAt).apply()
    }

    /** Active chains, used to restore alarms after a reboot. */
    fun getActiveTreePins(): Set<String> =
        prefs.getStringSet(KEY_ACTIVE_TREE_PINS, emptySet())?.toSet() ?: emptySet()

    fun addActiveTreePin(farmId: String, productKey: String) {
        val updated = getActiveTreePins().toMutableSet()
        updated.add("${farmId}@@${productKey}")
        prefs.edit().putStringSet(KEY_ACTIVE_TREE_PINS, updated).apply()
    }

    fun removeActiveTreePin(farmId: String, productKey: String) {
        val updated = getActiveTreePins().toMutableSet()
        updated.remove("${farmId}@@${productKey}")
        prefs.edit().putStringSet(KEY_ACTIVE_TREE_PINS, updated).apply()
    }

    /** Pending info for an active reminder chain (needed to restore after reboot). */
    fun saveTreePinInfo(farmId: String, productKey: String, info: TreePinInfo) {
        val key = pinKey(farmId, productKey)
        prefs.edit()
            .putString(KEY_TREE_PIN_TITLE + key, info.title)
            .putString(KEY_TREE_PIN_BODY + key, info.body)
            .putString(KEY_TREE_PIN_CATEGORY + key, info.categoryKey)
            .putInt(KEY_TREE_PIN_ICON + key, info.iconResId)
            .putLong(KEY_TREE_PIN_READY_AT + key, info.readyAt)
            .apply()
    }

    fun getTreePinInfo(farmId: String, productKey: String): TreePinInfo? {
        val key = pinKey(farmId, productKey)
        if (!prefs.contains(KEY_TREE_PIN_TITLE + key)) return null
        return TreePinInfo(
            title = prefs.getString(KEY_TREE_PIN_TITLE + key, "") ?: "",
            body = prefs.getString(KEY_TREE_PIN_BODY + key, "") ?: "",
            categoryKey = prefs.getString(KEY_TREE_PIN_CATEGORY + key, "") ?: "",
            iconResId = prefs.getInt(KEY_TREE_PIN_ICON + key, 0),
            readyAt = prefs.getLong(KEY_TREE_PIN_READY_AT + key, 0L)
        )
    }

    fun clearTreePinInfo(farmId: String, productKey: String) {
        val key = pinKey(farmId, productKey)
        prefs.edit()
            .remove(KEY_TREE_PIN_TITLE + key)
            .remove(KEY_TREE_PIN_BODY + key)
            .remove(KEY_TREE_PIN_CATEGORY + key)
            .remove(KEY_TREE_PIN_ICON + key)
            .remove(KEY_TREE_PIN_READY_AT + key)
            .apply()
    }

    fun resetTreePin(farmId: String, productKey: String) {
        removeActiveTreePin(farmId, productKey)
        setTreePinSentCount(farmId, productKey, 0)
        setTreePinLastReadyAt(farmId, productKey, 0L)
    }
}

data class TreePinInfo(
    val title: String,
    val body: String,
    val categoryKey: String,
    val iconResId: Int,
    val readyAt: Long
)