package com.example.geotaskaireminder.ui

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.geotaskaireminder.model.TaskEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceHelper(context: Context) {
    private val appContext = context.applicationContext
    private val geofencingClient = LocationServices.getGeofencingClient(appContext)
    private val permissionHelper = PermissionHelper(appContext)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            appContext,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    fun hasForegroundLocationPermission(): Boolean {
        return permissionHelper.hasForegroundLocation()
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return permissionHelper.hasBackgroundLocation()
    }

    fun canRegisterGeofence(): Boolean {
        return permissionHelper.canRegisterGeofence()
    }

    @SuppressLint("MissingPermission")
    fun registerTaskGeofence(task: TaskEntity): Boolean {
        if (!canRegisterGeofence()) return false

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

        return try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun removeTaskGeofence(taskId: Long) {
        geofencingClient.removeGeofences(listOf(taskId.toString()))
    }

    companion object {
        private const val GEOFENCE_REQUEST_CODE = 2001
    }
}
