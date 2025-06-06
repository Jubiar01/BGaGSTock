package com.example.growagarden

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.example.growagarden.background.BackgroundStockWorker
import com.example.growagarden.lifecycle.AppLifecycleManager

class GardenApplication : Application(), Configuration.Provider {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var lifecycleManager: AppLifecycleManager

    override fun onCreate() {
        super.onCreate()
        instance = this

        initializeLifecycleManager()
    }

    private fun initializeLifecycleManager() {
        lifecycleManager = AppLifecycleManager(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        @Volatile
        private var instance: GardenApplication? = null

        fun getInstance(): GardenApplication {
            return instance ?: synchronized(this) {
                instance ?: GardenApplication().also { instance = it }
            }
        }
    }
}