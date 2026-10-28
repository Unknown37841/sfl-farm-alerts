package com.sfl.notifapp.data.model

import com.google.gson.annotations.SerializedName
import com.sfl.notifapp.R


val SFL_OVERVIEW_CATEGORIES: List<TimerCategory> = TimerCategory.SFL_OVERVIEW_CATEGORIES

enum class TimerCategory(
    val key: String,
    val displayName: String,
    val faName: String,
    val emoji: String,
    val iconResId: Int
) {
    @SerializedName("crops")
    CROPS("crops", "Crops", "محصولات زراعی", "🌾", R.drawable.ic_crop),

    @SerializedName("fruit_patches")
    FRUIT_PATCHES("fruit_patches", "Fruit Patches", "درختان میوه", "🍊", R.drawable.ic_fruit),

    @SerializedName("greenhouse")
    GREENHOUSE("greenhouse", "Greenhouse", "گلخانه", "🏡", R.drawable.ic_greenhouse),

    @SerializedName("crop_machine")
    CROP_MACHINE("crop_machine", "Crop Machine", "ماشین برداشت مکانیزه", "⚙️", R.drawable.ic_crop),

    @SerializedName("flowers")
    FLOWERS("flowers", "Flowers", "باغچه گل‌ها", "🌸", R.drawable.ic_cat_flowers),

    @SerializedName("beehives")
    BEEHIVES("beehives", "Beehives", "کندوهای عسل", "🐝", R.drawable.ic_beehive),

    @SerializedName("resources")
    RESOURCES("resources", "Resources", "منابع (چوب، سنگ، آهن، طلا، نفت)", "🪓", R.drawable.ic_tree),

    @SerializedName("lava_pits")
    LAVA_PITS("lava_pits", "Lava Pits", "حوضچه‌های لاوا", "🌋", R.drawable.ic_cat_lava_pit),

    @SerializedName("salt")
    SALT("salt", "Salt", "تولید نمک", "🧂", R.drawable.ic_cat_salt),

    @SerializedName("crab_traps")
    CRAB_TRAPS("crab_traps", "Crab Traps", "تله‌های خرچنگ", "🦀", R.drawable.ic_cat_crab),

    @SerializedName("animals")
    ANIMALS("animals", "Animals", "حیوانات مزرعه", "🐔", R.drawable.ic_animal),

    @SerializedName("petting")
    PETTING("petting", "Petting", "نوازش حیوانات (Love)", "✋", R.drawable.ic_animal),

    @SerializedName("fire_pit")
    FIRE_PIT("fire_pit", "Fire Pit", "آتشگاه (Fire Pit)", "🔥", R.drawable.ic_cat_fire_pit),

    @SerializedName("bakery")
    BAKERY("bakery", "Bakery", "نانوایی (Bakery)", "🥧", R.drawable.ic_cat_bakery),

    @SerializedName("deli")
    DELI("deli", "Deli", "اغذیه / دلی (Deli)", "🥗", R.drawable.ic_cat_deli),

    @SerializedName("smoothie_shack")
    SMOOTHIE_SHACK("smoothie_shack", "Smoothie Shack", "اسموتی شَک", "🥤", R.drawable.ic_cat_smoothie),

    @SerializedName("kitchen")
    KITCHEN("kitchen", "Kitchen", "آشپزخانه بزرگ (Kitchen)", "🍳", R.drawable.ic_cat_kitchen),

    @SerializedName("fish_market")
    FISH_MARKET("fish_market", "Fish Market", "بازار ماهی (Fish Market)", "🐟", R.drawable.ic_cat_fish_market),

    @SerializedName("composters")
    COMPOSTERS("composters", "Composters", "کمپوست‌سازها", "🪱", R.drawable.ic_compost),

    @SerializedName("cave")
    CAVE("cave", "Cave", "غار قارچ (Cave)", "⛏️", R.drawable.ic_mining),

    @SerializedName("aging_rack")
    AGING_RACK("aging_rack", "Aging Rack", "قفسه کهنه‌سازی", "🧀", R.drawable.ic_cat_aging_rack),

    @SerializedName("fermentation_rack")
    FERMENTATION_RACK("fermentation_rack", "Fermentation Rack", "قفسه تخمیر", "🫙", R.drawable.ic_cat_deli),

    @SerializedName("spice_rack")
    SPICE_RACK("spice_rack", "Spice Rack", "قفسه ادویه‌جات", "🌶️", R.drawable.ic_cat_spice),

    @SerializedName("crafting_box")
    CRAFTING_BOX("crafting_box", "Crafting Box", "جعبه ساخت ابزار", "📦", R.drawable.ic_cat_crafting_box),

    @SerializedName("love_island")
    LOVE_ISLAND("love_island", "Love Island", "جزیره شناور (Love Island)", "❤️", R.drawable.ic_app_sunflower),

    @SerializedName("bud_box")
    BUD_BOX("bud_box", "Bud Box", "جعبه جوانه (Bud Box)", "🎁", R.drawable.ic_cat_chest),

    @SerializedName("power_skills")
    POWER_SKILLS("power_skills", "Power Skills", "مهارت‌های قدرتی", "⚡", R.drawable.ic_app_sunflower),

    @SerializedName("quests")
    QUESTS("quests", "Quests", "ماموریت‌های روزانه", "📜", R.drawable.ic_app_sunflower),

    @SerializedName("village_projects")
    VILLAGE_PROJECTS("village_projects", "Village Projects", "پروژه‌های روستا", "🏛️", R.drawable.ic_tree),

    @SerializedName("other")
    OTHER("other", "Other", "سایر موارد", "⏳", R.drawable.ic_app_sunflower);

    companion object {
        fun fromKey(key: String): TimerCategory {
            val norm = key.lowercase().trim().replace(" ", "_").replace("-", "_")
            return when (norm) {
                "fruit_trees" -> FRUIT_PATCHES
                "trees", "mining", "oil" -> RESOURCES
                "cooking" -> FIRE_PIT
                else -> entries.find { 
                    it.key.equals(norm, ignoreCase = true) || 
                    it.displayName.equals(key.trim(), ignoreCase = true) 
                } ?: OTHER
            }
        }

        /**
         * The official 29 categories from SFL Overview in exact order
         */
        val SFL_OVERVIEW_CATEGORIES: List<TimerCategory> = listOf(
            CROPS,
            FRUIT_PATCHES,
            GREENHOUSE,
            CROP_MACHINE,
            FLOWERS,
            BEEHIVES,
            RESOURCES,
            LAVA_PITS,
            SALT,
            CRAB_TRAPS,
            ANIMALS,
            PETTING,
            FIRE_PIT,
            BAKERY,
            DELI,
            SMOOTHIE_SHACK,
            KITCHEN,
            FISH_MARKET,
            COMPOSTERS,
            CAVE,
            AGING_RACK,
            FERMENTATION_RACK,
            SPICE_RACK,
            CRAFTING_BOX,
            LOVE_ISLAND,
            BUD_BOX,
            POWER_SKILLS,
            QUESTS,
            VILLAGE_PROJECTS
        )
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
