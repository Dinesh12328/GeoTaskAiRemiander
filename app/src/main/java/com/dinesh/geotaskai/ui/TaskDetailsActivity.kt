package com.dinesh.geotaskai.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.dinesh.geotaskai.R
import com.dinesh.geotaskai.data.TaskEntity
import com.dinesh.geotaskai.data.TaskInput
import com.dinesh.geotaskai.databinding.ActivityTaskDetailsBinding
import com.dinesh.geotaskai.location.GeofenceManager
import com.dinesh.geotaskai.notification.NotificationHelper
import com.dinesh.geotaskai.viewmodel.TaskViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class TaskDetailsActivity : ComponentActivity() {
    private lateinit var binding: ActivityTaskDetailsBinding
    private val viewModel: TaskViewModel by viewModels()
    private var currentTask: TaskEntity? = null
    private var hasFilledForm = false
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var permissionManager: LocationPermissionManager
    private lateinit var geofenceManager: GeofenceManager

    private val testNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            currentTask?.let(notificationHelper::showTaskNotification)
        } else {
            Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_SHORT).show()
        }
        updatePermissionStatusSection()
    }

    private val locationReminderNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            continueLocationReminderPermissionFlow()
        } else {
            showPermissionMessage(R.string.notification_permission_denied)
            updatePermissionStatusSection()
        }
    }

    private val foregroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (permissionManager.hasForegroundLocationPermission()) {
            continueLocationReminderPermissionFlow()
        } else {
            showPermissionMessage(R.string.foreground_location_permission_denied)
            updatePermissionStatusSection()
        }
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        if (permissionManager.hasBackgroundLocationPermission()) {
            continueLocationReminderPermissionFlow()
        } else {
            showPermissionMessage(R.string.background_location_permission_denied_short)
            updatePermissionStatusSection()
        }
    }

    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        updatePermissionStatusSection()
        if (permissionManager.allLocationReminderPermissionsGranted()) {
            registerLocationReminder()
        } else {
            showPermissionMessage(R.string.location_reminder_permissions_pending)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        notificationHelper = NotificationHelper(this)
        permissionManager = LocationPermissionManager(this)
        geofenceManager = GeofenceManager(this)

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        if (taskId <= 0L) {
            finish()
            return
        }

        binding.backButton.setOnClickListener { finish() }
        binding.saveTaskButton.setOnClickListener {
            val task = currentTask ?: return@setOnClickListener
            readTaskInput()?.let { input ->
                updateTaskAndReminder(task, input)
            }
        }
        binding.deleteTaskButton.setOnClickListener {
            currentTask?.let { task ->
                removeReminderThenDelete(task)
            }
        }
        binding.markCompletedButton.setOnClickListener {
            currentTask?.let { task ->
                markTaskCompleted(task)
            }
        }
        binding.testNotificationButton.setOnClickListener {
            showTestNotification()
        }
        binding.enableLocationReminderButton.setOnClickListener {
            continueLocationReminderPermissionFlow()
        }
        updatePermissionStatusSection()

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

    override fun onResume() {
        super.onResume()
        if (::permissionManager.isInitialized) {
            updatePermissionStatusSection()
        }
    }

    private fun showTestNotification() {
        val task = currentTask ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !permissionManager.hasNotificationPermission()
        ) {
            permissionManager.markNotificationRequested()
            testNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationHelper.showTaskNotification(task)
        }
    }

    private fun fillForm(task: TaskEntity) {
        binding.titleInput.setText(task.title.orEmpty())
        binding.notesInput.setText(task.notes.orEmpty())
        binding.locationNameInput.setText(task.locationName.orEmpty())
        setPrioritySelection(task.priority.orEmpty())
        binding.latitudeInput.setText(String.format(Locale.US, "%.6f", task.latitude))
        binding.longitudeInput.setText(String.format(Locale.US, "%.6f", task.longitude))
        binding.radiusInput.setText(String.format(Locale.US, "%.0f", task.radiusMeters))
        binding.createdAtValue.text = task.createdAt.formatTimestamp()
        binding.updatedAtValue.text = task.updatedAt.formatTimestamp()
        binding.markCompletedButton.isEnabled = !task.reminderTriggered
    }

    private fun continueLocationReminderPermissionFlow() {
        updatePermissionStatusSection()
        when {
            !permissionManager.hasNotificationPermission() -> requestNotificationPermissionForReminder()
            !permissionManager.hasForegroundLocationPermission() -> requestForegroundLocationPermission()
            !permissionManager.hasBackgroundLocationPermission() -> requestBackgroundLocationPermission()
            else -> registerLocationReminder()
        }
    }

    private fun requestNotificationPermissionForReminder() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            continueLocationReminderPermissionFlow()
            return
        }
        if (permissionManager.isNotificationPermanentlyDenied()) {
            showPermissionMessage(R.string.notification_permission_settings_required)
            permissionManager.showNotificationSettingsDialog(::openAppSettings)
        } else {
            permissionManager.markNotificationRequested()
            locationReminderNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestForegroundLocationPermission() {
        if (permissionManager.isForegroundLocationPermanentlyDenied()) {
            showPermissionMessage(R.string.foreground_location_settings_required)
            permissionManager.showForegroundLocationSettingsDialog(::openAppSettings)
        } else {
            permissionManager.showForegroundLocationExplanation {
                permissionManager.markForegroundLocationRequested()
                foregroundLocationPermissionLauncher.launch(permissionManager.foregroundPermissions())
            }
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            continueLocationReminderPermissionFlow()
            return
        }

        if (permissionManager.shouldUseSettingsForBackgroundLocation()) {
            permissionManager.showAndroid11BackgroundLocationEducation(
                onOpenSettings = {
                    permissionManager.markBackgroundLocationRequested()
                    openAppSettings()
                },
                onCancel = {
                    showPermissionMessage(R.string.location_reminder_cancelled)
                    updatePermissionStatusSection()
                },
            )
        } else if (permissionManager.isBackgroundLocationPermanentlyDenied()) {
            showPermissionMessage(R.string.background_location_settings_required)
            permissionManager.showBackgroundLocationSettingsDialog(::openAppSettings)
        } else {
            permissionManager.showAndroid10BackgroundLocationExplanation {
                permissionManager.markBackgroundLocationRequested()
                backgroundLocationPermissionLauncher.launch(permissionManager.backgroundPermission())
            }
        }
    }

    private fun openAppSettings() {
        appSettingsLauncher.launch(permissionManager.appSettingsIntent())
    }

    private fun registerLocationReminder() {
        val task = currentTask ?: return
        val input = readTaskInput() ?: return
        val updatedTask = task.withInput(input)
        currentTask = updatedTask
        viewModel.updateTask(task, input)
        setLocationReminderButtonEnabled(false)
        geofenceManager.registerTaskGeofence(updatedTask) { result ->
            runOnUiThread {
                setLocationReminderButtonEnabled(true)
                showPermissionMessage(
                    if (result.isSuccess) {
                        R.string.location_reminder_enabled
                    } else {
                        R.string.location_reminder_failed
                    },
                )
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                updatePermissionStatusSection()
            }
        }
    }

    private fun updateTaskAndReminder(task: TaskEntity, input: TaskInput) {
        val updatedTask = task.withInput(input)
        currentTask = updatedTask
        viewModel.updateTask(task, input)

        if (!geofenceManager.isTaskReminderEnabled(task.id)) {
            finish()
            return
        }

        setLocationReminderButtonEnabled(false)
        geofenceManager.updateTaskGeofence(updatedTask) { result ->
            runOnUiThread {
                setLocationReminderButtonEnabled(true)
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun removeReminderThenDelete(task: TaskEntity) {
        binding.deleteTaskButton.isEnabled = false
        geofenceManager.removeTaskGeofence(task.id) { result ->
            runOnUiThread {
                if (result.isSuccess) {
                    viewModel.deleteTask(task)
                    finish()
                } else {
                    binding.deleteTaskButton.isEnabled = true
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun markTaskCompleted(task: TaskEntity) {
        binding.markCompletedButton.isEnabled = false
        geofenceManager.removeTaskGeofence(task.id) {
            runOnUiThread {
                viewModel.markReminderTriggered(task.id)
                Toast.makeText(this, R.string.task_completed, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setLocationReminderButtonEnabled(isEnabled: Boolean) {
        binding.enableLocationReminderButton.isEnabled = isEnabled
    }

    private fun TaskEntity.withInput(input: TaskInput): TaskEntity {
        val now = System.currentTimeMillis()
        return copy(
            title = input.title.trim(),
            notes = input.notes.trim(),
            locationName = input.locationName.trim(),
            priority = input.priority.trim().ifBlank { getString(R.string.priority_medium) },
            latitude = input.latitude,
            longitude = input.longitude,
            radiusMeters = input.radiusMeters,
            updatedAt = now,
        )
    }

    private fun showPermissionMessage(messageRes: Int) {
        binding.permissionStatusMessage.text = getString(messageRes)
    }

    private fun updatePermissionStatusSection() {
        binding.notificationPermissionStatus.text = permissionManager.notificationStatusText()
        binding.foregroundLocationPermissionStatus.text = permissionManager.foregroundLocationStatusText()
        binding.backgroundLocationPermissionStatus.text = permissionManager.backgroundLocationStatusText()
    }

    private fun readTaskInput(): TaskInput? {
        clearErrors()

        val title = binding.titleInput.text.toString()
        val notes = binding.notesInput.text.toString()
        val locationName = binding.locationNameInput.text.toString()
        val priority = selectedPriority()
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

    private fun selectedPriority(): String {
        return when (binding.priorityChipGroup.checkedChipId) {
            R.id.lowPriorityChip -> getString(R.string.priority_low)
            R.id.highPriorityChip -> getString(R.string.priority_high)
            else -> getString(R.string.priority_medium)
        }
    }

    private fun setPrioritySelection(priority: String) {
        val chipId = when (priority.trim().lowercase(Locale.US)) {
            getString(R.string.priority_low).lowercase(Locale.US) -> R.id.lowPriorityChip
            getString(R.string.priority_high).lowercase(Locale.US) -> R.id.highPriorityChip
            else -> R.id.mediumPriorityChip
        }
        binding.priorityChipGroup.check(chipId)
    }

    private fun Long.formatTimestamp(): String {
        if (this <= 0L) return getString(R.string.not_available)
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(this))
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
