package com.example.growagarden.favorites

import android.content.Context
import android.content.SharedPreferences
import com.example.growagarden.data.FavoriteItem
import com.example.growagarden.data.StockType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val FAVORITES_KEY = "favorite_items"

        @Volatile
        private var INSTANCE: FavoritesManager? = null

        fun getInstance(context: Context): FavoritesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FavoritesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun addFavorite(itemName: String, stockType: StockType) {
        val favorites = getFavorites().toMutableList()
        if (!favorites.any { it.name == itemName && it.stockType == stockType }) {
            favorites.add(FavoriteItem(itemName, stockType))
            saveFavorites(favorites)
        }
    }

    fun removeFavorite(itemName: String, stockType: StockType) {
        val favorites = getFavorites().toMutableList()
        favorites.removeAll { it.name == itemName && it.stockType == stockType }
        saveFavorites(favorites)
    }

    fun isFavorite(itemName: String, stockType: StockType): Boolean {
        return getFavorites().any { it.name == itemName && it.stockType == stockType }
    }

    fun getFavorites(): List<FavoriteItem> {
        val favoritesJson = prefs.getString(FAVORITES_KEY, "[]")
        val type = object : TypeToken<List<FavoriteItem>>() {}.type
        return gson.fromJson(favoritesJson, type) ?: emptyList()
    }

    fun getFavoritesByType(stockType: StockType): List<FavoriteItem> {
        return getFavorites().filter { it.stockType == stockType }
    }

    fun clearFavorites() {
        prefs.edit().remove(FAVORITES_KEY).apply()
    }

    private fun saveFavorites(favorites: List<FavoriteItem>) {
        val favoritesJson = gson.toJson(favorites)
        prefs.edit().putString(FAVORITES_KEY, favoritesJson).apply()
    }
}