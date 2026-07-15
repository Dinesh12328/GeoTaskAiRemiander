package com.dinesh.geotaskai.backend.task;

import java.time.Instant;

record TaskRecord(
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
}
