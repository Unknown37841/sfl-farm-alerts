package com.sfl.notifapp.data.engine

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.model.TimerCategory
import com.sfl.notifapp.data.model.resolveItemIcon
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

object SflTimerEngine {

    // Base crop durations in milliseconds
    private val CROP_DURATIONS = mapOf(
        "Sunflower" to 60_000L,
        "Potato" to 5 * 60_000L,
        "Rhubarb" to 10 * 60_000L,
        "Pumpkin" to 30 * 60_000L,
        "Zucchini" to 30 * 60_000L,
        "Carrot" to 60 * 60_000L,
        "Yam" to 60 * 60_000L,
        "Cabbage" to 2 * 3600_000L,
        "Broccoli" to 2 * 3600_000L,
        "Soybean" to 3 * 3600_000L,
        "Beetroot" to 4 * 3600_000L,
        "Pepper" to 4 * 3600_000L,
        "Cauliflower" to 8 * 3600_000L,
        "Parsnip" to 12 * 3600_000L,
        "Eggplant" to 16 * 3600_000L,
        "Corn" to 20 * 3600_000L,
        "Onion" to 20 * 3600_000L,
        "Radish" to 24 * 3600_000L,
        "Wheat" to 24 * 3600_000L,
        "Turnip" to 24 * 3600_000L,
        "Kale" to 36 * 3600_000L,
        "Artichoke" to 36 * 3600_000L,
        "Barley" to 48 * 3600_000L,
        "Saltwort" to 12 * 3600_000L,
        "Crystal Leaf" to 24 * 3600_000L
    )

    private val FRUIT_DURATIONS = mapOf(
        "Tomato" to 2 * 3600_000L,
        "Lemon" to 4 * 3600_000L,
        "Blueberry" to 6 * 3600_000L,
        "Orange" to 8 * 3600_000L,
        "Apple" to 12 * 3600_000L,
        "Banana" to 12 * 3600_000L,
        "Celestine" to 6 * 3600_000L,
        "Lunara" to 12 * 3600_000L,
        "Duskberry" to 24 * 3600_000L,
        "Grape" to 12 * 3600_000L
    )

    private val GREENHOUSE_DURATIONS = mapOf(
        "Grape" to 12 * 3600_000L,
        "Rice" to 16 * 3600_000L,
        "Olive" to 24 * 3600_000L
    )

    private fun getFlowerDuration(name: String): Long {
        val lower = name.lowercase()
        return when {
            lower.contains("carnation") || lower.contains("lotus") -> 5 * 24 * 3600_000L // 5 days (Lily)
            lower.contains("balloon") || lower.contains("daffodil") -> 2 * 24 * 3600_000L // 2 days (Bloom)
            lower.contains("edelweiss") || lower.contains("gladiolus") ||
            lower.contains("lavender") || lower.contains("clover") -> 3 * 24 * 3600_000L // 3 days
            lower.contains("pansy") || lower.contains("cosmos") -> 24 * 3600_000L // 1 day (Sunpetal)
            else -> 24 * 3600_000L
        }
    }

    private val POWER_SKILL_COOLDOWNS = mapOf(
        "Instant Growth" to 24 * 3600_000L,
        "Tree Whisperer" to 24 * 3600_000L,
        "Mining Miracle" to 24 * 3600_000L,
        "Barn Blessing" to 24 * 3600_000L,
        "Gold Rush" to 24 * 3600_000L
    )

    private const val TREE_DURATION_MS = 2 * 3600_000L
    private const val STONE_DURATION_MS = 4 * 3600_000L
    private const val IRON_DURATION_MS = 8 * 3600_000L
    private const val GOLD_DURATION_MS = 24 * 3600_000L
    private const val CRIMSTONE_DURATION_MS = 24 * 3600_000L
    private const val SUNSTONE_DURATION_MS = 72 * 3600_000L
    private const val OIL_DURATION_MS = 20 * 3600_000L
    private const val CHICKEN_DURATION_MS = 20 * 60_000L
    private const val DEFAULT_LAVA_PIT_MS = 72 * 3600_000L
    private const val DEFAULT_SALT_NODE_MS = 12 * 3600_000L
    private const val DEFAULT_CRAB_TRAP_MS = 4 * 3600_000L

    data class RawTimer(
        val category: TimerCategory,
        val itemName: String,
        val readyAt: Long,
        val durationMs: Long,
        val icon: String
    )

    fun extractTimers(root: JsonObject): List<FarmTimerItem> {
        val farm = if (root.has("farm") && root.get("farm").isJsonObject) {
            root.getAsJsonObject("farm")
        } else {
            root
        }

        val rawList = mutableListOf<RawTimer>()

        // 1. Crops & Fruit Patches
        extractCrops(farm, rawList)
        extractFruits(farm, rawList)

        // 2. Greenhouse
        extractGreenhouse(farm, rawList)

        // 3. Crop Machine
        extractCropMachine(farm, rawList)

        // 4. Flowers & Beehives
        extractFlowers(farm, rawList)
        extractBeehives(farm, rawList)

        // 5. Resources (Trees, Stones, Iron, Gold, Crimstone, Sunstone, Oil)
        extractResources(farm, rawList)

        // 6. Lava Pits & Salt
        extractLavaPits(farm, rawList)
        extractSalt(farm, rawList)

        // 7. Crab Traps
        extractCrabTraps(farm, rawList)

        // 8. Animals & Petting
        extractAnimals(farm, rawList)
        extractPetting(farm, rawList)

        // 9. Cooking Buildings: Fire Pit, Bakery, Deli, Smoothie Shack, Kitchen, Fish Market
        extractCookingBuildings(farm, rawList)

        // 10. Composters
        extractComposters(farm, rawList)

        // 11. Cave
        extractCave(farm, rawList)

        // 12. Aging Shed: Aging Rack, Fermentation Rack, Spice Rack
        extractAgingShed(farm, rawList)

        // 13. Crafting Box
        extractCraftingBox(farm, rawList)

        // 14. Love Island & Bud Box
        extractLoveIsland(farm, rawList)
        extractBudBox(farm, rawList)

        // 15. Power Skills
        extractPowerSkills(farm, rawList)

        // 16. Quests & Village Projects
        extractQuests(farm, rawList)
        extractVillageProjects(farm, rawList)

        // Cluster raw timers that are within 90 seconds of each other
        return clusterTimers(rawList)
    }

