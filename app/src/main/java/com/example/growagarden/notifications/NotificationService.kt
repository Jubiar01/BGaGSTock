package com.example.growagarden.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.growagarden.MainActivity
import com.example.growagarden.R
import com.example.growagarden.data.StockInfo
import com.example.growagarden.data.StockItem
import com.example.growagarden.data.StockType
import com.example.growagarden.favorites.FavoritesManager

class NotificationService(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "stock_notifications"
        private const val CHANNEL_NAME = "Stock Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for favorite items in stock"
        private const val NOTIFICATION_ID = 1001
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val favoritesManager = FavoritesManager.getInstance(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun checkForFavoriteItems(stockInfo: StockInfo) {
        val allFavorites = favoritesManager.getFavorites()
        if (allFavorites.isEmpty()) return

        val availableFavorites = mutableListOf<Pair<String, StockType>>()

        allFavorites.forEach { favorite ->
            val stockList = when (favorite.stockType) {
                StockType.GEAR -> stockInfo.gearStock
                StockType.SEEDS -> stockInfo.seedsStock
                StockType.EGGS -> stockInfo.eggStock
                StockType.COSMETICS -> stockInfo.cosmeticsStock
                StockType.HONEY -> stockInfo.honeyStock
                StockType.NIGHT -> stockInfo.nightStock
                else -> emptyList()
            }

            if (stockList.any { it.name == favorite.name }) {
                availableFavorites.add(Pair(favorite.name, favorite.stockType))
            }
        }

        if (availableFavorites.isNotEmpty()) {
            sendNotification(availableFavorites)
        }
    }

    private fun sendNotification(availableFavorites: List<Pair<String, StockType>>) {
        if (!notificationManager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (availableFavorites.size == 1) {
            "Favorite Item Available!"
        } else {
            "Favorite Items Available!"
        }

        val content = if (availableFavorites.size == 1) {
            val (name, type) = availableFavorites.first()
            "${type.emoji} $name is now available in ${type.displayName}!"
        } else {
            "${availableFavorites.size} favorite items are now available!"
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(buildString {
                availableFavorites.forEach { (name, type) ->
                    appendLine("${type.emoji} $name (${type.displayName})")
                }
            })

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle notification permission denied
        }
    }
}