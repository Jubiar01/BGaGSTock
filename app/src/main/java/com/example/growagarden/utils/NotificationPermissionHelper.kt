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

object NotificationPermissionHelper {

    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    )) {
                    showPermissionRationaleDialog(activity) {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                        )
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    fun requestNotificationPermission(fragment: Fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(fragment.requireContext())) {
                if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showPermissionRationaleDialog(fragment.requireContext()) {
                        fragment.requestPermissions(
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                        )
                    }
                } else {
                    fragment.requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private fun showPermissionRationaleDialog(context: Context, onPositive: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Enable Notifications")
            .setMessage("Get notified when your favorite items become available in the garden stock! This helps you never miss the items you're looking for.")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("Enable") { _, _ -> onPositive() }
            .setNegativeButton("Not Now") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun showPermissionDeniedDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Notifications Disabled")
            .setMessage("You won't receive notifications when favorite items become available. You can enable notifications in your device settings later.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}