package com.dinesh.geotaskai.backend.task;

import java.time.Instant;

public record TaskResponse(
    long id,
    String title,
    String notes,
    String locationName,
    String priority,
    double latitude,
    double longitude,
    double radiusMeters,
    boolean reminderTriggered,
    Instant createdAt,
    Instant updatedAt
) {
    static TaskResponse from(TaskRecord task) {
        return new TaskResponse(
            task.id(),
            task.title(),
            task.notes(),
            task.locationName(),
            task.priority(),
            task.latitude(),
            task.longitude(),
            task.radiusMeters(),
            task.reminderTriggered(),
            task.createdAt(),
            task.updatedAt()
        );
    }
}
