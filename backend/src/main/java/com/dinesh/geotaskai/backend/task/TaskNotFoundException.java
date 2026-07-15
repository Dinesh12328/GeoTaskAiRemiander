package com.dinesh.geotaskai.backend.task;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(long taskId) {
        super("Task not found: " + taskId);
    }
}
