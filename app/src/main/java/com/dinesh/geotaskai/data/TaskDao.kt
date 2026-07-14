package com.dinesh.geotaskai.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {
    @get:Query("SELECT * FROM tasks ORDER BY updated_at DESC")
    val allTasks: LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun getTaskById(taskId: Long): LiveData<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun getTaskByIdSync(taskId: Long): TaskEntity?

    @Insert
    fun insert(task: TaskEntity): Long

    @Update
    fun update(task: TaskEntity): Int

    @Delete
    fun delete(task: TaskEntity): Int

    @Query("DELETE FROM tasks WHERE id = :taskId")
    fun deleteById(taskId: Long): Int

    @Query("UPDATE tasks SET reminder_triggered = :isTriggered, updated_at = :updatedAt WHERE id = :taskId")
    fun updateReminderTriggered(taskId: Long, isTriggered: Boolean, updatedAt: Long): Int
}
