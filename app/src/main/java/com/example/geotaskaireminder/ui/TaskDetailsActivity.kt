package com.example.geotaskaireminder.ui

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.geotaskaireminder.R
import com.example.geotaskaireminder.data.TaskInput
import com.example.geotaskaireminder.databinding.ActivityTaskDetailsBinding
import com.example.geotaskaireminder.model.TaskEntity
import com.example.geotaskaireminder.viewmodel.TaskViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class TaskDetailsActivity : ComponentActivity() {
    private lateinit var binding: ActivityTaskDetailsBinding
    private val viewModel: TaskViewModel by viewModels()
    private var currentTask: TaskEntity? = null
    private var hasFilledForm = false
    private lateinit var notificationHelper: NotificationHelper
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            currentTask?.let(notificationHelper::showTaskNotification)
        } else {
            Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        notificationHelper = NotificationHelper(this)

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        if (taskId <= 0L) {
            finish()
            return
        }

        binding.backButton.setOnClickListener { finish() }
        binding.saveTaskButton.setOnClickListener {
            val task = currentTask ?: return@setOnClickListener
            readTaskInput()?.let {
                viewModel.updateTask(task, it)
                finish()
            }
        }
        binding.deleteTaskButton.setOnClickListener {
            currentTask?.let {
                GeofenceHelper(this).removeTaskGeofence(it.id)
                viewModel.deleteTask(it)
                finish()
            }
        }
        binding.testNotificationButton.setOnClickListener {
            showTestNotification()
        }

        viewModel.getTask(taskId).observe(this) { task ->
            if (task == null) {
                finish()
            } else {
                currentTask = task
                if (!hasFilledForm) {
                    fillForm(task)
                    hasFilledForm = true
                }
            }
        }
    }

    private fun showTestNotification() {
        val task = currentTask ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationHelper.showTaskNotification(task)
        }
    }

    private fun fillForm(task: TaskEntity) {
        binding.titleInput.setText(task.title)
        binding.notesInput.setText(task.notes)
        binding.locationNameInput.setText(task.locationName)
        binding.priorityInput.setText(task.priority)
        binding.latitudeInput.setText(String.format(Locale.US, "%.6f", task.latitude))
        binding.longitudeInput.setText(String.format(Locale.US, "%.6f", task.longitude))
        binding.radiusInput.setText(String.format(Locale.US, "%.0f", task.radiusMeters))
        binding.createdAtValue.text = task.createdAt.formatTimestamp()
        binding.updatedAtValue.text = task.updatedAt.formatTimestamp()
    }

    private fun readTaskInput(): TaskInput? {
        clearErrors()

        val title = binding.titleInput.text.toString()
        val notes = binding.notesInput.text.toString()
        val locationName = binding.locationNameInput.text.toString()
        val priority = binding.priorityInput.text.toString().ifBlank { getString(R.string.default_priority) }
        val latitude = binding.latitudeInput.readDouble(R.string.latitude_error)
        val longitude = binding.longitudeInput.readDouble(R.string.longitude_error)
        val radius = binding.radiusInput.readDouble(R.string.radius_error)

        var valid = true
        if (title.isBlank()) {
            binding.titleInput.error = getString(R.string.title_required_error)
            valid = false
        }
        if (locationName.isBlank()) {
            binding.locationNameInput.error = getString(R.string.location_required_error)
            valid = false
        }
        if (latitude == null || latitude !in -90.0..90.0) {
            binding.latitudeInput.error = getString(R.string.latitude_error)
            valid = false
        }
        if (longitude == null || longitude !in -180.0..180.0) {
            binding.longitudeInput.error = getString(R.string.longitude_error)
            valid = false
        }
        if (radius == null || radius <= 0.0) {
            binding.radiusInput.error = getString(R.string.radius_error)
            valid = false
        }

        return if (valid) {
            TaskInput(
                title = title,
                notes = notes,
                locationName = locationName,
                priority = priority,
                latitude = latitude!!,
                longitude = longitude!!,
                radiusMeters = radius!!,
            )
        } else {
            null
        }
    }

    private fun clearErrors() {
        binding.titleInput.error = null
        binding.locationNameInput.error = null
        binding.latitudeInput.error = null
        binding.longitudeInput.error = null
        binding.radiusInput.error = null
    }

    private fun EditText.readDouble(errorRes: Int): Double? {
        return text.toString().trim().toDoubleOrNull().also {
            if (it == null) error = getString(errorRes)
        }
    }

    private fun Long.formatTimestamp(): String {
        if (this <= 0L) return getString(R.string.not_available)
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(this))
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
