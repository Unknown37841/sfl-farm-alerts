package com.sfl.notifapp.data.model

import com.google.gson.annotations.SerializedName
import com.sfl.notifapp.R

enum class TimerCategory(
    val key: String,
    val displayName: String,
    val emoji: String,
    val iconResId: Int
) {
    @SerializedName("crops")
    CROPS("crops", "Crops", "🌾", R.drawable.ic_crop),

    @SerializedName("fruit_trees")
    FRUIT_TREES("fruit_trees", "Fruit Trees", "🍊", R.drawable.ic_fruit),

    @SerializedName("trees")
    TREES("trees", "Trees", "🪓", R.drawable.ic_tree),

    @SerializedName("mining")
    MINING("mining", "Mining", "⛏️", R.drawable.ic_mining),

    @SerializedName("oil")
    OIL("oil", "Oil", "🛢️", R.drawable.ic_oil),

    @SerializedName("composters")
    COMPOSTERS("composters", "Composters", "🪱", R.drawable.ic_compost),

    @SerializedName("cooking")
    COOKING("cooking", "Cooking", "🍲", R.drawable.ic_cooking),

    @SerializedName("animals")
    ANIMALS("animals", "Animals", "🐔", R.drawable.ic_animal),

    @SerializedName("greenhouse")
    GREENHOUSE("greenhouse", "Greenhouse", "🏡", R.drawable.ic_greenhouse),

    @SerializedName("beehives")
    BEEHIVES("beehives", "Beehives", "🐝", R.drawable.ic_beehive),

    @SerializedName("flowers")
    FLOWERS("flowers", "Flowers", "🌸", R.drawable.ic_crop),

    @SerializedName("other")
    OTHER("other", "Other", "⏳", R.drawable.ic_app_sunflower);

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

    val iconResId: Int
        get() = category.iconResId

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
