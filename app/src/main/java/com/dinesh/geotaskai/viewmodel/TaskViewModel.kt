package com.dinesh.geotaskai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.dinesh.geotaskai.data.TaskInput
import com.dinesh.geotaskai.data.TaskEntity
import com.dinesh.geotaskai.repository.TaskRepository

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application)

    val allTasks: LiveData<List<TaskEntity>> = repository.allTasks

    fun getTask(taskId: Long): LiveData<TaskEntity?> = repository.getTask(taskId)

    fun createTask(input: TaskInput, onCreated: (TaskEntity) -> Unit = {}) {
        repository.createTask(input, onCreated)
    }

    fun updateTask(task: TaskEntity, input: TaskInput) {
        repository.updateTask(task, input)
    }

    fun deleteTask(task: TaskEntity) {
        repository.deleteTask(task)
    }

    fun deleteTaskById(taskId: Long) {
        repository.deleteTaskById(taskId)
    }

    fun markReminderTriggered(taskId: Long) {
        repository.markReminderTriggered(taskId)
    }
}
