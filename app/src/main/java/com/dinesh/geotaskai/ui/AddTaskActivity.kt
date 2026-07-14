package com.dinesh.geotaskai.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.dinesh.geotaskai.R
import com.dinesh.geotaskai.ai.AiTaskParserService
import com.dinesh.geotaskai.data.TaskInput
import com.dinesh.geotaskai.databinding.ActivityTaskCreateBinding
import com.dinesh.geotaskai.viewmodel.TaskViewModel
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AddTaskActivity : ComponentActivity() {
    private lateinit var binding: ActivityTaskCreateBinding
    private val viewModel: TaskViewModel by viewModels()
    private val aiParserService = AiTaskParserService()
    private val aiExecutor: ExecutorService = Executors.newSingleThreadExecutor()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.priorityChipGroup.check(R.id.mediumPriorityChip)
        binding.cancelButton.setOnClickListener { finish() }
        binding.parseAiButton.setOnClickListener { parseNaturalLanguageTask() }
        binding.pickOnMapButton.setOnClickListener { openMapPicker() }
        binding.saveTaskButton.setOnClickListener {
            readTaskInput()?.let {
                createTask(it)
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
                    setPrioritySelection(parsed.priority)
                    binding.aiParseStatus.text = getString(R.string.ai_parse_success)
                }.onFailure { error ->
                    binding.aiParseStatus.text = error.message ?: getString(R.string.ai_parse_failed)
                    Toast.makeText(this, R.string.ai_parse_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun createTask(input: TaskInput) {
        viewModel.createTask(input) {
            runOnUiThread {
                Toast.makeText(this, R.string.task_saved, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
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
        binding.naturalLanguageInput.error = null
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

    private fun EditText.readDouble(errorRes: Int): Double? {
        return text.toString().trim().toDoubleOrNull().also {
            if (it == null) error = getString(errorRes)
        }
    }
}
