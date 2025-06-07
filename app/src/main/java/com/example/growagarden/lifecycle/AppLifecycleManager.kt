package com.example.growagarden.lifecycle

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.growagarden.background.BackgroundStockWorker
import com.example.growagarden.service.ForegroundStockService
import com.example.growagarden.favorites.FavoritesManager

class AppLifecycleManager(private val application: Application) : DefaultLifecycleObserver {

    private var isAppInBackground = false
    private val favoritesManager = FavoritesManager.getInstance(application)

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isAppInBackground = false

        ForegroundStockService.stopService(application)
        BackgroundStockWorker.cancelWork(application)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInBackground = true

        if (favoritesManager.getFavorites().isNotEmpty()) {
            ForegroundStockService.startService(application)
        }
    }

    fun isInBackground(): Boolean = isAppInBackground

    fun onFavoritesChanged() {
        if (isAppInBackground) {
            if (favoritesManager.getFavorites().isNotEmpty()) {
                ForegroundStockService.startService(application)
            } else {
                ForegroundStockService.stopService(application)
            }
        }
    }

    fun stopAllServices() {
        ForegroundStockService.stopService(application)
        BackgroundStockWorker.cancelWork(application)
    }
}