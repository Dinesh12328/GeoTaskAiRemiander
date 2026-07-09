package com.example.geotaskaireminder.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.example.geotaskaireminder.databinding.ActivityTaskListBinding
import com.example.geotaskaireminder.databinding.ItemTaskBinding
import com.example.geotaskaireminder.model.TaskEntity
import com.example.geotaskaireminder.viewmodel.TaskViewModel
import java.util.Locale

class TaskListActivity : ComponentActivity() {
    private lateinit var binding: ActivityTaskListBinding
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createTaskButton.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        viewModel.allTasks.observe(this) { tasks ->
            renderTasks(tasks.orEmpty())
        }
    }

    private fun renderTasks(tasks: List<TaskEntity>) {
        binding.taskCount.text = resources.getQuantityString(
            com.example.geotaskaireminder.R.plurals.task_count,
            tasks.size,
            tasks.size,
        )
        binding.emptyState.isVisible = tasks.isEmpty()
        binding.taskListContainer.removeAllViews()

        tasks.forEach { task ->
            binding.taskListContainer.addView(createTaskRow(task))
        }
    }

    private fun createTaskRow(task: TaskEntity): View {
        val itemBinding = ItemTaskBinding.inflate(layoutInflater, binding.taskListContainer, false)
        itemBinding.taskTitle.text = task.title
        itemBinding.taskLocation.text = task.locationName
        itemBinding.taskRadius.text = getString(
            com.example.geotaskaireminder.R.string.location_summary,
            task.latitude.formatCoordinate(),
            task.longitude.formatCoordinate(),
            task.radiusMeters.formatRadius(),
        )
        itemBinding.taskPriority.text = getString(
            com.example.geotaskaireminder.R.string.priority_summary,
            task.priority,
        )
        itemBinding.root.setOnClickListener {
            startActivity(
                Intent(this, TaskDetailsActivity::class.java).putExtra(
                    TaskDetailsActivity.EXTRA_TASK_ID,
                    task.id,
                ),
            )
        }
        return itemBinding.root
    }

    private fun Double.formatCoordinate(): String = String.format(Locale.US, "%.5f", this)

    private fun Double.formatRadius(): String = String.format(Locale.US, "%.0f", this)
}