    // 1. Crops
    private fun extractCrops(farm: JsonObject, out: MutableList<RawTimer>) {
        val crops = farm.getAsJsonObjectSafe("crops") ?: return
        val farmWindows = getCropPlotWindows(farm)
        for ((_, elem) in crops.entrySet()) {
            if (!elem.isJsonObject) continue
            val plot = elem.asJsonObject
            val crop = plot.getAsJsonObjectSafe("crop") ?: continue
            if (!crop.has("plantedAt")) continue

            val plantedAt = crop.get("plantedAt").asLong
            val name = crop.getSafeString("name") ?: "Crop"
            val baseDur = crop.getEffectiveDurationMs(CROP_DURATIONS[name] ?: 3600_000L)
            // Grow-time boost windows: farm-wide buffs + this plot's fertiliser (e.g. Rapid Root 2x)
            val windows = farmWindows + getFertiliserWindow(
                plot,
                "Rapid Root" to 2.0, "Sproutroot Surprise" to 2.0
            )
            val readyAt = computeWindowedReadyAt(plantedAt, baseDur, windows)

            out.add(RawTimer(TimerCategory.CROPS, name, readyAt, baseDur, "🌾"))
        }
    }

    // 2. Fruit Patches
    private fun extractFruits(farm: JsonObject, out: MutableList<RawTimer>) {
        val fruits = farm.getAsJsonObjectSafe("fruitPatches") ?: return
        val farmWindows = getFruitPatchWindows(farm)
        for ((_, elem) in fruits.entrySet()) {
            if (!elem.isJsonObject) continue
            val patch = elem.asJsonObject
            val fruit = patch.getAsJsonObjectSafe("fruit") ?: continue

            val harvestsLeft = fruit.getSafeLong("harvestsLeft") ?: 1L
            // If tree is depleted (0 harvests left), skip timer until chopped and replanted
            if (harvestsLeft <= 0L) continue

            val plantedAt = fruit.getSafeLong("plantedAt") ?: 0L
            val harvestedAt = fruit.getSafeLong("harvestedAt") ?: 0L
            val startedAt = maxOf(harvestedAt, plantedAt)
            if (startedAt <= 0L) continue

            val name = fruit.getSafeString("name") ?: "Fruit"
            val baseDur = fruit.getEffectiveDurationMs(FRUIT_DURATIONS[name] ?: (8 * 3600_000L))
            // Boost windows + Turbofruit Mix fertiliser applied on the patch
            val windows = farmWindows + getFertiliserWindow(patch, "Turbofruit Mix" to 1.25)
            val readyAt = computeWindowedReadyAt(startedAt, baseDur, windows)

            out.add(RawTimer(TimerCategory.FRUIT_PATCHES, name, readyAt, baseDur, "🍊"))
        }
    }

    // 3. Greenhouse
    private fun extractGreenhouse(farm: JsonObject, out: MutableList<RawTimer>) {
        val gh = farm.getAsJsonObjectSafe("greenhouse") ?: return
        val pots = gh.getAsJsonObjectSafe("pots") ?: return
        for ((_, elem) in pots.entrySet()) {
            if (!elem.isJsonObject) continue
            val pot = elem.asJsonObject
            val plant = pot.getAsJsonObjectSafe("plant") ?: continue
            if (plant.has("plantedAt")) {
                val plantedAt = plant.get("plantedAt").asLong
                val name = plant.getSafeString("name") ?: "Greenhouse Plant"
                val baseDur = plant.getEffectiveDurationMs(GREENHOUSE_DURATIONS[name] ?: (12 * 3600_000L))
                // Boost windows + Greenhouse Glow fertiliser applied on the pot
                val windows = getGreenhouseWindows(farm, name) +
                    getFertiliserWindow(pot, "Greenhouse Glow" to 1.25)
                val readyAt = computeWindowedReadyAt(plantedAt, baseDur, windows)
                out.add(RawTimer(TimerCategory.GREENHOUSE, name, readyAt, baseDur, "🏡"))
            }
        }
    }

    // 4. Crop Machine
    private fun extractCropMachine(farm: JsonObject, out: MutableList<RawTimer>) {
        val buildings = farm.getAsJsonObjectSafe("buildings")
        val cmArray = buildings?.getAsJsonArraySafe("Crop Machine")
        val cmObj = if (cmArray != null && cmArray.size() > 0 && cmArray.get(0).isJsonObject) {
            cmArray.get(0).asJsonObject
        } else {
            farm.getAsJsonObjectSafe("cropMachine")
        } ?: return

        val queue = cmObj.getAsJsonArraySafe("queue") ?: return
        for (i in 0 until queue.size()) {
            val itemElem = queue.get(i)
            if (!itemElem.isJsonObject) continue
            val pack = itemElem.asJsonObject
            val crop = pack.getSafeString("crop") ?: "Crop Pack"
            val readyAt = pack.getSafeLong("readyAt") ?: continue
            val dur = pack.getSafeLong("growTimeRemaining") ?: 3600_000L
            out.add(RawTimer(TimerCategory.CROP_MACHINE, "Pack: $crop", readyAt, dur, "⚙️"))
        }
    }

    // 5. Flowers
    private fun extractFlowers(farm: JsonObject, out: MutableList<RawTimer>) {
        val flowers = farm.getAsJsonObjectSafe("flowers") ?: return
        val beds = flowers.getAsJsonObjectSafe("flowerBeds") ?: return
        for ((_, elem) in beds.entrySet()) {
            if (!elem.isJsonObject) continue
            val bed = elem.asJsonObject
            val plant = bed.getAsJsonObjectSafe("flower") ?: continue
            if (plant.has("plantedAt")) {
                val plantedAt = plant.get("plantedAt").asLong
                val name = plant.getSafeString("name") ?: "Flower"
                val baseDur = plant.getEffectiveDurationMs(getFlowerDuration(name))
                // Boost windows: Blossom Hourglass / Moth Shrine
                val windows = getFlowerBedWindows(farm)
                val readyAt = computeWindowedReadyAt(plantedAt, baseDur, windows)
                out.add(RawTimer(TimerCategory.FLOWERS, name, readyAt, baseDur, "🌸"))
            }
        }
    }

