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
                    delay(30000L)
                } catch (e: Exception) {
                    delay(60000L)
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
            repeat(15) { attempt ->
                try {
                    onRefreshNeeded()
                    val delay = when {
                        attempt < 5 -> 1000L
                        attempt < 10 -> 2000L
                        else -> 5000L
                    }
                    delay(delay)
                } catch (e: Exception) {
                    delay(3000L)
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

        return minOf(secondsUntilGearReset, secondsUntilEggReset, 30)
    }

    fun schedulePreemptiveRefresh() {
        val secondsUntilReset = getSecondsUntilNextReset()
        if (secondsUntilReset > 5) {
            scheduleRefreshIn(secondsUntilReset - 3)
        }
    }
}