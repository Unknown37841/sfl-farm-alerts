package com.sfl.notifapp.data.model

import com.google.gson.annotations.SerializedName

enum class TimerCategory(val key: String, val displayName: String, val emoji: String) {
    @SerializedName("crops")
    CROPS("crops", "Crops", "🌾"),

    @SerializedName("trees")
    TREES("trees", "Trees", "🪓"),

    @SerializedName("mining")
    MINING("mining", "Mining", "⛏️"),

    @SerializedName("composters")
    COMPOSTERS("composters", "Composters", "🪱"),

    @SerializedName("cooking")
    COOKING("cooking", "Cooking", "🍲"),

    @SerializedName("animals")
    ANIMALS("animals", "Animals", "🐔"),

    @SerializedName("greenhouse")
    GREENHOUSE("greenhouse", "Greenhouse", "🏡"),

    @SerializedName("beehives")
    BEEHIVES("beehives", "Beehives", "🐝"),

    @SerializedName("flowers")
    FLOWERS("flowers", "Flowers", "🌸"),

    @SerializedName("other")
    OTHER("other", "Other", "⏳");

    companion object {
        fun fromKey(key: String): TimerCategory {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: OTHER
        }
    }
}

data class FarmTimerItem(
    val id: String,
    val title: String,
    val body: String,
    val category: TimerCategory,
    val readyAt: Long,
    val totalDurationMs: Long = 0L,
    val count: Int = 1,
    val iconEmoji: String = "🌾"
) : Comparable<FarmTimerItem> {
    val isReady: Boolean
        get() = System.currentTimeMillis() >= readyAt

    val remainingMs: Long
        get() = (readyAt - System.currentTimeMillis()).coerceAtLeast(0L)

    override fun compareTo(other: FarmTimerItem): Int {
        return this.readyAt.compareTo(other.readyAt)
    }
}

data class SyncResult(
    val success: Boolean,
    val farmId: String,
    val timers: List<FarmTimerItem> = emptyList(),
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
