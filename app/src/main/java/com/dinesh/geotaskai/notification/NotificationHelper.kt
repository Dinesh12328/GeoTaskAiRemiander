package com.dinesh.geotaskai.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dinesh.geotaskai.R
import com.dinesh.geotaskai.data.TaskEntity
import com.dinesh.geotaskai.ui.TaskDetailsActivity

class NotificationHelper(private val context: Context) {
    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.task_notifications_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.task_notifications_channel_description)
        }

        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun showTaskNotification(task: TaskEntity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val detailsIntent = Intent(context, TaskDetailsActivity::class.java).apply {
            putExtra(TaskDetailsActivity.EXTRA_TASK_ID, task.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toNotificationId(),
            detailsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val content = context.getString(R.string.notification_task_location, task.locationName.orEmpty())

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title.orEmpty())
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(task.id.toNotificationId(), notification)
        } catch (_: SecurityException) {
            return false
        }
        return true
    }

    private fun Long.toNotificationId(): Int {
        return (this % Int.MAX_VALUE).toInt().coerceAtLeast(1)
    }

    companion object {
        private const val CHANNEL_ID = "task_notifications"
    }
}
