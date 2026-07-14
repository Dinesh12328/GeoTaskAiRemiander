package com.dinesh.geotaskai.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dinesh.geotaskai.data.TaskDatabase
import com.dinesh.geotaskai.notification.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.util.concurrent.Executors

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            Log.w(
                TAG,
                GeofenceErrorMessages.fromGeofenceStatusCode(context, geofencingEvent.errorCode),
            )
            return
        }
        if (geofencingEvent.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val pendingResult = goAsync()
        Executors.newSingleThreadExecutor().execute {
            try {
                val taskDao = TaskDatabase.getDatabase(context).taskDao()
                val notificationHelper = NotificationHelper(context)
                val geofenceManager = GeofenceManager(context)

                geofencingEvent.triggeringGeofences.orEmpty().forEach { geofence ->
                    val taskId = geofence.requestId.toLongOrNull()
                    if (taskId == null) {
                        Log.w(TAG, "Ignoring geofence with invalid request id: ${geofence.requestId}")
                        return@forEach
                    }
                    val task = taskDao.getTaskByIdSync(taskId)
                    if (task == null) {
                        geofenceManager.removeTaskGeofence(taskId)
                        Log.w(TAG, "Removing geofence for missing task id: $taskId")
                        return@forEach
                    }
                    if (task.reminderTriggered) return@forEach

                    val notificationShown = notificationHelper.showTaskNotification(task)
                    if (notificationShown) {
                        taskDao.updateReminderTriggered(taskId, true, System.currentTimeMillis())
                        geofenceManager.removeTaskGeofence(taskId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "GeofenceReceiver"
    }
}
