package com.example.growagarden.background

import android.content.Context
import androidx.work.*
import com.example.growagarden.repository.GardenRepository
import com.example.growagarden.favorites.FavoritesManager
import com.example.growagarden.notifications.NotificationService
import com.example.growagarden.data.StockInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BackgroundStockWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "background_stock_check"
        private const val LAST_STOCK_KEY = "last_stock_data"

        fun scheduleWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .build()

            val repeatingWork = PeriodicWorkRequestBuilder<BackgroundStockWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10000L,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                repeatingWork
            )
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun scheduleFrequentWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val frequentWork = PeriodicWorkRequestBuilder<BackgroundStockWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag("frequent_refresh")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "frequent_stock_check",
                ExistingPeriodicWorkPolicy.REPLACE,
                frequentWork
            )
        }
    }

    private val repository = GardenRepository()
    private val favoritesManager = FavoritesManager.getInstance(applicationContext)
    private val notificationService = NotificationService(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (favoritesManager.getFavorites().isEmpty()) {
                return@withContext Result.success()
            }

            val stockResult = repository.getStockData()

            if (stockResult.isSuccess) {
                val newStockInfo = stockResult.getOrNull()
                newStockInfo?.let { stockInfo ->
                    val lastStockHash = getLastStockHash()
                    val currentStockHash = generateStockHash(stockInfo)

                    if (lastStockHash != currentStockHash) {
                        notificationService.checkForFavoriteItems(stockInfo)
                        saveStockHash(currentStockHash)
                    }
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun generateStockHash(stockInfo: StockInfo): String {
        val allItems = stockInfo.gearStock + stockInfo.seedsStock + stockInfo.eggStock +
                stockInfo.cosmeticsStock + stockInfo.honeyStock + stockInfo.nightStock
        return allItems.joinToString(",") { "${it.name}-${it.value}" }.hashCode().toString()
    }

    private fun getLastStockHash(): String? {
        val prefs = applicationContext.getSharedPreferences("background_worker", Context.MODE_PRIVATE)
        return prefs.getString(LAST_STOCK_KEY, null)
    }

    private fun saveStockHash(hash: String) {
        val prefs = applicationContext.getSharedPreferences("background_worker", Context.MODE_PRIVATE)
        prefs.edit().putString(LAST_STOCK_KEY, hash).apply()
    }
}