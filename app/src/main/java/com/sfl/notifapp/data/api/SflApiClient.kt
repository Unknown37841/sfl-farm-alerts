package com.sfl.notifapp.data.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class SflApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchFarmJson(farmId: String): Result<JsonObject> = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://sfl-overview.com/api/farms/$farmId",
            "https://api.sunflower-land.com/community/farms/$farmId"
        )

        var lastException: Exception? = null

        for (url in endpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrBlank()) {
                            val json = JsonParser.parseString(bodyString).asJsonObject
                            // If the response wraps under {"farm": ...}, return the root object
                            return@withContext Result.success(json)
                        }
                    } else if (response.code == 429) {
                        // Rate limited on this endpoint, try fallback
                        lastException = IOException("Rate limited (HTTP 429) from $url")
                    } else {
                        lastException = IOException("HTTP ${response.code} from $url")
                    }
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: IOException("Failed to fetch farm data from all endpoints"))
    }
}
