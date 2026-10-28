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
    PETTING("petting", "Petting", "نوازش حیوانات (Love)", "✋", R.drawable.ic_item_petting),

    @SerializedName("fire_pit")
    FIRE_PIT("fire_pit", "Fire Pit", "آتشگاه (Fire Pit)", "🔥", R.drawable.ic_item_mashed_potato),

    @SerializedName("bakery")
    BAKERY("bakery", "Bakery", "نانوایی (Bakery)", "🥧", R.drawable.ic_item_apple_pie),

    @SerializedName("deli")
    DELI("deli", "Deli", "اغذیه / دلی (Deli)", "🥗", R.drawable.ic_item_club_sandwich),

    @SerializedName("smoothie_shack")
    SMOOTHIE_SHACK("smoothie_shack", "Smoothie Shack", "اسموتی شَک", "🥤", R.drawable.ic_item_carrot_juice),

    @SerializedName("kitchen")
    KITCHEN("kitchen", "Kitchen", "آشپزخانه بزرگ (Kitchen)", "🍳", R.drawable.ic_item_pizza),

    @SerializedName("fish_market")
    FISH_MARKET("fish_market", "Fish Market", "بازار ماهی (Fish Market)", "🐟", R.drawable.ic_item_fish_and_chips),

    @SerializedName("composters")
    COMPOSTERS("composters", "Composters", "کمپوست‌سازها", "🪱", R.drawable.ic_compost),

    @SerializedName("cave")
    CAVE("cave", "Cave", "غار قارچ (Cave)", "⛏️", R.drawable.ic_item_iron),

    @SerializedName("aging_rack")
    AGING_RACK("aging_rack", "Aging Rack", "قفسه کهنه‌سازی", "🧀", R.drawable.ic_cat_aging_rack),

    @SerializedName("fermentation_rack")
    FERMENTATION_RACK("fermentation_rack", "Fermentation Rack", "قفسه تخمیر", "🫙", R.drawable.ic_item_club_sandwich),

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
    val iconEmoji: String = "🌾",
    val specificIconResId: Int? = null
) : Comparable<FarmTimerItem> {
    val isReady: Boolean
        get() = System.currentTimeMillis() >= readyAt

    val remainingMs: Long
        get() = (readyAt - System.currentTimeMillis()).coerceAtLeast(0L)

    val iconResId: Int
        get() = specificIconResId ?: resolveItemIcon(title, category) ?: category.iconResId

    override fun compareTo(other: FarmTimerItem): Int {
        return this.readyAt.compareTo(other.readyAt)
    }
}

/**
 * Maps specific game items to authentic high-resolution game icons.
 */
