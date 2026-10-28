package com.sfl.notifapp.data.engine

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sfl.notifapp.data.model.FarmTimerItem
import com.sfl.notifapp.data.model.TimerCategory
import java.util.UUID
import kotlin.math.abs

object SflTimerEngine {

    // Base durations in milliseconds
    private val CROP_DURATIONS = mapOf(
        "Sunflower" to 60_000L,
        "Potato" to 5 * 60_000L,
        "Pumpkin" to 30 * 60_000L,
        "Carrot" to 60 * 60_000L,
        "Cabbage" to 2 * 3600_000L,
        "Beetroot" to 4 * 3600_000L,
        "Cauliflower" to 8 * 3600_000L,
        "Parsnip" to 12 * 3600_000L,
        "Eggplant" to 16 * 3600_000L,
        "Corn" to 20 * 3600_000L,
        "Radish" to 24 * 3600_000L,
        "Wheat" to 24 * 3600_000L,
        "Barley" to 24 * 3600_000L,
        "Kale" to 36 * 3600_000L,
        "Soybean" to 4 * 3600_000L,
        "Rice" to 4 * 3600_000L,
        "Olive" to 12 * 3600_000L
    )

    private val FRUIT_DURATIONS = mapOf(
        "Apple" to 4 * 3600_000L,
        "Orange" to 8 * 3600_000L,
        "Blueberry" to 6 * 3600_000L,
        "Banana" to 12 * 3600_000L,
        "Tomato" to 2 * 3600_000L,
        "Lemon" to 4 * 3600_000L
    )

    private const val TREE_DURATION_MS = 2 * 3600_000L
    private const val STONE_DURATION_MS = 4 * 3600_000L
    private const val IRON_DURATION_MS = 8 * 3600_000L
    private const val GOLD_DURATION_MS = 24 * 3600_000L
    private const val CRIMSTONE_DURATION_MS = 24 * 3600_000L
    private const val OIL_DURATION_MS = 20 * 3600_000L

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

        // 1. Buildings (Composters & Cooking)
        extractBuildings(farm, rawList)

        // 2. Trees (Wood)
        extractTrees(farm, rawList)

        // 3. Mining (Stones, Iron, Gold, Crimstone)
        extractMining(farm, rawList)

        // 4. Crops & Fruit Patches
        extractCrops(farm, rawList)
        extractFruits(farm, rawList)

        // 5. Oil Reserves
        extractOil(farm, rawList)

        // 6. Beehives
        extractBeehives(farm, rawList)

        // 7. Animals
        extractAnimals(farm, rawList)

        // 8. Greenhouse
        extractGreenhouse(farm, rawList)

