package com.example.geotaskaireminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.geotaskaireminder.data.TaskInput
import com.example.geotaskaireminder.model.TaskEntity
import com.example.geotaskaireminder.repository.TaskRepository

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application)

    val allTasks: LiveData<List<TaskEntity>> = repository.allTasks

    fun getTask(taskId: Long): LiveData<TaskEntity> = repository.getTask(taskId)

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
