package com.example.growagarden.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.growagarden.MainActivity
import com.example.growagarden.R
import com.example.growagarden.repository.GardenRepository
import com.example.growagarden.favorites.FavoritesManager
import com.example.growagarden.notifications.NotificationService
import com.example.growagarden.data.StockInfo
import kotlinx.coroutines.*

class ForegroundStockService : Service() {

    companion object {
        private const val SERVICE_ID = 1000
        private const val CHANNEL_ID = "foreground_stock_service"
        private const val CHANNEL_NAME = "Stock Monitoring Service"
        private const val REFRESH_INTERVAL_MS = 60000L // 1 minute

        fun startService(context: Context) {
            val intent = Intent(context, ForegroundStockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ForegroundStockService::class.java)
            context.stopService(intent)
        }
    }

    private val repository = GardenRepository()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null
    private var lastStockInfo: StockInfo? = null
    private var refreshCount = 0

    private lateinit var favoritesManager: FavoritesManager
    private lateinit var notificationService: NotificationService

    override fun onCreate() {
        super.onCreate()
        favoritesManager = FavoritesManager.getInstance(this)
        notificationService = NotificationService(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(SERVICE_ID, createServiceNotification())
        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        scope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the app monitoring for favorite items in the background"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val favoritesCount = favoritesManager.getFavorites().size

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🌾 Garden Tracker Active")
            .setContentText("Monitoring $favoritesCount favorite items • Refresh #$refreshCount")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateServiceNotification() {
        try {
            val notification = createServiceNotification()
            val notificationManager = NotificationManagerCompat.from(this)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(SERVICE_ID, notification)
            }
        } catch (e: SecurityException) {
            // Handle notification permission denied
        }
    }

    private fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    if (favoritesManager.getFavorites().isEmpty()) {
                        delay(REFRESH_INTERVAL_MS)
                        continue
                    }

                    refreshCount++
                    updateServiceNotification()

                    val stockResult = repository.getStockData()

                    if (stockResult.isSuccess) {
                        val newStockInfo = stockResult.getOrNull()
                        newStockInfo?.let { stockInfo ->
                            if (lastStockInfo != null && hasStockChanges(lastStockInfo!!, stockInfo)) {
                                notificationService.checkForFavoriteItems(stockInfo)
                            }
                            lastStockInfo = stockInfo
                        }
                    }

                    delay(REFRESH_INTERVAL_MS)
                } catch (e: Exception) {
                    delay(REFRESH_INTERVAL_MS)
                }
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun hasStockChanges(oldInfo: StockInfo, newInfo: StockInfo): Boolean {
        val oldStockHash = generateStockHash(oldInfo)
        val newStockHash = generateStockHash(newInfo)
        return oldStockHash != newStockHash
    }

    private fun generateStockHash(stockInfo: StockInfo): String {
        val allItems = stockInfo.gearStock + stockInfo.seedsStock + stockInfo.eggStock +
                stockInfo.cosmeticsStock + stockInfo.honeyStock + stockInfo.nightStock
        return allItems.joinToString(",") { "${it.name}-${it.value}" }.hashCode().toString()
    }
}