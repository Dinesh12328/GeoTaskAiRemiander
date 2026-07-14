package com.dinesh.geotaskai.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import com.dinesh.geotaskai.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LocationPermissionManager(private val activity: Activity) {
    private val appContext = activity.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun hasForegroundLocationPermission(): Boolean {
        return hasFineLocationPermission() || hasCoarseLocationPermission()
    }

    fun hasFineLocationPermission(): Boolean = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasCoarseLocationPermission(): Boolean = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasBackgroundLocationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    fun allLocationReminderPermissionsGranted(): Boolean {
        return hasNotificationPermission() &&
            hasForegroundLocationPermission() &&
            hasBackgroundLocationPermission()
    }

    fun notificationStatusText(): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            activity.getString(R.string.permission_not_required)
        } else {
            grantedText(hasNotificationPermission())
        }
    }

    fun foregroundLocationStatusText(): String {
        return when {
            hasFineLocationPermission() -> activity.getString(R.string.permission_granted_precise)
            hasCoarseLocationPermission() -> activity.getString(R.string.permission_granted_approximate)
            else -> activity.getString(R.string.permission_not_granted)
        }
    }

    fun backgroundLocationStatusText(): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            activity.getString(R.string.permission_not_required)
        } else {
            grantedText(hasBackgroundLocationPermission())
        }
    }

    fun foregroundPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun backgroundPermission(): String = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    fun markNotificationRequested() {
        preferences.edit { putBoolean(KEY_NOTIFICATION_REQUESTED, true) }
    }

    fun markForegroundLocationRequested() {
        preferences.edit { putBoolean(KEY_FOREGROUND_LOCATION_REQUESTED, true) }
    }

    fun markBackgroundLocationRequested() {
        preferences.edit { putBoolean(KEY_BACKGROUND_LOCATION_REQUESTED, true) }
    }

    fun isNotificationPermanentlyDenied(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotificationPermission()) return false
        return preferences.getBoolean(KEY_NOTIFICATION_REQUESTED, false) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            )
    }

    fun isForegroundLocationPermanentlyDenied(): Boolean {
        if (hasForegroundLocationPermission()) return false
        if (!preferences.getBoolean(KEY_FOREGROUND_LOCATION_REQUESTED, false)) return false

        val fineNeedsRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        val coarseNeedsRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        return !fineNeedsRationale && !coarseNeedsRationale
    }

    fun isBackgroundLocationPermanentlyDenied(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || hasBackgroundLocationPermission()) return false
        return preferences.getBoolean(KEY_BACKGROUND_LOCATION_REQUESTED, false) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            )
    }

    fun shouldUseSettingsForBackgroundLocation(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    fun appSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null),
        )
    }

    fun showNotificationSettingsDialog(onOpenSettings: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.notification_settings_dialog_title)
            .setMessage(R.string.notification_settings_dialog_message)
            .setPositiveButton(R.string.open_settings) { _, _ -> onOpenSettings() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showForegroundLocationExplanation(onContinue: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.foreground_location_dialog_title)
            .setMessage(R.string.foreground_location_dialog_message)
            .setPositiveButton(R.string.continue_label) { _, _ -> onContinue() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showForegroundLocationSettingsDialog(onOpenSettings: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.foreground_location_settings_dialog_title)
            .setMessage(R.string.foreground_location_settings_dialog_message)
            .setPositiveButton(R.string.open_settings) { _, _ -> onOpenSettings() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showAndroid10BackgroundLocationExplanation(onContinue: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.background_location_dialog_title)
            .setMessage(R.string.background_location_android10_dialog_message)
            .setPositiveButton(R.string.continue_label) { _, _ -> onContinue() }
            .setNegativeButton(R.string.not_now, null)
            .show()
    }

    fun showBackgroundLocationSettingsDialog(onOpenSettings: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.background_location_settings_dialog_title)
            .setMessage(R.string.background_location_settings_dialog_message)
            .setPositiveButton(R.string.open_settings) { _, _ -> onOpenSettings() }
            .setNegativeButton(R.string.not_now, null)
            .show()
    }

    fun showAndroid11BackgroundLocationEducation(
        onOpenSettings: () -> Unit,
        onCancel: () -> Unit,
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.background_location_dialog_title)
            .setMessage(R.string.background_location_settings_dialog_message)
            .setPositiveButton(R.string.open_settings) { _, _ -> onOpenSettings() }
            .setNegativeButton(R.string.not_now) { _, _ -> onCancel() }
            .show()
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun grantedText(isGranted: Boolean): String {
        return if (isGranted) {
            activity.getString(R.string.permission_granted)
        } else {
            activity.getString(R.string.permission_not_granted)
        }
    }

    private companion object {
        const val PREFS_NAME = "location_permission_manager"
        const val KEY_NOTIFICATION_REQUESTED = "notification_requested"
        const val KEY_FOREGROUND_LOCATION_REQUESTED = "foreground_location_requested"
        const val KEY_BACKGROUND_LOCATION_REQUESTED = "background_location_requested"
    }
}
