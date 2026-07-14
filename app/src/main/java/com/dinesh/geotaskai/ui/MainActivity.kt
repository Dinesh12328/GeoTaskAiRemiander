package com.dinesh.geotaskai.ui

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dinesh.geotaskai.R
import com.dinesh.geotaskai.data.TaskEntity
import com.dinesh.geotaskai.databinding.ActivityTaskListBinding
import com.dinesh.geotaskai.databinding.ItemTaskBinding
import com.dinesh.geotaskai.viewmodel.TaskViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityTaskListBinding
    private val viewModel: TaskViewModel by viewModels()
    private val taskAdapter = TaskAdapter { task ->
        startActivity(
            Intent(this, TaskDetailsActivity::class.java).putExtra(
                TaskDetailsActivity.EXTRA_TASK_ID,
                task.id,
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.taskRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.taskRecyclerView.adapter = taskAdapter

        binding.createTaskButton.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        viewModel.allTasks.observe(this) { tasks ->
            renderTasks(tasks.orEmpty())
        }
    }

    private fun renderTasks(tasks: List<TaskEntity>) {
        binding.taskCount.text = resources.getQuantityString(
            R.plurals.task_count,
            tasks.size,
            tasks.size,
        )
        binding.emptyState.isVisible = tasks.isEmpty()
        taskAdapter.submitTasks(tasks)
    }

    private fun Double.formatCoordinate(): String = String.format(Locale.US, "%.5f", this)

    private fun Double.formatRadius(): String = String.format(Locale.US, "%.0f", this)

    private inner class TaskAdapter(
        private val onTaskClick: (TaskEntity) -> Unit,
    ) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
        private val tasks = mutableListOf<TaskEntity>()

        fun submitTasks(newTasks: List<TaskEntity>) {
            val oldTasks = tasks.toList()
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldTasks.size

                override fun getNewListSize(): Int = newTasks.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldTasks[oldItemPosition].id == newTasks[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldTasks[oldItemPosition] == newTasks[newItemPosition]
                }
            })
            tasks.clear()
            tasks.addAll(newTasks)
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val itemBinding = ItemTaskBinding.inflate(layoutInflater, parent, false)
            return TaskViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            holder.bind(tasks[position])
        }

        override fun getItemCount(): Int = tasks.size

        private inner class TaskViewHolder(
            private val itemBinding: ItemTaskBinding,
        ) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(task: TaskEntity) {
                itemBinding.taskTitle.text = task.title.orEmpty()
                itemBinding.taskLocation.text = task.locationName.orEmpty()
                itemBinding.taskRadius.text = getString(
                    R.string.location_summary,
                    task.latitude.formatCoordinate(),
                    task.longitude.formatCoordinate(),
                    task.radiusMeters.formatRadius(),
                )
                itemBinding.taskPriority.text = task.priority.orEmpty()
                itemBinding.root.setOnClickListener { onTaskClick(task) }
            }
        }
    }
}
