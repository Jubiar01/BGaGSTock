package com.example.growagarden.service

import android.content.Context
import kotlinx.coroutines.*
import com.example.growagarden.repository.GardenRepository
import com.example.growagarden.data.ResetTimes
import java.util.*

class AutoRefreshService(
    private val context: Context,
    private val onRefreshNeeded: suspend () -> Unit,
    private val onResetTimesUpdate: (ResetTimes) -> Unit
) {
    private val repository = GardenRepository()
    private var refreshJob: Job? = null
    private var fastRefreshJob: Job? = null
    private var timerJob: Job? = null
    private var previousResetTimes: ResetTimes? = null
    private var isActive = false

    fun start() {
        if (isActive) return
        isActive = true

        startTimerUpdates()
        startPeriodicRefresh()
    }

    fun stop() {
        isActive = false
        refreshJob?.cancel()
        fastRefreshJob?.cancel()
        timerJob?.cancel()
    }

    private fun startTimerUpdates() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                try {
                    val newResetTimes = repository.calculateResetTimes()
                    onResetTimesUpdate(newResetTimes)

                    checkForResets(newResetTimes)
                    previousResetTimes = newResetTimes

                    delay(1000L)
                } catch (e: Exception) {
                    delay(5000L)
                }
            }
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    onRefreshNeeded()
                    delay(60000L) // Changed from 30 seconds to 1 minute
                } catch (e: Exception) {
                    delay(60000L) // Changed from 60 seconds to 1 minute
                }
            }
        }
    }

    private fun checkForResets(newResetTimes: ResetTimes) {
        previousResetTimes?.let { previous ->
            val resetDetected =
                (previous.gear != "⚡ Resetting now!" && newResetTimes.gear == "⚡ Resetting now!") ||
                        (previous.egg != "⚡ Resetting now!" && newResetTimes.egg == "⚡ Resetting now!") ||
                        (previous.honey != "⚡ Resetting now!" && newResetTimes.honey == "⚡ Resetting now!") ||
                        (previous.cosmetic != "⚡ Resetting now!" && newResetTimes.cosmetic == "⚡ Resetting now!")

            if (resetDetected) {
                startFastRefresh()
            }
        }
    }

    private fun startFastRefresh() {
        fastRefreshJob?.cancel()
        fastRefreshJob = CoroutineScope(Dispatchers.IO).launch {
            repeat(10) { attempt -> // Reduced from 15 to 10 attempts
                try {
                    onRefreshNeeded()
                    val delay = when {
                        attempt < 3 -> 2000L // Changed from 1 second to 2 seconds
                        attempt < 6 -> 5000L // Changed timing
                        else -> 10000L // Changed from 5 seconds to 10 seconds
                    }
                    delay(delay)
                } catch (e: Exception) {
                    delay(5000L)
                }
            }
        }
    }

    fun triggerFastRefresh() {
        startFastRefresh()
    }

    fun scheduleRefreshIn(seconds: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(seconds * 1000L)
            if (isActive) {
                onRefreshNeeded()
            }
        }
    }

    private fun getSecondsUntilNextReset(): Int {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.MINUTE)
        val currentSeconds = now.get(Calendar.SECOND)

        val totalCurrentSeconds = (currentMinutes * 60) + currentSeconds
        val gearResetInterval = 5 * 60
        val secondsUntilGearReset = gearResetInterval - (totalCurrentSeconds % gearResetInterval)

        val secondsUntilEggReset = if (currentMinutes < 30) {
            ((30 - currentMinutes) * 60) - currentSeconds
        } else {
            ((60 - currentMinutes) * 60) - currentSeconds
        }

        return minOf(secondsUntilGearReset, secondsUntilEggReset, 60) // Changed from 30 to 60 seconds
    }

    fun schedulePreemptiveRefresh() {
        val secondsUntilReset = getSecondsUntilNextReset()
        if (secondsUntilReset > 10) { // Changed from 5 to 10 seconds
            scheduleRefreshIn(secondsUntilReset - 5) // Keep 5 seconds buffer
        }
    }
}