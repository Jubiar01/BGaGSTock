package com.example.growagarden.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

object NetworkUtils {

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
}

object TimeUtils {

    fun formatTimestamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    fun formatResetTime(hours: Int, minutes: Int, seconds: Int): String {
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun formatCountdown(totalSeconds: Int): String {
        if (totalSeconds <= 0) {
            return "⚡ Resetting now!"
        }

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun getSecondsUntilNextInterval(intervalMinutes: Int): Int {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.MINUTE)
        val currentSeconds = now.get(Calendar.SECOND)

        val totalCurrentSeconds = (currentMinutes * 60) + currentSeconds
        val intervalSeconds = intervalMinutes * 60
        val secondsSinceLastReset = totalCurrentSeconds % intervalSeconds
        return intervalSeconds - secondsSinceLastReset
    }

    fun getSecondsUntilNextHour(): Int {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.MINUTE)
        val currentSeconds = now.get(Calendar.SECOND)
        return ((60 - currentMinutes) * 60) - currentSeconds
    }

    fun getSecondsUntilNext4Hours(): Int {
        val now = Calendar.getInstance()
        val currentHours = now.get(Calendar.HOUR_OF_DAY)
        val currentMinutes = now.get(Calendar.MINUTE)
        val currentSeconds = now.get(Calendar.SECOND)

        val hoursUntilNext4h = 4 - (currentHours % 4)
        return (hoursUntilNext4h * 3600) - (currentMinutes * 60) - currentSeconds
    }

    fun getSecondsUntilNext30Minutes(): Int {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.MINUTE)
        val currentSeconds = now.get(Calendar.SECOND)

        return if (currentMinutes < 30) {
            ((30 - currentMinutes) * 60) - currentSeconds
        } else {
            ((60 - currentMinutes) * 60) - currentSeconds
        }
    }
}

object Constants {
    const val BASE_URL = "https://growagarden.gg"
    const val API_TIMEOUT_SECONDS = 15L
    const val REFRESH_INTERVAL_MS = 30000L
    const val ANIMATION_DURATION_MS = 300L
    const val TIMER_UPDATE_INTERVAL_MS = 1000L

    const val STOCK_RESET_INTERVAL_MINUTES = 5
    const val EGG_RESET_INTERVAL_MINUTES = 30
    const val HONEY_RESET_INTERVAL_HOURS = 1
    const val COSMETIC_RESET_INTERVAL_HOURS = 4

    const val PREFS_NAME = "garden_prefs"
    const val PREF_LAST_REFRESH = "last_refresh"
    const val PREF_AUTO_REFRESH = "auto_refresh"
}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (exception: Throwable) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}

inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) action()
    return this
}

object DeviceUtils {

    fun isTablet(context: Context): Boolean {
        val configuration = context.resources.configuration
        return configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    fun getScreenWidthDp(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return (displayMetrics.widthPixels / displayMetrics.density).toInt()
    }

    fun getOptimalSpanCount(context: Context, itemWidthDp: Int = 180): Int {
        val screenWidthDp = getScreenWidthDp(context)
        return when {
            screenWidthDp >= 840 -> 4
            screenWidthDp >= 600 -> 3
            screenWidthDp >= 480 -> 2
            else -> 1
        }
    }
}

object PreferenceManager {

    fun getSharedPreferences(context: Context) =
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    fun saveLastRefreshTime(context: Context, timestamp: Long) {
        getSharedPreferences(context)
            .edit()
            .putLong(Constants.PREF_LAST_REFRESH, timestamp)
            .apply()
    }

    fun getLastRefreshTime(context: Context): Long {
        return getSharedPreferences(context)
            .getLong(Constants.PREF_LAST_REFRESH, 0)
    }

    fun setAutoRefreshEnabled(context: Context, enabled: Boolean) {
        getSharedPreferences(context)
            .edit()
            .putBoolean(Constants.PREF_AUTO_REFRESH, enabled)
            .apply()
    }

    fun isAutoRefreshEnabled(context: Context): Boolean {
        return getSharedPreferences(context)
            .getBoolean(Constants.PREF_AUTO_REFRESH, true)
    }
}