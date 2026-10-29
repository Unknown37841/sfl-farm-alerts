package com.sfl.notifapp.data.repository

import android.content.Context
import org.json.JSONArray
import java.io.InputStreamReader

data class QuestDetail(
    val id: String,
    val title: String,
    val description: String,
    val choices: String,
    val outcomes: String
)

object QuestRepository {
    private var cachedQuests: Map<String, QuestDetail>? = null

    fun getQuest(context: Context, rawName: String): QuestDetail? {
        val map = getQuests(context)
        val normalized = normalize(rawName)
        return map[normalized] ?: map.values.firstOrNull { 
            val normTitle = normalize(it.title)
            normTitle == normalized || it.id.contains(normalized) || normalized.contains(it.id)
        }
    }

    @Synchronized
    fun getQuests(context: Context): Map<String, QuestDetail> {
        cachedQuests?.let { return it }

        val map = mutableMapOf<String, QuestDetail>()
        try {
            val stream = context.assets.open("quests.json")
            val text = InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
            val array = JSONArray(text)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                val title = obj.optString("title")
                val desc = obj.optString("description")
                val choices = obj.optString("choices")
                val outcomes = obj.optString("outcomes")
                val quest = QuestDetail(id, title, desc, choices, outcomes)
                map[id] = quest
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedQuests = map
        return map
    }

    fun normalize(s: String): String {
        return s.lowercase()
            .replace("'", "")
            .replace("’", "")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }
}
