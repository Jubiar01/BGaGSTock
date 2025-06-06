package com.example.growagarden.repository

import com.example.growagarden.data.*
import com.example.growagarden.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.random.Random

class GardenRepository {
    private val apiService = ApiClient.apiService

    suspend fun getStockData(): Result<StockInfo> = withContext(Dispatchers.IO) {
        try {
            val (timestamp, randomParam) = getTimestampAndRandom()
            val headers = createStockHeaders(timestamp, randomParam)

            val response = apiService.getStockData(
                timestamp = timestamp,
                randomParam = randomParam,
                headers = headers
            )

            if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                val stockInfo = response.body()!![0].result.data.json
                Result.success(stockInfo)
            } else {
                Result.failure(Exception("Failed to fetch stock data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeatherData(): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val (timestamp, randomParam) = getTimestampAndRandom()
            val headers = createWeatherHeaders(timestamp)

            val response = apiService.getWeatherData(
                timestamp = timestamp,
                randomParam = randomParam,
                headers = headers
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch weather data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun calculateResetTimes(): ResetTimes {
        val now = Calendar.getInstance()

        return ResetTimes(
            gear = calculateGearReset(now),
            egg = calculateEggReset(now),
            honey = calculateHoneyReset(now),
            cosmetic = calculateCosmeticReset(now)
        )
    }

    private fun getTimestampAndRandom(): Pair<Long, String> {
        val timestamp = System.currentTimeMillis()
        val randomParam = (1..7).map {
            "abcdefghijklmnopqrstuvwxyz0123456789"[Random.nextInt(36)]
        }.joinToString("")
        return Pair(timestamp, randomParam)
    }

    private fun createStockHeaders(timestamp: Long, randomParam: String): Map<String, String> {
        return mapOf(
            "accept" to "*/*",
            "accept-language" to "en-US,en;q=0.9",
            "priority" to "u=1, i",
            "referer" to "https://growagarden.gg/stocks",
            "trpc-accept" to "application/json",
            "x-trpc-source" to "gag",
            "User-Agent" to "GAG-Bot-V2/1.0-$randomParam-$timestamp",
            "Cache-Control" to "no-cache, no-store, must-revalidate, max-age=0",
            "Pragma" to "no-cache",
            "Expires" to "Thu, 01 Jan 1970 00:00:00 GMT",
            "X-Requested-With" to "XMLHttpRequest",
            "X-Cache-Buster" to timestamp.toString(),
            "If-None-Match" to "*",
            "If-Modified-Since" to "Thu, 01 Jan 1970 00:00:00 GMT"
        )
    }

    private fun createWeatherHeaders(timestamp: Long): Map<String, String> {
        return mapOf(
            "accept" to "*/*",
            "accept-language" to "en-US,en;q=0.9",
            "priority" to "u=1, i",
            "referer" to "https://growagarden.gg/weather",
            "Content-Length" to "0",
            "Cache-Control" to "no-cache, no-store, must-revalidate, max-age=0",
            "Pragma" to "no-cache",
            "Expires" to "Thu, 01 Jan 1970 00:00:00 GMT",
            "X-Cache-Buster" to timestamp.toString(),
            "If-None-Match" to "*",
            "If-Modified-Since" to "Thu, 01 Jan 1970 00:00:00 GMT"
        )
    }

    private fun calculateGearReset(now: Calendar): String {
        val currentMinutes = now.get(Calendar.MINUTE)
        val currentSeconds = now.get(Calendar.SECOND)

        val totalCurrentSeconds = (currentMinutes * 60) + currentSeconds
        val resetIntervalSeconds = 5 * 60

        val secondsSinceLastReset = totalCurrentSeconds % resetIntervalSeconds
        val secondsUntilNextReset = resetIntervalSeconds - secondsSinceLastReset

        if (secondsUntilNextReset <= 0) {
            return "⚡ Resetting now!"
        }

        val minutes = secondsUntilNextReset / 60
        val seconds = secondsUntilNextReset % 60
        return "${minutes}m ${seconds}s"
    }

    private fun calculateEggReset(now: Calendar): String {
        val currentMinutes = now.get(Calendar.MINUTE)
        val currentSeconds = now.get(Calendar.SECOND)

        val secondsUntilReset = if (currentMinutes < 30) {
            ((30 - currentMinutes) * 60) - currentSeconds
        } else {
            ((60 - currentMinutes) * 60) - currentSeconds
        }

        if (secondsUntilReset <= 0) {
            return "⚡ Resetting now!"
        }

        val minutes = secondsUntilReset / 60
        val seconds = secondsUntilReset % 60
        return "${minutes}m ${seconds}s"
    }

    private fun calculateHoneyReset(now: Calendar): String {
        val currentMinutes = now.get(Calendar.MINUTE)
        val currentSeconds = now.get(Calendar.SECOND)

        val secondsUntilReset = ((60 - currentMinutes) * 60) - currentSeconds

        if (secondsUntilReset <= 0 || (currentMinutes == 0 && currentSeconds == 0)) {
            return "⚡ Resetting now!"
        }

        val minutes = secondsUntilReset / 60
        val seconds = secondsUntilReset % 60
        return "${minutes}m ${seconds}s"
    }

    private fun calculateCosmeticReset(now: Calendar): String {
        val currentHours = now.get(Calendar.HOUR_OF_DAY)
        val currentMinutes = now.get(Calendar.MINUTE)
        val currentSeconds = now.get(Calendar.SECOND)

        val hoursUntilNext4h = 4 - (currentHours % 4)
        val totalSecondsUntilReset = (hoursUntilNext4h * 3600) - (currentMinutes * 60) - currentSeconds

        if (totalSecondsUntilReset <= 0) {
            return "⚡ Resetting now!"
        }

        val hours = totalSecondsUntilReset / 3600
        val minutes = (totalSecondsUntilReset % 3600) / 60
        val seconds = totalSecondsUntilReset % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}