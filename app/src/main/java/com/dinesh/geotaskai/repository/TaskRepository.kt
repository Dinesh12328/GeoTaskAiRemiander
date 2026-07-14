package com.dinesh.geotaskai.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.dinesh.geotaskai.data.TaskInput
import com.dinesh.geotaskai.data.TaskDao
import com.dinesh.geotaskai.data.TaskDatabase
import com.dinesh.geotaskai.data.TaskEntity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TaskRepository(context: Context) {
    private val taskDao: TaskDao = TaskDatabase.getDatabase(context).taskDao()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    val allTasks: LiveData<List<TaskEntity>> = taskDao.allTasks

    fun getTask(taskId: Long): LiveData<TaskEntity?> = taskDao.getTaskById(taskId)

    fun createTask(input: TaskInput, onCreated: (TaskEntity) -> Unit = {}) {
        executor.execute {
            val task = input.toNewEntity().also {
                it.id = taskDao.insert(it)
            }
            onCreated(task)
        }
    }

    fun updateTask(task: TaskEntity, input: TaskInput) {
        executor.execute {
            task.applyInput(input, keepCreatedAt = true)
            taskDao.update(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        executor.execute {
            taskDao.delete(task)
        }
    }

    fun deleteTaskById(taskId: Long) {
        executor.execute {
            taskDao.deleteById(taskId)
        }
    }

    fun markReminderTriggered(taskId: Long) {
        executor.execute {
            taskDao.updateReminderTriggered(taskId, true, System.currentTimeMillis())
        }
    }

    private fun TaskInput.toNewEntity(): TaskEntity {
        return TaskEntity().also {
            it.applyInput(this, keepCreatedAt = false)
        }
    }

    private fun TaskEntity.applyInput(input: TaskInput, keepCreatedAt: Boolean) {
        val now = System.currentTimeMillis()
        title = input.title.trim()
        notes = input.notes.trim()
        locationName = input.locationName.trim()
        priority = input.priority.trim().ifBlank { DEFAULT_PRIORITY }
        latitude = input.latitude
        longitude = input.longitude
        radiusMeters = input.radiusMeters
        if (!keepCreatedAt || createdAt == 0L) {
            createdAt = now
        }
        updatedAt = now
    }

    private companion object {
        const val DEFAULT_PRIORITY = "Medium"
    }
}