        // Cluster raw timers that are within 90 seconds of each other
        return clusterTimers(rawList)
    }

    private fun extractBuildings(farm: JsonObject, out: MutableList<RawTimer>) {
        val buildings = farm.getAsJsonObjectSafe("buildings") ?: return
        for ((bName, bElement) in buildings.entrySet()) {
            if (!bElement.isJsonArray) continue
            val bArray = bElement.asJsonArray
            for (bObj in bArray) {
                if (!bObj.isJsonObject) continue
                val building = bObj.asJsonObject

                // Composter producing
                val producing = building.getAsJsonObjectSafe("producing")
                if (producing != null && producing.has("readyAt")) {
                    val readyAt = producing.get("readyAt").asLong
                    val items = producing.getAsJsonObjectSafe("items")
                    val desc = if (items != null && items.entrySet().isNotEmpty()) {
                        items.entrySet().joinToString(", ") { "${it.value.asString} ${it.key}" }
                    } else {
                        bName
                    }
                    out.add(RawTimer(TimerCategory.COMPOSTERS, "$bName: $desc", readyAt, 0L, "🪱"))
                }

                // Cooking crafting queue
                val crafting = building.getAsJsonArraySafe("crafting")
                if (crafting != null) {
                    for (cItem in crafting) {
                        if (!cItem.isJsonObject) continue
                        val cObj = cItem.asJsonObject
                        if (cObj.has("readyAt")) {
                            val readyAt = cObj.get("readyAt").asLong
                            val name = cObj.getSafeString("name") ?: bName
                            out.add(RawTimer(TimerCategory.COOKING, name, readyAt, 0L, "🍲"))
                        }
                    }
                }
            }
        }
    }

    private fun extractTrees(farm: JsonObject, out: MutableList<RawTimer>) {
        val trees = farm.getAsJsonObjectSafe("trees") ?: return
        for ((_, tElem) in trees.entrySet()) {
            if (!tElem.isJsonObject) continue
            val wood = tElem.asJsonObject.getAsJsonObjectSafe("wood") ?: continue
            if (wood.has("choppedAt")) {
                val choppedAt = wood.get("choppedAt").asLong
                val duration = wood.getSafeLong("boostedTime") ?: TREE_DURATION_MS
                out.add(RawTimer(TimerCategory.TREES, "Wood Tree", choppedAt + duration, duration, "🪓"))
            }
        }
    }

    private fun extractMining(farm: JsonObject, out: MutableList<RawTimer>) {
        // Stones
        farm.getAsJsonObjectSafe("stones")?.let { stones ->
            for ((_, elem) in stones.entrySet()) {
                val st = elem.asJsonObject.getAsJsonObjectSafe("stone") ?: continue
                if (st.has("minedAt")) {
                    val minedAt = st.get("minedAt").asLong
                    val dur = st.getSafeLong("boostedTime") ?: STONE_DURATION_MS
                    out.add(RawTimer(TimerCategory.MINING, "Stone", minedAt + dur, dur, "🪨"))
                }
            }
        }

        // Iron
        farm.getAsJsonObjectSafe("iron")?.let { iron ->
            for ((_, elem) in iron.entrySet()) {
                val ir = elem.asJsonObject.getAsJsonObjectSafe("iron") ?: continue
                if (ir.has("minedAt")) {
                    val minedAt = ir.get("minedAt").asLong
                    val dur = ir.getSafeLong("boostedTime") ?: IRON_DURATION_MS
                    out.add(RawTimer(TimerCategory.MINING, "Iron Ore", minedAt + dur, dur, "⛏️"))
                }
            }
        }

        // Gold
        farm.getAsJsonObjectSafe("gold")?.let { gold ->
            for ((_, elem) in gold.entrySet()) {
                val gd = elem.asJsonObject.getAsJsonObjectSafe("gold") ?: continue
                if (gd.has("minedAt")) {
                    val minedAt = gd.get("minedAt").asLong
                    val dur = gd.getSafeLong("boostedTime") ?: GOLD_DURATION_MS
                    out.add(RawTimer(TimerCategory.MINING, "Gold Ore", minedAt + dur, dur, "🪙"))
                }
            }
        }

        // Crimstones
        farm.getAsJsonObjectSafe("crimstones")?.let { crimstones ->
            for ((_, elem) in crimstones.entrySet()) {
                val cr = elem.asJsonObject.getAsJsonObjectSafe("crimstone") ?: continue
                if (cr.has("minedAt")) {
                    val minedAt = cr.get("minedAt").asLong
                    val dur = cr.getSafeLong("boostedTime") ?: CRIMSTONE_DURATION_MS
                    out.add(RawTimer(TimerCategory.MINING, "Crimstone", minedAt + dur, dur, "💎"))
                }
            }
        }
    }

    private fun extractCrops(farm: JsonObject, out: MutableList<RawTimer>) {
        val crops = farm.getAsJsonObjectSafe("crops") ?: return
        for ((_, elem) in crops.entrySet()) {
            if (!elem.isJsonObject) continue
            val c = elem.asJsonObject.getAsJsonObjectSafe("crop") ?: continue
            if (c.has("plantedAt")) {
                val plantedAt = c.get("plantedAt").asLong
                val name = c.getSafeString("name") ?: "Crop"
                val baseDur = CROP_DURATIONS[name] ?: 3600_000L
                val dur = c.getSafeLong("boostedTime") ?: baseDur
                val readyAt = c.getSafeLong("readyAt") ?: (plantedAt + dur)
                out.add(RawTimer(TimerCategory.CROPS, name, readyAt, dur, "🌾"))
            }
        }
    }

    private fun extractFruits(farm: JsonObject, out: MutableList<RawTimer>) {
        val fruits = farm.getAsJsonObjectSafe("fruitPatches") ?: return
        for ((_, elem) in fruits.entrySet()) {
            if (!elem.isJsonObject) continue
            val f = elem.asJsonObject.getAsJsonObjectSafe("fruit") ?: continue
            if (f.has("plantedAt")) {
                val plantedAt = f.get("plantedAt").asLong
                val name = f.getSafeString("name") ?: "Fruit"
                val baseDur = FRUIT_DURATIONS[name] ?: (4 * 3600_000L)
                val dur = f.getSafeLong("boostedTime") ?: baseDur
                val readyAt = f.getSafeLong("readyAt") ?: (plantedAt + dur)
                out.add(RawTimer(TimerCategory.CROPS, "$name Fruit", readyAt, dur, "🍊"))
            }
        }
    }

    private fun extractOil(farm: JsonObject, out: MutableList<RawTimer>) {
        val oils = farm.getAsJsonObjectSafe("oilReserves") ?: return
        for ((_, elem) in oils.entrySet()) {
            if (!elem.isJsonObject) continue
            val oil = elem.asJsonObject.getAsJsonObjectSafe("oil") ?: continue
            if (oil.has("drilledAt")) {
                val drilledAt = oil.get("drilledAt").asLong
                val dur = oil.getSafeLong("boostedTime") ?: OIL_DURATION_MS
                out.add(RawTimer(TimerCategory.MINING, "Oil Reserve", drilledAt + dur, dur, "🛢️"))
            }
        }
    }

    private fun extractBeehives(farm: JsonObject, out: MutableList<RawTimer>) {
        val beehives = farm.getAsJsonObjectSafe("beehives") ?: return
        for ((_, elem) in beehives.entrySet()) {
            if (!elem.isJsonObject) continue
            val b = elem.asJsonObject
            val flowers = b.getAsJsonArraySafe("flowers")
            if (flowers != null && flowers.size() > 0) {
                var maxUntil = 0L
                for (fl in flowers) {
                    if (fl.isJsonObject && fl.asJsonObject.has("attachedUntil")) {
                        val until = fl.asJsonObject.get("attachedUntil").asLong
                        if (until > maxUntil) maxUntil = until
                    }
                }
                if (maxUntil > 0L) {
                    out.add(RawTimer(TimerCategory.BEEHIVES, "Beehive Honey", maxUntil, 0L, "🐝"))
                }
            }
        }
    }

    private fun extractAnimals(farm: JsonObject, out: MutableList<RawTimer>) {
        // Chickens
        farm.getAsJsonObjectSafe("chickens")?.let { chickens ->
            for ((_, elem) in chickens.entrySet()) {
                if (!elem.isJsonObject) continue
                val ch = elem.asJsonObject
                if (ch.has("fedAt")) {
                    val fedAt = ch.get("fedAt").asLong
                    val dur = ch.getSafeLong("boostedTime") ?: (2 * 3600_000L)
                    out.add(RawTimer(TimerCategory.ANIMALS, "Chicken Egg", fedAt + dur, dur, "🐔"))
                }
            }
        }
    }

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

    private fun JsonObject.getAsJsonArraySafe(key: String): com.google.gson.JsonArray? {
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
