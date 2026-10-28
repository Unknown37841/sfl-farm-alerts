package com.sfl.notifapp.data.engine

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.model.TimerCategory
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
        "Banana" to 12 * 3600_000L
    )

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
        for ((_, elem) in crops.entrySet()) {
            if (!elem.isJsonObject) continue
            val plot = elem.asJsonObject
            val crop = plot.getAsJsonObjectSafe("crop") ?: continue
            if (!crop.has("plantedAt")) continue

            val plantedAt = crop.get("plantedAt").asLong
            val name = crop.getSafeString("name") ?: "Crop"
            val dur = crop.getSafeLong("boostedTime") ?: CROP_DURATIONS[name] ?: 3600_000L
            val readyAt = crop.getSafeLong("readyAt") ?: (plantedAt + dur)

            out.add(RawTimer(TimerCategory.CROPS, name, readyAt, dur, "🌾"))
        }
    }

    // 2. Fruit Patches
    private fun extractFruits(farm: JsonObject, out: MutableList<RawTimer>) {
        val fruits = farm.getAsJsonObjectSafe("fruitPatches") ?: return
        for ((_, elem) in fruits.entrySet()) {
            if (!elem.isJsonObject) continue
            val patch = elem.asJsonObject
            val fruit = patch.getAsJsonObjectSafe("fruit") ?: continue
            if (!fruit.has("plantedAt")) continue

            val plantedAt = fruit.get("plantedAt").asLong
            val name = fruit.getSafeString("name") ?: "Fruit"
            val dur = fruit.getSafeLong("boostedTime") ?: FRUIT_DURATIONS[name] ?: (4 * 3600_000L)
            val readyAt = fruit.getSafeLong("readyAt") ?: (plantedAt + dur)

            out.add(RawTimer(TimerCategory.FRUIT_PATCHES, name, readyAt, dur, "🍊"))
        }
    }

    // 3. Greenhouse
    private fun extractGreenhouse(farm: JsonObject, out: MutableList<RawTimer>) {
        val gh = farm.getAsJsonObjectSafe("greenhouse") ?: return
        val pots = gh.getAsJsonObjectSafe("pots") ?: return
        for ((_, elem) in pots.entrySet()) {
            if (!elem.isJsonObject) continue
            val plant = elem.asJsonObject.getAsJsonObjectSafe("plant") ?: continue
            if (plant.has("plantedAt")) {
                val plantedAt = plant.get("plantedAt").asLong
                val name = plant.getSafeString("name") ?: "Greenhouse Plant"
                val dur = plant.getSafeLong("boostedTime") ?: (12 * 3600_000L)
                val readyAt = plant.getSafeLong("readyAt") ?: (plantedAt + dur)
                out.add(RawTimer(TimerCategory.GREENHOUSE, name, readyAt, dur, "🏡"))
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
                val dur = plant.getSafeLong("boostedTime") ?: (24 * 3600_000L)
                val readyAt = plant.getSafeLong("readyAt") ?: (plantedAt + dur)
                out.add(RawTimer(TimerCategory.FLOWERS, name, readyAt, dur, "🌸"))
            }
        }
    }

    // 6. Beehives
    private fun extractBeehives(farm: JsonObject, out: MutableList<RawTimer>) {
        val hives = farm.getAsJsonObjectSafe("beehives") ?: return
        for ((hiveId, elem) in hives.entrySet()) {
            if (!elem.isJsonObject) continue
            val hive = elem.asJsonObject
            val honey = hive.getAsJsonObjectSafe("honey")
            val updatedAt = honey?.getSafeLong("updatedAt") ?: hive.getSafeLong("updatedAt")
            val produced = honey?.getSafeLong("produced") ?: 0L

            if (updatedAt != null && updatedAt > 0L) {
                val targetMs = 24 * 3600_000L
                val remainingMs = (targetMs - produced).coerceAtLeast(0L)
                val readyAt = System.currentTimeMillis() + remainingMs
                out.add(RawTimer(TimerCategory.BEEHIVES, "Beehive #$hiveId", readyAt, targetMs, "🐝"))
            }
        }
    }

    // 7. Resources (Trees, Stones, Iron, Gold, Crimstone, Sunstone, Oil)
    private fun extractResources(farm: JsonObject, out: MutableList<RawTimer>) {
        // Trees
        val trees = farm.getAsJsonObjectSafe("trees")
        trees?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                val wood = obj.getAsJsonObjectSafe("wood")
                if (wood != null && wood.has("choppedAt")) {
                    val choppedAt = wood.get("choppedAt").asLong
                    val dur = wood.getSafeLong("duration") ?: TREE_DURATION_MS
                    out.add(RawTimer(TimerCategory.RESOURCES, "Wood (Tree)", choppedAt + dur, dur, "🪓"))
                }
            }
        }

        // Stones
        val stones = farm.getAsJsonObjectSafe("stones")
        stones?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val stone = elem.asJsonObject.getAsJsonObjectSafe("stone")
                if (stone != null && stone.has("minedAt")) {
                    val minedAt = stone.get("minedAt").asLong
                    val dur = stone.getSafeLong("duration") ?: STONE_DURATION_MS
                    out.add(RawTimer(TimerCategory.RESOURCES, "Stone", minedAt + dur, dur, "🪨"))
                }
            }
        }

        // Iron
        val iron = farm.getAsJsonObjectSafe("iron")
        iron?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val stone = elem.asJsonObject.getAsJsonObjectSafe("stone")
                if (stone != null && stone.has("minedAt")) {
                    val minedAt = stone.get("minedAt").asLong
                    val dur = stone.getSafeLong("duration") ?: IRON_DURATION_MS
                    out.add(RawTimer(TimerCategory.RESOURCES, "Iron", minedAt + dur, dur, "⛏️"))
                }
            }
        }

        // Gold
        val gold = farm.getAsJsonObjectSafe("gold")
        gold?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val stone = elem.asJsonObject.getAsJsonObjectSafe("stone")
                if (stone != null && stone.has("minedAt")) {
                    val minedAt = stone.get("minedAt").asLong
                    val dur = stone.getSafeLong("duration") ?: GOLD_DURATION_MS
                    out.add(RawTimer(TimerCategory.RESOURCES, "Gold", minedAt + dur, dur, "🪙"))
                }
            }
        }

        // Crimstone
        val crimstones = farm.getAsJsonObjectSafe("crimstones")
        crimstones?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val stone = elem.asJsonObject.getAsJsonObjectSafe("stone")
                if (stone != null && stone.has("minedAt")) {
                    val minedAt = stone.get("minedAt").asLong
                    val dur = stone.getSafeLong("duration") ?: CRIMSTONE_DURATION_MS
                    out.add(RawTimer(TimerCategory.RESOURCES, "Crimstone", minedAt + dur, dur, "💎"))
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
                    val dur = stone.getSafeLong("duration") ?: SUNSTONE_DURATION_MS
                    out.add(RawTimer(TimerCategory.RESOURCES, "Sunstone", minedAt + dur, dur, "☀️"))
                }
            }
        }

        // Oil Reserves
        val oil = farm.getAsJsonObjectSafe("oilReserves")
        oil?.entrySet()?.forEach { (_, elem) ->
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                val oilData = obj.getAsJsonObjectSafe("oil")
                if (oilData != null && oilData.has("drilledAt")) {
                    val drilledAt = oilData.get("drilledAt").asLong
                    val dur = oilData.getSafeLong("duration") ?: OIL_DURATION_MS
                    out.add(RawTimer(TimerCategory.RESOURCES, "Oil Reserve", drilledAt + dur, dur, "🛢️"))
                }
            }
        }
    }

    // 8. Lava Pits
    private fun extractLavaPits(farm: JsonObject, out: MutableList<RawTimer>) {
        val pits = farm.getAsJsonObjectSafe("lavaPits") ?: return
        for ((id, elem) in pits.entrySet()) {
            if (!elem.isJsonObject) continue
            val pit = elem.asJsonObject
            val startedAt = pit.getSafeLong("startedAt") ?: continue
            val readyAt = pit.getSafeLong("readyAt") ?: (startedAt + DEFAULT_LAVA_PIT_MS)
            val dur = (readyAt - startedAt).coerceAtLeast(3600_000L)
            out.add(RawTimer(TimerCategory.LAVA_PITS, "Lava Pit #$id", readyAt, dur, "🌋"))
        }
    }

    // 9. Salt
    private fun extractSalt(farm: JsonObject, out: MutableList<RawTimer>) {
        val saltFarm = farm.getAsJsonObjectSafe("saltFarm")
        val nodes = saltFarm?.getAsJsonObjectSafe("nodes") ?: farm.getAsJsonObjectSafe("saltNodes")
        if (nodes != null) {
            for ((id, elem) in nodes.entrySet()) {
                if (!elem.isJsonObject) continue
                val node = elem.asJsonObject
                val nextChargeAt = node.getSafeLong("nextChargeAt")
                if (nextChargeAt != null && nextChargeAt > 0L) {
                    out.add(RawTimer(TimerCategory.SALT, "Salt Node #$id", nextChargeAt, DEFAULT_SALT_NODE_MS, "🧂"))
                }
            }
        }
    }

    // 10. Crab Traps
    private fun extractCrabTraps(farm: JsonObject, out: MutableList<RawTimer>) {
        val traps = farm.getAsJsonObjectSafe("crabTraps")
        if (traps != null) {
            for ((id, elem) in traps.entrySet()) {
                if (!elem.isJsonObject) continue
                val trap = elem.asJsonObject
                val harvestedAt = trap.getSafeLong("harvestedAt") ?: trap.getSafeLong("placedAt") ?: continue
                val dur = trap.getSafeLong("duration") ?: DEFAULT_CRAB_TRAP_MS
                val readyAt = trap.getSafeLong("readyAt") ?: (harvestedAt + dur)
                out.add(RawTimer(TimerCategory.CRAB_TRAPS, "Crab Trap #$id", readyAt, dur, "🦀"))
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
                out.add(RawTimer(TimerCategory.COMPOSTERS, cName, readyAt, dur, "🪱"))
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
                val dur = batch.getSafeLong("duration") ?: (8 * 3600_000L)
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
            val dur = itemObj.getSafeLong("duration") ?: 3600_000L
            out.add(RawTimer(TimerCategory.CRAFTING_BOX, name, readyAt, dur, "📦"))
        }
    }

    // 25. Love Island
    private fun extractLoveIsland(farm: JsonObject, out: MutableList<RawTimer>) {
        val island = farm.getAsJsonObjectSafe("floatingIsland") ?: farm.getAsJsonObjectSafe("loveIsland")
        if (island != null) {
            val resetAt = island.getSafeLong("nextResetAt") ?: island.getSafeLong("readyAt")
            if (resetAt != null && resetAt > 0L) {
                out.add(RawTimer(TimerCategory.LOVE_ISLAND, "Floating Island Event", resetAt, 24 * 3600_000L, "❤️"))
            }
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
            val name = quest.getSafeString("name") ?: "Daily Quest"
            if (startAt != null && startAt > 0L) {
                out.add(RawTimer(TimerCategory.QUESTS, name, startAt, 24 * 3600_000L, "📜"))
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

        // Group by category and itemName
        val grouped = raw.groupBy { "${it.category.key}_${it.itemName}" }

        for ((_, items) in grouped) {
            val sorted = items.sortedBy { it.readyAt }
            var currentCluster = mutableListOf<RawTimer>()

            for (item in sorted) {
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
        val title = if (count > 1) {
            "${representative.itemName} (x$count)"
        } else {
            representative.itemName
        }

        val body = if (count > 1) {
            "$count items in ${representative.category.displayName} are ready!"
        } else {
            "${representative.itemName} is ready to collect!"
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
            iconEmoji = representative.icon
        )
    }

    private fun JsonObject.getAsJsonObjectSafe(key: String): JsonObject? {
        return if (has(key) && get(key).isJsonObject) getAsJsonObject(key) else null
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
}
