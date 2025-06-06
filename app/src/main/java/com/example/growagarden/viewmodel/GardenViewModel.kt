package com.example.growagarden.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.growagarden.data.*
import com.example.growagarden.repository.GardenRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = false,
    val stockInfo: StockInfo? = null,
    val weatherData: WeatherResponse? = null,
    val resetTimes: ResetTimes? = null,
    val error: String? = null
)

class GardenViewModel : ViewModel() {
    private val repository = GardenRepository()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadData()
        calculateResetTimes()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val stockResult = repository.getStockData()
                val weatherResult = repository.getWeatherData()

                val stockInfo = if (stockResult.isSuccess) stockResult.getOrNull() else null
                val weatherData = if (weatherResult.isSuccess) weatherResult.getOrNull() else null

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
                    error = error
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData()
            _isRefreshing.value = false
        }
    }

    fun calculateResetTimes() {
        viewModelScope.launch {
            val resetTimes = repository.calculateResetTimes()
            _uiState.value = _uiState.value.copy(resetTimes = resetTimes)
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
            StockType.BLOOD -> stockInfo.bloodStock
            StockType.ALL -> getAllStocks(stockInfo)
        }
    }

    private fun getAllStocks(stockInfo: StockInfo): List<StockItem> {
        return stockInfo.gearStock + stockInfo.seedsStock + stockInfo.eggStock +
                stockInfo.cosmeticsStock + stockInfo.honeyStock + stockInfo.nightStock +
                stockInfo.bloodStock
    }

    fun getTotalItemCount(): Int {
        val stockInfo = _uiState.value.stockInfo ?: return 0
        return stockInfo.gearStock.size + stockInfo.seedsStock.size + stockInfo.eggStock.size +
                stockInfo.cosmeticsStock.size + stockInfo.honeyStock.size + stockInfo.nightStock.size +
                stockInfo.bloodStock.size
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}