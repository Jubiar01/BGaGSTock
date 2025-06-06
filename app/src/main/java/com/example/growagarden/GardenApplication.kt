package com.example.growagarden

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GardenApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

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