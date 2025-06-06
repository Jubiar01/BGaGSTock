package com.example.growagarden.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.growagarden.favorites.FavoritesManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                restartBackgroundWork(context)
            }
        }
    }

    private fun restartBackgroundWork(context: Context) {
        val favoritesManager = FavoritesManager.getInstance(context)

        if (favoritesManager.getFavorites().isNotEmpty()) {
            BackgroundStockWorker.scheduleWork(context)
        }
    }
}