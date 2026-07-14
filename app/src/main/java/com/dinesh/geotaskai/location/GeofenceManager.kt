package com.dinesh.geotaskai.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.dinesh.geotaskai.R
import com.dinesh.geotaskai.data.TaskEntity
import com.dinesh.geotaskai.notification.NotificationHelper
import com.dinesh.geotaskai.ui.PermissionHelper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(context: Context) {
    private val appContext = context.applicationContext
    private val geofencingClient = LocationServices.getGeofencingClient(appContext)
    private val notificationHelper = NotificationHelper(appContext)
    private val permissionHelper = PermissionHelper(appContext)
    private val preferences: SharedPreferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            appContext,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    fun isTaskReminderEnabled(taskId: Long): Boolean {
        return enabledTaskIds().contains(taskId.toString())
    }

    fun registerTaskGeofence(task: TaskEntity, onResult: (GeofenceOperationResult) -> Unit) {
        val validationResult = validateTaskForRegistration(task)
        if (!validationResult.isSuccess) {
            onResult(validationResult)
            return
        }
        replaceTaskGeofence(task, onResult)
    }

    fun updateTaskGeofence(task: TaskEntity, onResult: (GeofenceOperationResult) -> Unit) {
        removeTaskGeofence(task.id) { removeResult ->
            if (!removeResult.isSuccess) {
                onResult(removeResult)
                return@removeTaskGeofence
            }

            val validationResult = validateTaskForRegistration(task)
            if (!validationResult.isSuccess) {
                setTaskReminderEnabled(task.id, false)
                onResult(validationResult)
                return@removeTaskGeofence
            }
            addTaskGeofence(task, onResult)
        }
    }

    fun removeTaskGeofence(
        taskId: Long,
        onResult: (GeofenceOperationResult) -> Unit = {},
    ) {
        if (taskId <= 0L) {
            onResult(GeofenceOperationResult.failure(appContext.getString(R.string.geofence_error_missing_task)))
            return
        }

        if (!isTaskReminderEnabled(taskId)) {
            onResult(GeofenceOperationResult.success(appContext.getString(R.string.geofence_removed)))
            return
        }

        geofencingClient.removeGeofences(listOf(taskId.toString()))
            .addOnSuccessListener {
                setTaskReminderEnabled(taskId, false)
                onResult(GeofenceOperationResult.success(appContext.getString(R.string.geofence_removed)))
            }
            .addOnFailureListener { error ->
                onResult(GeofenceOperationResult.failure(GeofenceErrorMessages.fromException(appContext, error)))
            }
    }

    fun clearEnabledState(taskId: Long) {
        setTaskReminderEnabled(taskId, false)
    }

    private fun replaceTaskGeofence(
        task: TaskEntity,
        onResult: (GeofenceOperationResult) -> Unit,
    ) {
        val requestId = task.id.toString()
        geofencingClient.removeGeofences(listOf(requestId))
            .addOnCompleteListener {
                addTaskGeofence(task, onResult)
            }
    }

    @SuppressLint("MissingPermission")
    private fun addTaskGeofence(
        task: TaskEntity,
        onResult: (GeofenceOperationResult) -> Unit,
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(task.id.toString())
            .setCircularRegion(
                task.latitude,
                task.longitude,
                task.radiusMeters.toFloat(),
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    setTaskReminderEnabled(task.id, true)
                    onResult(GeofenceOperationResult.success(appContext.getString(R.string.geofence_registered)))
                }
                .addOnFailureListener { error ->
                    setTaskReminderEnabled(task.id, false)
                    onResult(GeofenceOperationResult.failure(GeofenceErrorMessages.fromException(appContext, error)))
                }
        } catch (_: SecurityException) {
            setTaskReminderEnabled(task.id, false)
            onResult(GeofenceOperationResult.failure(appContext.getString(R.string.geofence_error_missing_permissions)))
        }
    }

    private fun validateTaskForRegistration(task: TaskEntity): GeofenceOperationResult {
        if (task.id <= 0L) {
            return GeofenceOperationResult.failure(appContext.getString(R.string.geofence_error_missing_task))
        }
        if (task.latitude !in MIN_LATITUDE..MAX_LATITUDE) {
            return GeofenceOperationResult.failure(appContext.getString(R.string.latitude_error))
        }
        if (task.longitude !in MIN_LONGITUDE..MAX_LONGITUDE) {
            return GeofenceOperationResult.failure(appContext.getString(R.string.longitude_error))
        }
        if (task.radiusMeters <= 0.0) {
            return GeofenceOperationResult.failure(appContext.getString(R.string.radius_error))
        }
        if (!notificationHelper.canPostNotifications()) {
            return GeofenceOperationResult.failure(appContext.getString(R.string.notification_permission_denied))
        }
        if (!permissionHelper.hasForegroundLocation()) {
            return GeofenceOperationResult.failure(appContext.getString(R.string.foreground_location_permission_denied))
        }
        if (!permissionHelper.hasBackgroundLocation()) {
            return GeofenceOperationResult.failure(appContext.getString(R.string.background_location_permission_denied_short))
        }
        if (!isLocationEnabled()) {
            return GeofenceOperationResult.failure(appContext.getString(R.string.location_services_disabled))
        }

        val playServicesStatus = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(appContext)
        if (playServicesStatus != ConnectionResult.SUCCESS) {
            return GeofenceOperationResult.failure(
                GeofenceErrorMessages.fromGooglePlayServicesCode(appContext, playServicesStatus),
            )
        }

        return GeofenceOperationResult.success(appContext.getString(R.string.location_reminder_ready))
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = ContextCompat.getSystemService(appContext, LocationManager::class.java)
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun enabledTaskIds(): Set<String> {
        return preferences.getStringSet(KEY_ENABLED_TASK_IDS, emptySet()).orEmpty()
    }

    private fun setTaskReminderEnabled(taskId: Long, isEnabled: Boolean) {
        val taskIds = enabledTaskIds().toMutableSet()
        if (isEnabled) {
            taskIds.add(taskId.toString())
        } else {
            taskIds.remove(taskId.toString())
        }
        preferences.edit { putStringSet(KEY_ENABLED_TASK_IDS, taskIds) }
    }

    data class GeofenceOperationResult(
        val isSuccess: Boolean,
        val message: String,
    ) {
        companion object {
            fun success(message: String): GeofenceOperationResult {
                return GeofenceOperationResult(isSuccess = true, message = message)
            }

            fun failure(message: String): GeofenceOperationResult {
                return GeofenceOperationResult(isSuccess = false, message = message)
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "geofence_manager"
        const val KEY_ENABLED_TASK_IDS = "enabled_task_ids"
        const val GEOFENCE_REQUEST_CODE = 2001
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
    }
}
