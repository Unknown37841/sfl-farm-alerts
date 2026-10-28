package com.sfl.notifapp.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.model.TimerCategory

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "sfl_notif_prefs"
        private const val KEY_FARM_ID = "pref_farm_id"
        private const val KEY_NOTIFS_ENABLED = "pref_notifs_enabled"
        private const val KEY_CATEGORY_PREFIX = "pref_cat_"
        private const val KEY_LAST_SYNC = "pref_last_sync"
        private const val KEY_CACHED_TIMERS = "pref_cached_timers"
        private const val KEY_AUTO_SYNC_MINS = "pref_auto_sync_mins"
        private const val KEY_SCHEDULED_ALARMS = "pref_scheduled_alarms"
    }

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

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

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
}
