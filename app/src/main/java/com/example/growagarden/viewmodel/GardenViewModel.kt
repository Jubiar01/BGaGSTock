package com.example.growagarden.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.growagarden.data.*
import com.example.growagarden.repository.GardenRepository
import com.example.growagarden.favorites.FavoritesManager
import com.example.growagarden.notifications.NotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

data class UiState(
    val isLoading: Boolean = false,
    val stockInfo: StockInfo? = null,
    val weatherData: WeatherResponse? = null,
    val resetTimes: ResetTimes? = null,
    val error: String? = null
)

class GardenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GardenRepository()
    private val favoritesManager = FavoritesManager.getInstance(application)
    private val notificationService = NotificationService(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var timerJob: Job? = null
    private var fastRefreshJob: Job? = null
    private var previousResetTimes: ResetTimes? = null
    private var lastStockInfo: StockInfo? = null

    init {
        loadData()
        startResetTimerUpdates()
    }

    private fun startResetTimerUpdates() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                calculateResetTimes()
                delay(1000L)
            }
        }
    }

    private fun startFastRefresh() {
        fastRefreshJob?.cancel()
        fastRefreshJob = viewModelScope.launch {
            repeat(10) {
                if (!_isRefreshing.value && !_uiState.value.isLoading) {
                    loadDataSilently()
                }
                delay(2000L)
            }

            repeat(5) {
                if (!_isRefreshing.value && !_uiState.value.isLoading) {
                    loadDataSilently()
                }
                delay(5000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        fastRefreshJob?.cancel()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            performDataLoad()
        }
    }

    private fun loadDataSilently() {
        viewModelScope.launch {
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

            val error = when {
                stockResult.isFailure && weatherResult.isFailure ->
                    "Failed to load stock and weather data"
                stockResult.isFailure -> "Failed to load stock data"
                weatherResult.isFailure -> "Failed to load weather data"
                else -> null
            }

            stockInfo?.let { newStockInfo ->
                if (lastStockInfo != null && hasNewStocks(lastStockInfo!!, newStockInfo)) {
                    notificationService.checkForFavoriteItems(newStockInfo)
                }
                lastStockInfo = newStockInfo
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                stockInfo = stockInfo,
                weatherData = weatherData,
                error = error
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
        }
    }

    fun calculateResetTimes() {
        val newResetTimes = repository.calculateResetTimes()

        previousResetTimes?.let { previous ->
            val shouldAutoRefresh =
                (previous.gear != "⚡ Resetting now!" && newResetTimes.gear == "⚡ Resetting now!") ||
                        (previous.egg != "⚡ Resetting now!" && newResetTimes.egg == "⚡ Resetting now!") ||
                        (previous.honey != "⚡ Resetting now!" && newResetTimes.honey == "⚡ Resetting now!") ||
                        (previous.cosmetic != "⚡ Resetting now!" && newResetTimes.cosmetic == "⚡ Resetting now!")

            if (shouldAutoRefresh && !_uiState.value.isLoading && !_isRefreshing.value) {
                refreshData()
                startFastRefresh()
            }
        }

        previousResetTimes = newResetTimes
        _uiState.value = _uiState.value.copy(resetTimes = newResetTimes)
    }

    fun toggleFavorite(item: StockItem, stockType: StockType) {
        if (favoritesManager.isFavorite(item.name, stockType)) {
            favoritesManager.removeFavorite(item.name, stockType)
        } else {
            favoritesManager.addFavorite(item.name, stockType)
        }

        _uiState.value.stockInfo?.let { stockInfo ->
            _uiState.value = _uiState.value.copy(stockInfo = addFavoriteStatus(stockInfo))
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