fun resolveItemIcon(title: String, category: TimerCategory): Int? {
    val lower = title.lowercase()
    return when {
        // Fruits
        lower.contains("orange") -> R.drawable.ic_item_orange
        lower.contains("apple") && !lower.contains("pie") -> R.drawable.ic_item_apple
        lower.contains("banana") -> R.drawable.ic_item_banana
        lower.contains("blueberry") -> R.drawable.ic_item_blueberry
        lower.contains("lemon") -> R.drawable.ic_item_lemon
        lower.contains("tomato") -> R.drawable.ic_item_tomato
        lower.contains("celestine") -> R.drawable.ic_item_celestine
        lower.contains("lunara") -> R.drawable.ic_item_lunara
        lower.contains("duskberry") -> R.drawable.ic_item_duskberry

        // Greenhouse
        lower.contains("grape") -> R.drawable.ic_item_grape
        lower.contains("olive") -> R.drawable.ic_item_olive
        lower.contains("rice") -> R.drawable.ic_item_rice

        // Flowers
        lower.contains("carnation") -> when {
            lower.contains("red") -> R.drawable.ic_flower_carnation_red
            lower.contains("purple") -> R.drawable.ic_flower_carnation_purple
            lower.contains("white") -> R.drawable.ic_flower_carnation_white
            lower.contains("yellow") -> R.drawable.ic_flower_carnation_yellow
            else -> R.drawable.ic_flower_carnation_blue
        }
        lower.contains("lotus") -> when {
            lower.contains("red") -> R.drawable.ic_flower_lotus_red
            lower.contains("purple") -> R.drawable.ic_flower_lotus_purple
            lower.contains("white") -> R.drawable.ic_flower_lotus_white
            lower.contains("yellow") -> R.drawable.ic_flower_lotus_yellow
            else -> R.drawable.ic_flower_lotus_purple
        }
        lower.contains("edelweiss") -> when {
            lower.contains("red") -> R.drawable.ic_flower_edelweiss_red
            lower.contains("purple") -> R.drawable.ic_flower_edelweiss_purple
            lower.contains("white") -> R.drawable.ic_flower_edelweiss_white
            lower.contains("yellow") -> R.drawable.ic_flower_edelweiss_yellow
            else -> R.drawable.ic_flower_edelweiss_blue
        }
        lower.contains("gladiolus") -> when {
            lower.contains("red") -> R.drawable.ic_flower_gladiolus_red
            lower.contains("purple") -> R.drawable.ic_flower_gladiolus_purple
            lower.contains("white") -> R.drawable.ic_flower_gladiolus_white
            lower.contains("yellow") -> R.drawable.ic_flower_gladiolus_yellow
            else -> R.drawable.ic_flower_gladiolus_blue
        }
        lower.contains("lavender") -> when {
            lower.contains("red") -> R.drawable.ic_flower_lavender_red
            lower.contains("purple") -> R.drawable.ic_flower_lavender_purple
            lower.contains("white") -> R.drawable.ic_flower_lavender_white
            lower.contains("yellow") -> R.drawable.ic_flower_lavender_yellow
            else -> R.drawable.ic_flower_lavender_blue
        }
        lower.contains("clover") -> when {
            lower.contains("red") -> R.drawable.ic_flower_clover_red
            lower.contains("purple") -> R.drawable.ic_flower_clover_purple
            lower.contains("yellow") -> R.drawable.ic_flower_clover_yellow
            else -> R.drawable.ic_flower_clover_blue
        }
        lower.contains("cosmos") -> when {
            lower.contains("red") -> R.drawable.ic_flower_cosmos_red
            lower.contains("purple") -> R.drawable.ic_flower_cosmos_purple
            lower.contains("white") -> R.drawable.ic_flower_cosmos_white
            lower.contains("yellow") -> R.drawable.ic_flower_cosmos_yellow
            else -> R.drawable.ic_flower_cosmos_blue
        }
        lower.contains("pansy") -> when {
            lower.contains("red") -> R.drawable.ic_flower_pansy_red
            lower.contains("purple") -> R.drawable.ic_flower_pansy_purple
            lower.contains("white") -> R.drawable.ic_flower_pansy_white
            lower.contains("yellow") -> R.drawable.ic_flower_pansy_yellow
            else -> R.drawable.ic_flower_pansy_blue
        }
        lower.contains("daffodil") -> when {
            lower.contains("red") -> R.drawable.ic_flower_daffodil_red
            lower.contains("purple") -> R.drawable.ic_flower_daffodil_purple
            lower.contains("white") -> R.drawable.ic_flower_daffodil_white
            lower.contains("yellow") -> R.drawable.ic_flower_daffodil_yellow
            else -> R.drawable.ic_flower_daffodil_blue
        }
        lower.contains("balloon") -> R.drawable.ic_flower_balloon_blue
        lower.contains("chamomile") -> R.drawable.ic_flower_chamomile
        lower.contains("frostbloom") -> R.drawable.ic_flower_frostbloom
        lower.contains("primula") -> R.drawable.ic_flower_primula
        lower.contains("prism") -> R.drawable.ic_flower_prism_petal
        lower.contains("lunalist") -> R.drawable.ic_flower_lunalist

        // Resources
        lower.contains("honey") || category == TimerCategory.BEEHIVES -> R.drawable.ic_item_honey
        lower.contains("oil") -> R.drawable.ic_item_oil
        lower.contains("iron") -> R.drawable.ic_item_iron
        lower.contains("gold") -> R.drawable.ic_item_gold
        lower.contains("crimstone") -> R.drawable.ic_item_crimstone
        lower.contains("sunstone") -> R.drawable.ic_item_sunstone
        lower.contains("salt") || category == TimerCategory.SALT -> R.drawable.ic_item_salt
        lower.contains("lava") || category == TimerCategory.LAVA_PITS -> R.drawable.ic_cat_lava_pit

        // Composters
        lower.contains("sprout mix") -> R.drawable.ic_item_sprout_mix
        lower.contains("fruitful blend") -> R.drawable.ic_item_fruitful_blend
        lower.contains("rapid root") -> R.drawable.ic_item_rapid_root
        lower.contains("wormery") -> R.drawable.ic_item_wormery
        lower.contains("compost") -> R.drawable.ic_compost

        // Animals & Feeds
        lower.contains("pet") || category == TimerCategory.PETTING -> R.drawable.ic_item_petting
        lower.contains("chicken") || lower.contains("egg") -> R.drawable.ic_animal
        lower.contains("hay") -> R.drawable.ic_item_hay
        lower.contains("kernel") -> R.drawable.ic_item_kernel_blend
        lower.contains("barn delight") -> R.drawable.ic_item_barn_delight
        lower.contains("mixed grain") -> R.drawable.ic_item_mixed_grain
        lower.contains("nutri barley") -> R.drawable.ic_item_nutri_barley

        // Crafting
        lower.contains("crafting") || category == TimerCategory.CRAFTING_BOX -> R.drawable.ic_cat_crafting_box

        // Food & Cooking
        lower.contains("apple pie") -> R.drawable.ic_item_apple_pie
        lower.contains("carrot juice") -> R.drawable.ic_item_carrot_juice
        lower.contains("mashed potato") -> R.drawable.ic_item_mashed_potato
        lower.contains("pumpkin soup") -> R.drawable.ic_item_pumpkin_soup
        lower.contains("boiled egg") -> R.drawable.ic_item_boiled_eggs
        lower.contains("pancake") -> R.drawable.ic_item_pancakes
        lower.contains("sunflower cake") -> R.drawable.ic_item_sunflower_cake
        lower.contains("pizza") -> R.drawable.ic_item_pizza
        lower.contains("club sandwich") || lower.contains("sandwich") -> R.drawable.ic_item_club_sandwich
        lower.contains("fish and chips") -> R.drawable.ic_item_fish_and_chips
        lower.contains("corn bread") -> R.drawable.ic_item_corn_bread

        else -> null
    }
}

data class SyncResult(
    val success: Boolean,
    val farmId: String,
    val timers: List<FarmTimerItem> = emptyList(),
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
