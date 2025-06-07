package com.example.growagarden.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ForegroundServicePermissionHelper {

    const val FOREGROUND_SERVICE_PERMISSION_REQUEST_CODE = 1002

    fun hasForegroundServicePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasAllRequiredPermissions(context: Context): Boolean {
        val hasNotificationPermission = NotificationPermissionHelper.hasNotificationPermission(context)
        val hasForegroundServicePermission = hasForegroundServicePermission(context)
        return hasNotificationPermission && hasForegroundServicePermission
    }

    fun requestForegroundServicePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (!hasForegroundServicePermission(activity)) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.FOREGROUND_SERVICE
                    )) {
                    showPermissionRationaleDialog(activity) {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.FOREGROUND_SERVICE),
                            FOREGROUND_SERVICE_PERMISSION_REQUEST_CODE
                        )
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.FOREGROUND_SERVICE),
                        FOREGROUND_SERVICE_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    fun requestAllPermissions(activity: Activity) {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationPermissionHelper.hasNotificationPermission(activity)) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (!hasForegroundServicePermission(activity)) {
                permissionsNeeded.add(Manifest.permission.FOREGROUND_SERVICE)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            showAllPermissionsRationaleDialog(activity) {
                ActivityCompat.requestPermissions(
                    activity,
                    permissionsNeeded.toTypedArray(),
                    FOREGROUND_SERVICE_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun showPermissionRationaleDialog(context: Context, onPositive: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Enable Background Monitoring")
            .setMessage("Allow the app to run in the background to continuously monitor your favorite items even when the app is closed. This ensures you never miss when they become available!")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("Enable") { _, _ -> onPositive() }
            .setNegativeButton("Not Now") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showAllPermissionsRationaleDialog(context: Context, onPositive: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Enable Full Monitoring")
            .setMessage("To provide the best experience, the app needs permissions to:\n\n• Send notifications when favorite items are available\n• Run in the background to monitor stocks continuously\n\nThis ensures you never miss your favorite items!")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("Enable All") { _, _ -> onPositive() }
            .setNegativeButton("Not Now") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun showPermissionDeniedDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Background Monitoring Disabled")
            .setMessage("The app won't be able to monitor your favorite items when closed. You can enable this permission in your device settings later for the best experience.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}