    // 6. Beehives
    private fun extractBeehives(farm: JsonObject, out: MutableList<RawTimer>) {
        val hives = farm.getAsJsonObjectSafe("beehives") ?: return
        val targetMs = 24 * 3600_000L // 86,400,000 ms = full honey production time
        val now = System.currentTimeMillis()

        for ((_, elem) in hives.entrySet()) {
            if (!elem.isJsonObject) continue
            val hive = elem.asJsonObject
            val honey = hive.getAsJsonObjectSafe("honey")
            val updatedAt = honey?.getSafeLong("updatedAt") ?: hive.getSafeLong("updatedAt") ?: 0L
            val produced = honey?.getSafeDouble("produced") ?: 0.0

            val flowersArray = hive.getAsJsonArraySafe("flowers")
            val flowers = mutableListOf<AttachedFlower>()
            if (flowersArray != null) {
                for (fElem in flowersArray) {
                    if (!fElem.isJsonObject) continue
                    val fObj = fElem.asJsonObject
                    val attAt = fObj.getSafeLong("attachedAt") ?: 0L
                    val attUntil = fObj.getSafeLong("attachedUntil") ?: 0L
                    val rate = fObj.getSafeDouble("rate") ?: 1.0
                    if (attUntil > attAt) {
                        flowers.add(AttachedFlower(attAt, attUntil, rate))
                    }
                }
            }
            flowers.sortBy { it.attachedAt }

            // 1. Calculate current honey produced up to now
            var currentHoney = produced
            for (flower in flowers) {
                val start = maxOf(updatedAt, flower.attachedAt)
                val end = minOf(now, flower.attachedUntil)
                if (end > start) {
                    currentHoney += (end - start) * flower.rate
                }
            }

            // 2. Determine readyAt
            if (currentHoney >= targetMs) {
                // Honey is ready to collect now
                val readyAt = if (updatedAt > 0L) updatedAt else (now - 1000L)
                out.add(RawTimer(TimerCategory.BEEHIVES, "Beehive", readyAt, targetMs, "🐝"))
            } else if (flowers.isNotEmpty()) {
                // Project forward through attached flowers deterministically
                var projHoney = produced
                var finishAt: Long? = null

                for (flower in flowers) {
                    val start = maxOf(updatedAt, flower.attachedAt)
                    val end = flower.attachedUntil
                    if (end > start) {
                        val gain = (end - start) * flower.rate
                        if (projHoney + gain >= targetMs) {
                            val needed = targetMs - projHoney
                            val durationNeeded = (needed / flower.rate).toLong()
                            finishAt = start + durationNeeded
                            break
                        }
                        projHoney += gain
                    }
                }

                if (finishAt != null) {
                    out.add(RawTimer(TimerCategory.BEEHIVES, "Beehive", finishAt, targetMs, "🐝"))
                }
            }
        }
    }

