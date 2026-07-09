package com.example.geotaskaireminder.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.geotaskaireminder.database.AppDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.util.concurrent.Executors

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return
        if (geofencingEvent.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val pendingResult = goAsync()
        Executors.newSingleThreadExecutor().execute {
            try {
                val taskDao = AppDatabase.getDatabase(context).taskDao()
                val notificationHelper = NotificationHelper(context)

                geofencingEvent.triggeringGeofences.orEmpty().forEach { geofence ->
                    val taskId = geofence.requestId.toLongOrNull() ?: return@forEach
                    val task = taskDao.getTaskByIdSync(taskId) ?: return@forEach
                    if (task.isReminderTriggered) return@forEach

                    val notificationShown = notificationHelper.showTaskNotification(task)
                    if (notificationShown) {
                        taskDao.updateReminderTriggered(taskId, true, System.currentTimeMillis())
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
