package com.example.geotaskaireminder.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import com.example.geotaskaireminder.R
import com.example.geotaskaireminder.ai.AiTaskParserService
import com.example.geotaskaireminder.data.TaskInput
import com.example.geotaskaireminder.databinding.ActivityTaskCreateBinding
import com.example.geotaskaireminder.viewmodel.TaskViewModel
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AddTaskActivity : ComponentActivity() {
    private lateinit var binding: ActivityTaskCreateBinding
    private val viewModel: TaskViewModel by viewModels()
    private val aiParserService = AiTaskParserService()
    private val aiExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var geofenceHelper: GeofenceHelper
    private lateinit var permissionHelper: PermissionHelper
    private var pendingTaskInput: TaskInput? = null

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val latitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, Double.NaN)
            val longitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, Double.NaN)
            if (!latitude.isNaN() && !longitude.isNaN()) {
                binding.latitudeInput.setText(String.format(Locale.US, "%.6f", latitude))
                binding.longitudeInput.setText(String.format(Locale.US, "%.6f", longitude))
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_SHORT).show()
        }
        continueWithForegroundLocationPermission()
    }

    private val foregroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            continueWithBackgroundLocationPermission()
        } else {
            saveTaskWithoutGeofence(R.string.location_permission_denied)
        }
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            createTaskAndRegisterGeofence()
        } else {
            saveTaskWithoutGeofence(R.string.background_location_permission_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geofenceHelper = GeofenceHelper(this)
        permissionHelper = PermissionHelper(this)

        binding.priorityInput.setText(R.string.default_priority)
        binding.cancelButton.setOnClickListener { finish() }
        binding.parseAiButton.setOnClickListener { parseNaturalLanguageTask() }
        binding.pickOnMapButton.setOnClickListener { openMapPicker() }
        binding.saveTaskButton.setOnClickListener {
            readTaskInput()?.let {
                pendingTaskInput = it
                continueWithNotificationPermission()
            }
        }
    }

    private fun openMapPicker() {
        val intent = Intent(this, MapPickerActivity::class.java)
        val latitude = binding.latitudeInput.text.toString().trim().toDoubleOrNull()
        val longitude = binding.longitudeInput.text.toString().trim().toDoubleOrNull()
        if (latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0) {
            intent.putExtra(MapPickerActivity.EXTRA_LATITUDE, latitude)
            intent.putExtra(MapPickerActivity.EXTRA_LONGITUDE, longitude)
        }
        mapPickerLauncher.launch(intent)
    }

    override fun onDestroy() {
        aiExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun parseNaturalLanguageTask() {
        val input = binding.naturalLanguageInput.text.toString()
        if (input.isBlank()) {
            binding.naturalLanguageInput.error = getString(R.string.ai_input_required)
            return
        }

        binding.parseAiButton.isEnabled = false
        binding.aiParseStatus.text = getString(R.string.ai_parse_loading)

        aiExecutor.execute {
            val result = aiParserService.parseTask(input)
            runOnUiThread {
                binding.parseAiButton.isEnabled = true
                result.onSuccess { parsed ->
                    binding.titleInput.setText(parsed.title)
                    binding.notesInput.setText(parsed.description)
                    binding.locationNameInput.setText(parsed.locationName)
                    binding.priorityInput.setText(parsed.priority)
                    binding.aiParseStatus.text = getString(R.string.ai_parse_success)
                }.onFailure { error ->
                    binding.aiParseStatus.text = error.message ?: getString(R.string.ai_parse_failed)
                    Toast.makeText(this, R.string.ai_parse_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun continueWithNotificationPermission() {
        if (permissionHelper.hasNotificationPermission()) {
            continueWithForegroundLocationPermission()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun continueWithForegroundLocationPermission() {
        if (permissionHelper.hasForegroundLocation()) {
            continueWithBackgroundLocationPermission()
        } else {
            foregroundLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    private fun continueWithBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || permissionHelper.hasBackgroundLocation()) {
            createTaskAndRegisterGeofence()
        } else {
            showBackgroundLocationExplanation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showBackgroundLocationExplanation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.background_location_dialog_title)
            .setMessage(
                getString(
                    R.string.background_location_dialog_message,
                    backgroundLocationOptionLabel(),
                ),
            )
            .setPositiveButton(R.string.continue_label) { _, _ ->
                backgroundLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                )
            }
            .setNegativeButton(R.string.not_now) { _, _ ->
                saveTaskWithoutGeofence(R.string.background_location_permission_denied)
            }
            .show()
    }

    private fun backgroundLocationOptionLabel(): String {
        return getString(R.string.allow_all_the_time)
    }

    private fun createTaskAndRegisterGeofence() {
        val input = pendingTaskInput ?: return
        pendingTaskInput = null

        viewModel.createTask(input) { task ->
            runOnUiThread {
                val registered = geofenceHelper.registerTaskGeofence(task)
                if (registered) {
                    Toast.makeText(this, R.string.geofence_registered, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        R.string.geofence_registration_skipped,
                        Toast.LENGTH_LONG,
                    ).show()
                }
                finish()
            }
        }
    }

    private fun saveTaskWithoutGeofence(messageRes: Int) {
        val input = pendingTaskInput ?: return
        pendingTaskInput = null

        viewModel.createTask(input) {
            runOnUiThread {
                Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show()
                finish()
            }
        }
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
        binding.naturalLanguageInput.error = null
    }

    private fun EditText.readDouble(errorRes: Int): Double? {
        return text.toString().trim().toDoubleOrNull().also {
            if (it == null) error = getString(errorRes)
        }
    }
}