    // 7. Resources (Trees, Stones, Iron, Gold, Crimstone, Sunstone, Oil)
    private fun extractResources(farm: JsonObject, out: MutableList<RawTimer>) {
        // Trees
        val treeWindows = getTreeWindows(farm)
        val trees = farm.getAsJsonObjectSafe("trees")
        trees?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                val wood = obj.getAsJsonObjectSafe("wood")
                if (wood != null && wood.has("choppedAt")) {
                    val choppedAt = wood.get("choppedAt").asLong
                    val dur = wood.getEffectiveDurationMs(TREE_DURATION_MS)
                    val readyAt = computeWindowedReadyAt(choppedAt, dur, treeWindows)
                    out.add(RawTimer(TimerCategory.RESOURCES, "Wood (Tree)", readyAt, dur, "🪓"))
                }
            }
        }

        // Stones
        val stoneWindows = getMineWindows(farm, "Stone")
        val stones = farm.getAsJsonObjectSafe("stones")
        stones?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val stone = elem.asJsonObject.getAsJsonObjectSafe("stone")
                if (stone != null && stone.has("minedAt")) {
                    val minedAt = stone.get("minedAt").asLong
                    val dur = stone.getEffectiveDurationMs(STONE_DURATION_MS)
                    val readyAt = computeWindowedReadyAt(minedAt, dur, stoneWindows)
                    out.add(RawTimer(TimerCategory.RESOURCES, "Stone", readyAt, dur, "🪨"))
                }
            }
        }

        // Iron
        val ironWindows = getMineWindows(farm, "Iron")
        val iron = farm.getAsJsonObjectSafe("iron")
        iron?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val stone = elem.asJsonObject.getAsJsonObjectSafe("stone")
                if (stone != null && stone.has("minedAt")) {
                    val minedAt = stone.get("minedAt").asLong
                    val dur = stone.getEffectiveDurationMs(IRON_DURATION_MS)
                    val readyAt = computeWindowedReadyAt(minedAt, dur, ironWindows)
                    out.add(RawTimer(TimerCategory.RESOURCES, "Iron", readyAt, dur, "⛏️"))
                }
            }
        }

        // Gold
        val goldWindows = getMineWindows(farm, "Gold")
        val gold = farm.getAsJsonObjectSafe("gold")
        gold?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val stone = elem.asJsonObject.getAsJsonObjectSafe("stone")
                if (stone != null && stone.has("minedAt")) {
                    val minedAt = stone.get("minedAt").asLong
                    val dur = stone.getEffectiveDurationMs(GOLD_DURATION_MS)
                    val readyAt = computeWindowedReadyAt(minedAt, dur, goldWindows)
                    out.add(RawTimer(TimerCategory.RESOURCES, "Gold", readyAt, dur, "🪙"))
                }
            }
        }

        // Crimstone
        val crimstoneWindows = getMineWindows(farm, "Crimstone")
        val crimstones = farm.getAsJsonObjectSafe("crimstones")
        crimstones?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val stone = elem.asJsonObject.getAsJsonObjectSafe("stone")
                if (stone != null && stone.has("minedAt")) {
                    val minedAt = stone.get("minedAt").asLong
                    val dur = stone.getEffectiveDurationMs(CRIMSTONE_DURATION_MS)
                    val readyAt = computeWindowedReadyAt(minedAt, dur, crimstoneWindows)
                    out.add(RawTimer(TimerCategory.RESOURCES, "Crimstone", readyAt, dur, "💎"))
                }
            }
        }

        // Sunstone
        val sunstones = farm.getAsJsonObjectSafe("sunstones")
        sunstones?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val stone = elem.asJsonObject.getAsJsonObjectSafe("stone")
                if (stone != null && stone.has("minedAt")) {
                    val minedAt = stone.get("minedAt").asLong
                    val dur = stone.getEffectiveDurationMs(SUNSTONE_DURATION_MS)
                    val readyAt = computeWindowedReadyAt(minedAt, dur, emptyList())
                    out.add(RawTimer(TimerCategory.RESOURCES, "Sunstone", readyAt, dur, "☀️"))
                }
            }
        }

        // Oil Reserves
        val oilWindows = getOilWindows(farm)
        val oil = farm.getAsJsonObjectSafe("oilReserves")
        oil?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                val oilData = obj.getAsJsonObjectSafe("oil")
                if (oilData != null && oilData.has("drilledAt")) {
                    val drilledAt = oilData.get("drilledAt").asLong
                    val dur = oilData.getEffectiveDurationMs(OIL_DURATION_MS)
                    val readyAt = computeWindowedReadyAt(drilledAt, dur, oilWindows)
                    out.add(RawTimer(TimerCategory.RESOURCES, "Oil Reserve", readyAt, dur, "🛢️"))
                }
            }
        }
    }

    // 8. Lava Pits
    private fun extractLavaPits(farm: JsonObject, out: MutableList<RawTimer>) {
        val pits = farm.getAsJsonObjectSafe("lavaPits") ?: return
        for ((_, elem) in pits.entrySet()) {
            if (!elem.isJsonObject) continue
            val pit = elem.asJsonObject
            val startedAt = pit.getSafeLong("startedAt") ?: continue
            val readyAt = pit.getSafeLong("readyAt") ?: (startedAt + DEFAULT_LAVA_PIT_MS)
            val dur = (readyAt - startedAt).coerceAtLeast(3600_000L)
            out.add(RawTimer(TimerCategory.LAVA_PITS, "Lava Pit", readyAt, dur, "🌋"))
        }
    }

    // 9. Salt
    private fun extractSalt(farm: JsonObject, out: MutableList<RawTimer>) {
        val saltFarm = farm.getAsJsonObjectSafe("saltFarm")
        val nodes = saltFarm?.getAsJsonObjectSafe("nodes") ?: farm.getAsJsonObjectSafe("saltNodes")
        if (nodes != null) {
            for ((_, elem) in nodes.entrySet()) {
                if (!elem.isJsonObject) continue
                val node = elem.asJsonObject
                val nextChargeAt = node.getSafeLong("nextChargeAt")
                if (nextChargeAt != null && nextChargeAt > 0L) {
                    out.add(RawTimer(TimerCategory.SALT, "Salt Node", nextChargeAt, DEFAULT_SALT_NODE_MS, "🧂"))
                }
            }
        }
    }

    // 10. Crab Traps
    private fun extractCrabTraps(farm: JsonObject, out: MutableList<RawTimer>) {
        val traps = farm.getAsJsonObjectSafe("crabTraps")
        if (traps != null) {
            for ((_, elem) in traps.entrySet()) {
                if (!elem.isJsonObject) continue
                val trap = elem.asJsonObject
                val harvestedAt = trap.getSafeLong("harvestedAt") ?: trap.getSafeLong("placedAt") ?: continue
                val dur = trap.getEffectiveDurationMs(DEFAULT_CRAB_TRAP_MS)
                val readyAt = trap.getSafeLong("readyAt") ?: (harvestedAt + dur)
                out.add(RawTimer(TimerCategory.CRAB_TRAPS, "Crab Trap", readyAt, dur, "🦀"))
            }
        }
    }

    // 11. Animals
    private fun extractAnimals(farm: JsonObject, out: MutableList<RawTimer>) {
        // Chickens
        val chickens = farm.getAsJsonObjectSafe("chickens")
        chickens?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val ch = elem.asJsonObject
                if (ch.has("fedAt")) {
                    val fedAt = ch.get("fedAt").asLong
                    out.add(RawTimer(TimerCategory.ANIMALS, "Chicken (Egg)", fedAt + CHICKEN_DURATION_MS, CHICKEN_DURATION_MS, "🐔"))
                }
            }
        }

        // Barn animals (cows, sheep, etc.)
        val animals = farm.getAsJsonObjectSafe("animals")
        animals?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val an = elem.asJsonObject
                val type = an.getSafeString("type") ?: "Animal"
                val awakeAt = an.getSafeLong("awakeAt")
                val asleepAt = an.getSafeLong("asleepAt")
                if (awakeAt != null && awakeAt > 0L) {
                    val dur = if (asleepAt != null && awakeAt > asleepAt) awakeAt - asleepAt else 8 * 3600_000L
                    out.add(RawTimer(TimerCategory.ANIMALS, type, awakeAt, dur, "🐮"))
                }
            }
        }
    }

    // 12. Petting
    private fun extractPetting(farm: JsonObject, out: MutableList<RawTimer>) {
        val animals = farm.getAsJsonObjectSafe("animals")
        animals?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val an = elem.asJsonObject
                val type = an.getSafeString("type") ?: "Animal"
                val lovedAt = an.getSafeLong("lovedAt")
                if (lovedAt != null && lovedAt > 0L) {
                    val loveCooldown = 24 * 3600_000L
                    val readyAt = lovedAt + loveCooldown
                    out.add(RawTimer(TimerCategory.PETTING, "Pet $type", readyAt, loveCooldown, "✋"))
                }
            }
        }

        // Hen House / Barn love
        val henHouse = farm.getAsJsonObjectSafe("henHouse")
        val henLoved = henHouse?.getSafeLong("lovedAt")
        if (henLoved != null && henLoved > 0L) {
            val cooldown = 24 * 3600_000L
            out.add(RawTimer(TimerCategory.PETTING, "Love Hen House", henLoved + cooldown, cooldown, "🐔"))
        }

        val barn = farm.getAsJsonObjectSafe("barn")
        val barnLoved = barn?.getSafeLong("lovedAt")
        if (barnLoved != null && barnLoved > 0L) {
            val cooldown = 24 * 3600_000L
            out.add(RawTimer(TimerCategory.PETTING, "Love Barn", barnLoved + cooldown, cooldown, "🐮"))
        }
    }

    // 13-18. Cooking Buildings: Fire Pit, Bakery, Deli, Smoothie Shack, Kitchen, Fish Market
    private fun extractCookingBuildings(farm: JsonObject, out: MutableList<RawTimer>) {
        val buildings = farm.getAsJsonObjectSafe("buildings") ?: return

        val buildingCategoryMap = mapOf(
            "Fire Pit" to Pair(TimerCategory.FIRE_PIT, "🔥"),
            "Bakery" to Pair(TimerCategory.BAKERY, "🥧"),
            "Deli" to Pair(TimerCategory.DELI, "🥗"),
            "Smoothie Shack" to Pair(TimerCategory.SMOOTHIE_SHACK, "🥤"),
            "Kitchen" to Pair(TimerCategory.KITCHEN, "🍳"),
            "Fish Market" to Pair(TimerCategory.FISH_MARKET, "🐟")
        )

        for ((bName, catInfo) in buildingCategoryMap) {
            val arr = buildings.getAsJsonArraySafe(bName) ?: continue
            for (i in 0 until arr.size()) {
                val bElem = arr.get(i)
                if (!bElem.isJsonObject) continue
                val bObj = bElem.asJsonObject

                // Crafting queue
                val queue = bObj.getAsJsonArraySafe("crafting")
                if (queue != null) {
                    for (j in 0 until queue.size()) {
                        val craftElem = queue.get(j)
                        if (!craftElem.isJsonObject) continue
                        val craft = craftElem.asJsonObject
                        val name = craft.getSafeString("name") ?: bName
                        val readyAt = craft.getSafeLong("readyAt") ?: continue
                        val amount = craft.getSafeLong("amount") ?: 1L
                        val label = if (amount > 1) "$name (x$amount)" else name
                        out.add(RawTimer(catInfo.first, label, readyAt, 3600_000L, catInfo.second))
                    }
                }
            }
        }
    }

    // 19. Composters
    private fun extractComposters(farm: JsonObject, out: MutableList<RawTimer>) {
        val buildings = farm.getAsJsonObjectSafe("buildings") ?: return
        val composterNames = listOf("Compost Bin", "Turbo Composter", "Premium Composter")

        for (cName in composterNames) {
            val arr = buildings.getAsJsonArraySafe(cName) ?: continue
            for (i in 0 until arr.size()) {
                val bElem = arr.get(i)
                if (!bElem.isJsonObject) continue
                val bObj = bElem.asJsonObject
                val producing = bObj.getAsJsonObjectSafe("producing") ?: continue
                val readyAt = producing.getSafeLong("readyAt") ?: continue
                val startedAt = producing.getSafeLong("startedAt") ?: (readyAt - 4 * 3600_000L)
                val dur = (readyAt - startedAt).coerceAtLeast(60_000L)

                // Exact fertilizer produced by composter type
                val itemsObj = producing.getAsJsonObjectSafe("items")
                val producedItem = itemsObj?.keySet()?.firstOrNull() ?: when (cName) {
                    "Compost Bin" -> "Sprout Mix"
                    "Turbo Composter" -> "Fruitful Blend"
                    "Premium Composter" -> "Rapid Root"
                    else -> cName
                }

                out.add(RawTimer(TimerCategory.COMPOSTERS, producedItem, readyAt, dur, "🪱"))
            }
        }
    }

    // 20. Cave
    private fun extractCave(farm: JsonObject, out: MutableList<RawTimer>) {
        val cave = farm.getAsJsonObjectSafe("cave") ?: return
        val batch = cave.getAsJsonObjectSafe("batch")
        if (batch != null) {
            val readyAt = batch.getSafeLong("readyAt")
            if (readyAt != null && readyAt > 0L) {
                val dur = batch.getEffectiveDurationMs(8 * 3600_000L)
                out.add(RawTimer(TimerCategory.CAVE, "Myco Bin Batch", readyAt, dur, "⛏️"))
            }
        }
    }

    // 21-23. Aging Shed: Aging Rack, Fermentation Rack, Spice Rack
    private fun extractAgingShed(farm: JsonObject, out: MutableList<RawTimer>) {
        val agingShed = farm.getAsJsonObjectSafe("agingShed") ?: return
        val racks = agingShed.getAsJsonObjectSafe("racks") ?: return

        // Aging Rack
        val aging = racks.getAsJsonArraySafe("aging")
        if (aging != null) {
            for (i in 0 until aging.size()) {
                val slot = aging.get(i)
                if (!slot.isJsonObject) continue
                val slotObj = slot.asJsonObject
                val fish = slotObj.getSafeString("fish") ?: "Fish"
                val readyAt = slotObj.getSafeLong("readyAt") ?: continue
                out.add(RawTimer(TimerCategory.AGING_RACK, "Aged $fish", readyAt, 24 * 3600_000L, "🧀"))
            }
        }

        // Fermentation Rack
        val fermentation = racks.getAsJsonArraySafe("fermentation")
        if (fermentation != null) {
            for (i in 0 until fermentation.size()) {
                val slot = fermentation.get(i)
                if (!slot.isJsonObject) continue
                val slotObj = slot.asJsonObject
                val name = slotObj.getSafeString("name") ?: "Fermenting Item"
                val readyAt = slotObj.getSafeLong("readyAt") ?: continue
                out.add(RawTimer(TimerCategory.FERMENTATION_RACK, name, readyAt, 12 * 3600_000L, "🫙"))
            }
        }

        // Spice Rack
        val spice = racks.getAsJsonArraySafe("spice")
        if (spice != null) {
            for (i in 0 until spice.size()) {
                val slot = spice.get(i)
                if (!slot.isJsonObject) continue
                val slotObj = slot.asJsonObject
                val name = slotObj.getSafeString("name") ?: "Spice"
                val readyAt = slotObj.getSafeLong("readyAt") ?: continue
                out.add(RawTimer(TimerCategory.SPICE_RACK, name, readyAt, 6 * 3600_000L, "🌶️"))
            }
        }
    }

    // 24. Crafting Box
    private fun extractCraftingBox(farm: JsonObject, out: MutableList<RawTimer>) {
        val buildings = farm.getAsJsonObjectSafe("buildings")
        val boxArr = buildings?.getAsJsonArraySafe("Crafting Box")
        val boxObj = if (boxArr != null && boxArr.size() > 0 && boxArr.get(0).isJsonObject) {
            boxArr.get(0).asJsonObject
        } else {
            farm.getAsJsonObjectSafe("craftingBox")
        } ?: return

        val queue = boxObj.getAsJsonArraySafe("queue") ?: boxObj.getAsJsonArraySafe("crafting") ?: return
        for (i in 0 until queue.size()) {
            val itemElem = queue.get(i)
            if (!itemElem.isJsonObject) continue
            val itemObj = itemElem.asJsonObject
            val name = itemObj.getSafeString("name") ?: "Crafting Item"
            val readyAt = itemObj.getSafeLong("readyAt") ?: continue
            val dur = itemObj.getEffectiveDurationMs(3600_000L)
            out.add(RawTimer(TimerCategory.CRAFTING_BOX, name, readyAt, dur, "📦"))
        }
    }

    // 25. Love Island
    private fun extractLoveIsland(farm: JsonObject, out: MutableList<RawTimer>) {
        val island = farm.getAsJsonObjectSafe("floatingIsland") ?: farm.getAsJsonObjectSafe("loveIsland") ?: return
        val now = System.currentTimeMillis()

        // 1. Process official event flight schedule
        val schedule = island.getAsJsonArraySafe("schedule")
        if (schedule != null && schedule.size() > 0) {
            var addedAny = false
            for (elem in schedule) {
                if (!elem.isJsonObject) continue
                val sObj = elem.asJsonObject
                val startAt = sObj.getSafeLong("startAt") ?: continue
                val endAt = sObj.getSafeLong("endAt") ?: (startAt + 30 * 60_000L)
                val duration = if (endAt > startAt) (endAt - startAt) else (30 * 60_000L)

                // Only schedule events that are currently live or upcoming
                if (endAt > now) {
                    out.add(RawTimer(TimerCategory.LOVE_ISLAND, "Floating Island", startAt, duration, "❤️"))
                    addedAny = true
                }
            }
            if (addedAny) return
        }

        // 2. Fallback for alternate schemas
        val resetAt = island.getSafeLong("nextResetAt") 
            ?: island.getSafeLong("readyAt") 
            ?: island.getSafeLong("startAt")
        if (resetAt != null && resetAt > 0L) {
            out.add(RawTimer(TimerCategory.LOVE_ISLAND, "Floating Island", resetAt, 30 * 60_000L, "❤️"))
        }
    }

    // 26. Bud Box
    private fun extractBudBox(farm: JsonObject, out: MutableList<RawTimer>) {
        val plaza = farm.getAsJsonObjectSafe("pumpkinPlaza")
        val budBox = plaza?.getAsJsonObjectSafe("budBox") ?: farm.getAsJsonObjectSafe("budBox")
        if (budBox != null) {
            val openedAt = budBox.getSafeLong("openedAt")
            val nextMidnight = getNextUtcMidnight()
            if (openedAt != null && openedAt > 0L) {
                out.add(RawTimer(TimerCategory.BUD_BOX, "Daily Bud Box", nextMidnight, 24 * 3600_000L, "🎁"))
            }
        }
    }

    // 27. Power Skills
    private fun extractPowerSkills(farm: JsonObject, out: MutableList<RawTimer>) {
        val bumpkin = farm.getAsJsonObjectSafe("bumpkin") ?: return
        val previousPowerUseAt = bumpkin.getAsJsonObjectSafe("previousPowerUseAt") ?: return

        for ((skillName, elem) in previousPowerUseAt.entrySet()) {
            if (elem.isJsonPrimitive && elem.asJsonPrimitive.isNumber) {
                val usedAt = elem.asLong
                val cooldown = POWER_SKILL_COOLDOWNS[skillName] ?: (24 * 3600_000L)
                val readyAt = usedAt + cooldown
                out.add(RawTimer(TimerCategory.POWER_SKILLS, skillName, readyAt, cooldown, "⚡"))
            }
        }
    }

    // 28. Quests
    private fun extractQuests(farm: JsonObject, out: MutableList<RawTimer>) {
        val telegram = farm.getAsJsonObjectSafe("telegram")
        val quest = telegram?.getAsJsonObjectSafe("quest")
        if (quest != null) {
            val startAt = quest.getSafeLong("startAt")
            val rawName = quest.getSafeString("name") ?: "Daily Quest"
            val friendlyName = rawName.split("-", "_", " ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                .ifBlank { "Daily Quest" }
            if (startAt != null && startAt > 0L) {
                out.add(RawTimer(TimerCategory.QUESTS, friendlyName, startAt, 24 * 3600_000L, "📜"))
            }
        }
    }

    // 29. Village Projects
    private fun extractVillageProjects(farm: JsonObject, out: MutableList<RawTimer>) {
        val village = farm.getAsJsonObjectSafe("villageProjects") ?: farm.getAsJsonObjectSafe("island")
        if (village != null) {
            val projects = village.getAsJsonObjectSafe("projects")
            projects?.entrySet()?.forEach { (projName, elem) ->
                if (elem.isJsonObject) {
                    val pObj = elem.asJsonObject
                    val readyAt = pObj.getSafeLong("readyAt") ?: pObj.getSafeLong("completedAt")
                    if (readyAt != null && readyAt > 0L) {
                        out.add(RawTimer(TimerCategory.VILLAGE_PROJECTS, projName, readyAt, 24 * 3600_000L, "🏛️"))
                    }
                }
            }
        }
    }

    private fun getNextUtcMidnight(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun clusterTimers(raw: List<RawTimer>): List<FarmTimerItem> {
        val result = mutableListOf<FarmTimerItem>()
        val now = System.currentTimeMillis()

        // Group by category and itemName
        val grouped = raw.groupBy { "${it.category.key}_${it.itemName}" }

        for ((_, items) in grouped) {
            // 1. Separate items that are ALREADY ready (readyAt <= now) vs items still in progress (readyAt > now)
            val readyItems = items.filter { it.readyAt <= now }
            val pendingItems = items.filter { it.readyAt > now }

            // All ready items of the exact same resource are merged into a single cluster (e.g. Iron (x11) Ready)
            if (readyItems.isNotEmpty()) {
                result.add(createTimerItem(readyItems))
            }

            // Pending items are clustered if their readyAt is within 90 seconds of each other
            val sortedPending = pendingItems.sortedBy { it.readyAt }
            var currentCluster = mutableListOf<RawTimer>()

            for (item in sortedPending) {
                if (currentCluster.isEmpty()) {
                    currentCluster.add(item)
                } else {
                    val first = currentCluster.first()
                    // Cluster if within 90 seconds (90,000 ms)
                    if (abs(item.readyAt - first.readyAt) <= 90_000L) {
                        currentCluster.add(item)
                    } else {
                        result.add(createTimerItem(currentCluster))
                        currentCluster = mutableListOf(item)
                    }
                }
            }

            if (currentCluster.isNotEmpty()) {
                result.add(createTimerItem(currentCluster))
            }
        }

        return result.sorted()
    }

    private fun createTimerItem(cluster: List<RawTimer>): FarmTimerItem {
        val representative = cluster.first()
        val count = cluster.size
        val title = if (representative.category == TimerCategory.LOVE_ISLAND ||
            representative.category == TimerCategory.QUESTS ||
            representative.category == TimerCategory.BUD_BOX
        ) {
            representative.itemName
        } else {
            "${representative.itemName} (x$count)"
        }

        val body = when (representative.category) {
            TimerCategory.LOVE_ISLAND -> "بالن جزیره شناور (Love Island) در دسترس است! ❤️"
            TimerCategory.QUESTS -> "دیلی کوئست روزانه تلگرام آماده شد! 📜"
            TimerCategory.BUD_BOX -> "جعبه جوانه روزانه (Bud Box) آماده باز شدن است! 🎁"
            else -> if (count > 1) {
                "$count items in ${representative.category.displayName} are ready!"
            } else {
                "1 item in ${representative.category.displayName} is ready to collect!"
            }
        }

        val id = "${representative.category.key}_${representative.itemName}_${representative.readyAt}".hashCode().toString()

        return FarmTimerItem(
            id = id,
            title = title,
            body = body,
            category = representative.category,
            readyAt = representative.readyAt,
            totalDurationMs = representative.durationMs,
            count = count,
            iconEmoji = representative.icon,
            specificIconResId = resolveItemIcon(representative.itemName, representative.category)
        )
    }

    private fun JsonObject.getAsJsonObjectSafe(key: String): JsonObject? {
        return if (has(key) && get(key).isJsonObject) getAsJsonObject(key) else null
    }

    /**
     * Reads the resource duration from the farm object.
     * The game recently renamed `duration` to `baseDurationMs` and added
     * `boostedTime` (ms shaved off the base duration by boosts).
     */
    private fun JsonObject.getEffectiveDurationMs(fallbackMs: Long): Long {
        val base = getSafeLong("baseDurationMs")
            ?: getSafeLong("duration")
            ?: return fallbackMs
        val boosted = getSafeLong("boostedTime") ?: 0L
        return (base - boosted).coerceAtLeast(0L)
    }

    // ---- Boost speed-window model (ported from the game's boostWindows.ts) ----

    private data class BoostWindow(val from: Long, val to: Long, val speed: Double)

    private const val MAX_SAFE_MS = 9007199254740991L

    /** Cooldown (ms) during which a placed temporary collectible stays active. */
    private val COLLECTIBLE_EXPIRY_MS: Map<String, Long> = mapOf(
        "Time Warp Totem" to 4 * 3600_000L,
        "Super Totem" to 7 * 24 * 3600_000L,
        "Harvest Hourglass" to 9 * 3600_000L,
        "Timber Hourglass" to 4 * 3600_000L,
        "Ore Hourglass" to 5 * 3600_000L,
        "Orchard Hourglass" to 8 * 3600_000L,
        "Blossom Hourglass" to 12 * 3600_000L,
        "Sparrow Shrine" to 7 * 24 * 3600_000L,
        "Tortoise Shrine" to 7 * 24 * 3600_000L,
        "Toucan Shrine" to 7 * 24 * 3600_000L,
        "Moth Shrine" to 7 * 24 * 3600_000L,
        "Mole Shrine" to 7 * 24 * 3600_000L,
        "Badger Shrine" to 7 * 24 * 3600_000L,
        "Stag Shrine" to 7 * 24 * 3600_000L
    )

    /**
     * Windows of one temporary collectible: live placements (farm.collectibles,
     * expiry = createdAt + cooldown) unioned with finalised intervals in
     * farm.boostHistory, then merged (a boost never stacks with itself).
     */
    private fun getBoostWindows(farm: JsonObject, name: String, speed: Double): List<BoostWindow> {
        val windows = mutableListOf<BoostWindow>()
        val placements = farm.getAsJsonObjectSafe("collectibles")?.getAsJsonArraySafe(name)
        if (placements != null) {
            for (i in 0 until placements.size()) {
                val el = placements.get(i)
                if (!el.isJsonObject) continue
                val placed = el.asJsonObject
                val createdAt = placed.getSafeLong("createdAt") ?: continue
                val cooldown = COLLECTIBLE_EXPIRY_MS[name]
                val expiry = if (cooldown != null) {
                    createdAt + cooldown + (placed.getSafeLong("extendedMs") ?: 0L)
                } else MAX_SAFE_MS
                val removedAt = placed.getSafeLong("removedAt") ?: MAX_SAFE_MS
                val to = minOf(expiry, removedAt)
                if (to > createdAt) windows.add(BoostWindow(createdAt, to, speed))
            }
        }
        val history = farm.getAsJsonObjectSafe("boostHistory")?.getAsJsonArraySafe(name)
        if (history != null) {
            for (i in 0 until history.size()) {
                val el = history.get(i)
                if (!el.isJsonObject) continue
                val w = el.asJsonObject
                val from = w.getSafeLong("from") ?: continue
                val to = w.getSafeLong("to") ?: continue
                if (to > from) windows.add(BoostWindow(from, to, speed))
            }
        }
        return mergeWindows(windows)
    }

    /** Sort + coalesce overlapping/adjacent windows of the same speed. */
    private fun mergeWindows(windows: List<BoostWindow>): List<BoostWindow> {
        if (windows.isEmpty()) return emptyList()
        val out = mutableListOf<BoostWindow>()
        for (w in windows.sortedWith(compareBy({ it.from }, { it.to }))) {
            val last = out.lastOrNull()
            if (last != null && w.speed == last.speed && w.from <= last.to) {
                if (w.to > last.to) out[out.size - 1] = last.copy(to = w.to)
            } else {
                out.add(w)
            }
        }
        return out
    }

    /** Super & Time Warp Totems are both 2x and must not stack with each other. */
    private fun getMergedTotemWindows(farm: JsonObject, speed: Double): List<BoostWindow> =
        mergeWindows(getBoostWindows(farm, "Super Totem", speed) + getBoostWindows(farm, "Time Warp Totem", speed))

    /** Power hour lives in farm.buffs (active) + boostHistory (finished). */
    private fun getPowerHourWindows(farm: JsonObject): List<BoostWindow> {
        val windows = mutableListOf<BoostWindow>()
        val buff = farm.getAsJsonObjectSafe("buffs")?.getAsJsonObjectSafe("Power hour")
        val startedAt = buff?.getSafeLong("startedAt")
        if (startedAt != null) {
            val to = startedAt + (buff.getSafeLong("durationMS") ?: 0L)
            if (to > startedAt) windows.add(BoostWindow(startedAt, to, 2.0))
        }
        val history = farm.getAsJsonObjectSafe("boostHistory")?.getAsJsonArraySafe("Power hour")
        if (history != null) {
            for (i in 0 until history.size()) {
                val el = history.get(i)
                if (!el.isJsonObject) continue
                val w = el.asJsonObject
                val from = w.getSafeLong("from") ?: continue
                val to = w.getSafeLong("to") ?: continue
                if (to > from) windows.add(BoostWindow(from, to, 2.0))
            }
        }
        return mergeWindows(windows)
    }

    /** Sunshower is boostHistory-only. */
    private fun getSunshowerWindows(farm: JsonObject): List<BoostWindow> {
        val history = farm.getAsJsonObjectSafe("boostHistory")?.getAsJsonArraySafe("Sunshower") ?: return emptyList()
        val out = mutableListOf<BoostWindow>()
        for (i in 0 until history.size()) {
            val el = history.get(i)
            if (!el.isJsonObject) continue
            val w = el.asJsonObject
            val from = w.getSafeLong("from") ?: continue
            val to = w.getSafeLong("to") ?: continue
            if (to > from) out.add(BoostWindow(from, to, 2.0))
        }
        return out
    }

    private fun getCropPlotWindows(farm: JsonObject): List<BoostWindow> =
        getBoostWindows(farm, "Sparrow Shrine", 1.35) +
            getBoostWindows(farm, "Harvest Hourglass", 1.35) +
            getMergedTotemWindows(farm, 2.0) +
            getPowerHourWindows(farm) +
            getSunshowerWindows(farm)

    private fun getTreeWindows(farm: JsonObject): List<BoostWindow> =
        getMergedTotemWindows(farm, 2.0) +
            getBoostWindows(farm, "Timber Hourglass", 1.35) +
            getBoostWindows(farm, "Badger Shrine", 1.35)

    /** Mines: stone gets Badger, iron/gold get Mole, crimstone Mole-only, sunstone none. */
    private fun getMineWindows(farm: JsonObject, resource: String): List<BoostWindow> = when (resource) {
        "Stone" -> getMergedTotemWindows(farm, 2.0) +
            getBoostWindows(farm, "Ore Hourglass", 2.0) +
            getBoostWindows(farm, "Badger Shrine", 1.35)
        "Iron", "Gold" -> getMergedTotemWindows(farm, 2.0) +
            getBoostWindows(farm, "Ore Hourglass", 2.0) +
            getBoostWindows(farm, "Mole Shrine", 1.35)
        "Crimstone" -> getBoostWindows(farm, "Mole Shrine", 1.35)
        else -> emptyList() // Sunstone / Ascension Crystal: no temporary boost
    }

    private fun getOilWindows(farm: JsonObject): List<BoostWindow> =
        getBoostWindows(farm, "Stag Shrine", 1.35)

    private fun getFruitPatchWindows(farm: JsonObject): List<BoostWindow> =
        getMergedTotemWindows(farm, 2.0) +
            getBoostWindows(farm, "Orchard Hourglass", 1.35) +
            getBoostWindows(farm, "Toucan Shrine", 1.35)

    private fun getGreenhouseWindows(farm: JsonObject, plantName: String?): List<BoostWindow> {
        val shared = getMergedTotemWindows(farm, 2.0) +
            getBoostWindows(farm, "Tortoise Shrine", 1.5)
        val hourglass = if (plantName == "Grape") "Orchard Hourglass" else "Harvest Hourglass"
        return shared + getBoostWindows(farm, hourglass, 1.35)
    }

    private fun getFlowerBedWindows(farm: JsonObject): List<BoostWindow> =
        getBoostWindows(farm, "Blossom Hourglass", 1.35) +
            getBoostWindows(farm, "Moth Shrine", 1.35)

    /**
     * Fertiliser speed window on a plot/patch/pot: [fertilisedAt, inf) with the
     * grow-TIME multiplier. Open-ended — it stays active until harvest.
     */
    private fun getFertiliserWindow(holder: JsonObject, vararg speeds: Pair<String, Double>): List<BoostWindow> {
        val fert = holder.getAsJsonObjectSafe("fertiliser") ?: return emptyList()
        val name = fert.getSafeString("name") ?: return emptyList()
        val speed = speeds.firstOrNull { it.first == name }?.second ?: return emptyList()
        val fertilisedAt = fert.getSafeLong("fertilisedAt") ?: return emptyList()
        return listOf(BoostWindow(fertilisedAt, MAX_SAFE_MS, speed))
    }

    /**
     * Port of the game's computeReadyAt (boostWindows.ts): work accrues at the
     * effective speed — the PRODUCT of every covering window, so overlapping
     * boosts stack multiplicatively. Ready when accrued work reaches the
     * (already boosted-time-adjusted) duration.
     */
    private fun computeWindowedReadyAt(startedAt: Long, baseDurationMs: Long, windows: List<BoostWindow>): Long {
        if (baseDurationMs <= 0L) return startedAt
        val clipped = windows.asSequence()
            .map { it.copy(from = maxOf(it.from, startedAt)) }
            .filter { it.to > it.from }
            .toList()
        if (clipped.isEmpty()) return startedAt + baseDurationMs
        val bounds = sortedSetOf(startedAt)
        clipped.forEach {
            bounds.add(it.from)
            bounds.add(it.to)
        }
        val ordered = bounds.toList()
        var accumulated = 0.0
        for (i in 0 until ordered.size - 1) {
            val from = ordered[i]
            val to = ordered[i + 1]
            val mid = (from + to) / 2.0
            var speed = 1.0
            for (w in clipped) {
                if (w.from.toDouble() <= mid && mid < w.to.toDouble()) speed *= w.speed
            }
            val segmentWork = (to - from) * speed
            if (accumulated + segmentWork >= baseDurationMs) {
                return from + ((baseDurationMs - accumulated) / speed).toLong()
            }
            accumulated += segmentWork
        }
        // Tail beyond the final boundary runs at 1x.
        return ordered.last() + (baseDurationMs - accumulated).toLong()
    }

    private fun JsonObject.getAsJsonArraySafe(key: String): JsonArray? {
        return if (has(key) && get(key).isJsonArray) getAsJsonArray(key) else null
    }

    private fun JsonObject.getSafeString(key: String): String? {
        return if (has(key) && !get(key).isJsonNull) get(key).asString else null
    }

    private fun JsonObject.getSafeLong(key: String): Long? {
        return if (has(key) && !get(key).isJsonNull) {
            try { get(key).asLong } catch (e: Exception) { null }
        } else null
    }

    private fun JsonObject.getSafeDouble(key: String): Double? {
        return if (has(key) && !get(key).isJsonNull) {
            try { get(key).asDouble } catch (e: Exception) { null }
        } else null
    }

    private data class AttachedFlower(
        val attachedAt: Long,
        val attachedUntil: Long,
        val rate: Double
    )
}
