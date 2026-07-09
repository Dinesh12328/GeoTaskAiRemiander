package com.example.geotaskaireminder.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.geotaskaireminder.model.TaskEntity;

import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY updated_at DESC")
    LiveData<List<TaskEntity>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    LiveData<TaskEntity> getTaskById(long taskId);

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    TaskEntity getTaskByIdSync(long taskId);

    @Insert
    long insert(TaskEntity task);

    @Update
    int update(TaskEntity task);

    @Delete
    int delete(TaskEntity task);

    @Query("DELETE FROM tasks WHERE id = :taskId")
    int deleteById(long taskId);

    @Query("UPDATE tasks SET reminder_triggered = :isTriggered, updated_at = :updatedAt WHERE id = :taskId")
    int updateReminderTriggered(long taskId, boolean isTriggered, long updatedAt);
}
