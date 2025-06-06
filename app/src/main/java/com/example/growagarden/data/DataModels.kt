package com.example.growagarden.data

import com.google.gson.annotations.SerializedName

data class StockResponse(
    @SerializedName("result")
    val result: StockResult
)

data class StockResult(
    @SerializedName("data")
    val data: StockData
)

data class StockData(
    @SerializedName("json")
    val json: StockInfo
)

data class StockInfo(
    @SerializedName("gearStock")
    val gearStock: List<StockItem> = emptyList(),
    @SerializedName("seedsStock")
    val seedsStock: List<StockItem> = emptyList(),
    @SerializedName("eggStock")
    val eggStock: List<StockItem> = emptyList(),
    @SerializedName("cosmeticsStock")
    val cosmeticsStock: List<StockItem> = emptyList(),
    @SerializedName("honeyStock")
    val honeyStock: List<StockItem> = emptyList(),
    @SerializedName("nightStock")
    val nightStock: List<StockItem> = emptyList()
)

data class StockItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: Int,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("price")
    val price: Int? = null,
    @SerializedName("rarity")
    val rarity: String? = null,
    var isFavorite: Boolean = false
)

data class FavoriteItem(
    val name: String,
    val stockType: StockType,
    val dateAdded: Long = System.currentTimeMillis()
)

data class WeatherResponse(
    @SerializedName("currentWeather")
    val currentWeather: String,
    @SerializedName("cropBonuses")
    val cropBonuses: String,
    @SerializedName("icon")
    val icon: String
)

data class ResetTimes(
    val gear: String,
    val egg: String,
    val honey: String,
    val cosmetic: String
)

enum class StockType(val displayName: String, val emoji: String) {
    GEAR("Gear", "🛠️"),
    SEEDS("Seeds", "🌱"),
    EGGS("Eggs", "🥚"),
    COSMETICS("Cosmetics", "💄"),
    HONEY("Honey", "🍯"),
    NIGHT("Night", "🌙"),
    ALL("All Stocks", "📊")
}