package com.example.growagarden.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.growagarden.data.*
import com.example.growagarden.repository.GardenRepository
import com.example.growagarden.favorites.FavoritesManager
import com.example.growagarden.notifications.NotificationService
import com.example.growagarden.service.AutoRefreshService
import com.example.growagarden.lifecycle.AppLifecycleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class UiState(
    val isLoading: Boolean = false,
    val stockInfo: StockInfo? = null,
    val weatherData: WeatherResponse? = null,
    val resetTimes: ResetTimes? = null,
    val error: String? = null,
    val lastUpdateTime: Long = 0L
)

class GardenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GardenRepository()
    private val favoritesManager = FavoritesManager.getInstance(application)
    private val notificationService = NotificationService(application)
    private val lifecycleManager = AppLifecycleManager(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _autoRefreshStatus = MutableStateFlow("Starting...")
    val autoRefreshStatus: StateFlow<String> = _autoRefreshStatus.asStateFlow()

    private var lastStockInfo: StockInfo? = null
    private var autoRefreshService: AutoRefreshService? = null
    private var refreshCount = 0

    init {
        setupAutoRefresh()
        loadData()
    }

    private fun setupAutoRefresh() {
        autoRefreshService = AutoRefreshService(
            context = getApplication(),
            onRefreshNeeded = {
                performAutoRefresh()
            },
            onResetTimesUpdate = { resetTimes ->
                _uiState.value = _uiState.value.copy(resetTimes = resetTimes)
            }
        )
        autoRefreshService?.start()
        _autoRefreshStatus.value = "Auto-refresh active"
    }

    private suspend fun performAutoRefresh() {
        try {
            refreshCount++
            _autoRefreshStatus.value = "Auto-refreshing... (#$refreshCount)"

            val stockResult = repository.getStockData()
            val weatherResult = repository.getWeatherData()

            val stockInfo = if (stockResult.isSuccess) {
                val info = stockResult.getOrNull()
                info?.let { addFavoriteStatus(it) }
            } else null

            val weatherData = if (weatherResult.isSuccess) weatherResult.getOrNull() else null

            stockInfo?.let { newStockInfo ->
                if (lastStockInfo != null && hasNewStocks(lastStockInfo!!, newStockInfo)) {
                    if (!lifecycleManager.isInBackground()) {
                        notificationService.checkForFavoriteItems(newStockInfo)
                    }
                }
                lastStockInfo = newStockInfo
            }

            val error = when {
                stockResult.isFailure && weatherResult.isFailure -> "Auto-refresh failed"
                stockResult.isFailure -> "Stock data failed"
                weatherResult.isFailure -> "Weather data failed"
                else -> null
            }

            _uiState.value = _uiState.value.copy(
                stockInfo = stockInfo,
                weatherData = weatherData,
                error = error,
                lastUpdateTime = System.currentTimeMillis()
            )

            _autoRefreshStatus.value = "Last updated: ${getTimeAgo()}"

        } catch (e: Exception) {
            _autoRefreshStatus.value = "Auto-refresh error, retrying..."
            delay(5000L)
        }
    }

    private fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val lastUpdate = _uiState.value.lastUpdateTime
        val diffSeconds = (now - lastUpdate) / 1000

        return when {
            diffSeconds < 60 -> "just now"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            else -> "${diffSeconds / 3600}h ago"
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshService?.stop()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            performDataLoad()
        }
    }

    private suspend fun performDataLoad() {
        try {
            val stockResult = repository.getStockData()
            val weatherResult = repository.getWeatherData()

            val stockInfo = if (stockResult.isSuccess) {
                val info = stockResult.getOrNull()
                info?.let { addFavoriteStatus(it) }
            } else null

            val weatherData = if (weatherResult.isSuccess) weatherResult.getOrNull() else null

            stockInfo?.let { newStockInfo ->
                if (lastStockInfo != null && hasNewStocks(lastStockInfo!!, newStockInfo)) {
                    notificationService.checkForFavoriteItems(newStockInfo)
                }
                lastStockInfo = newStockInfo
            }

            val error = when {
                stockResult.isFailure && weatherResult.isFailure ->
                    "Failed to load stock and weather data"
                stockResult.isFailure -> "Failed to load stock data"
                weatherResult.isFailure -> "Failed to load weather data"
                else -> null
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                stockInfo = stockInfo,
                weatherData = weatherData,
                error = error,
                lastUpdateTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Unknown error occurred"
            )
        }
    }

    private fun hasNewStocks(oldInfo: StockInfo, newInfo: StockInfo): Boolean {
        return oldInfo.gearStock != newInfo.gearStock ||
                oldInfo.seedsStock != newInfo.seedsStock ||
                oldInfo.eggStock != newInfo.eggStock ||
                oldInfo.cosmeticsStock != newInfo.cosmeticsStock ||
                oldInfo.honeyStock != newInfo.honeyStock ||
                oldInfo.nightStock != newInfo.nightStock
    }

    private fun addFavoriteStatus(stockInfo: StockInfo): StockInfo {
        fun List<StockItem>.withFavorites(type: StockType) = map { item ->
            item.copy(isFavorite = favoritesManager.isFavorite(item.name, type))
        }

        return stockInfo.copy(
            gearStock = stockInfo.gearStock.withFavorites(StockType.GEAR),
            seedsStock = stockInfo.seedsStock.withFavorites(StockType.SEEDS),
            eggStock = stockInfo.eggStock.withFavorites(StockType.EGGS),
            cosmeticsStock = stockInfo.cosmeticsStock.withFavorites(StockType.COSMETICS),
            honeyStock = stockInfo.honeyStock.withFavorites(StockType.HONEY),
            nightStock = stockInfo.nightStock.withFavorites(StockType.NIGHT)
        )
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            performDataLoad()
            _isRefreshing.value = false
            autoRefreshService?.triggerFastRefresh()
        }
    }

    fun toggleFavorite(item: StockItem, stockType: StockType) {
        if (favoritesManager.isFavorite(item.name, stockType)) {
            favoritesManager.removeFavorite(item.name, stockType)
        } else {
            favoritesManager.addFavorite(item.name, stockType)
        }

        lifecycleManager.onFavoritesChanged()

        _uiState.value.stockInfo?.let { stockInfo ->
            _uiState.value = _uiState.value.copy(stockInfo = addFavoriteStatus(stockInfo))
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            _autoRefreshStatus.value = "Force refreshing..."
            autoRefreshService?.triggerFastRefresh()
            performAutoRefresh()
        }
    }

    fun getStocksByType(type: StockType): List<StockItem> {
        val stockInfo = _uiState.value.stockInfo ?: return emptyList()

        return when (type) {
            StockType.GEAR -> stockInfo.gearStock
            StockType.SEEDS -> stockInfo.seedsStock
            StockType.EGGS -> stockInfo.eggStock
            StockType.COSMETICS -> stockInfo.cosmeticsStock
            StockType.HONEY -> stockInfo.honeyStock
            StockType.NIGHT -> stockInfo.nightStock
            StockType.ALL -> getAllStocks(stockInfo)
        }
    }

    private fun getAllStocks(stockInfo: StockInfo): List<StockItem> {
        return stockInfo.gearStock + stockInfo.seedsStock + stockInfo.eggStock +
                stockInfo.cosmeticsStock + stockInfo.honeyStock + stockInfo.nightStock
    }

    fun getTotalItemCount(): Int {
        val stockInfo = _uiState.value.stockInfo ?: return 0
        return stockInfo.gearStock.size + stockInfo.seedsStock.size + stockInfo.eggStock.size +
                stockInfo.cosmeticsStock.size + stockInfo.honeyStock.size + stockInfo.nightStock.size
    }

    fun getFavoritesCount(): Int {
        return favoritesManager.getFavorites().size
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}