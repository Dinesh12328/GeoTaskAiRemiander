package com.dinesh.geotaskai.backend.task;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    private final Map<Long, TaskRecord> tasks = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1L);
    private final Clock clock;

    public TaskService(Clock clock) {
        this.clock = clock;
    }

    public List<TaskResponse> findAll() {
        return tasks.values().stream()
            .sorted(Comparator.comparingLong(TaskRecord::id))
            .map(TaskResponse::from)
            .toList();
    }

    public TaskResponse findById(long taskId) {
        return TaskResponse.from(findRecord(taskId));
    }

    public TaskResponse create(TaskRequest request) {
        long id = nextId.getAndIncrement();
        Instant now = clock.instant();
        TaskRecord task = new TaskRecord(
            id,
            clean(request.title()),
            clean(request.notes()),
            clean(request.locationName()),
            normalizePriority(request.priority()),
            request.latitude(),
            request.longitude(),
            request.radiusMeters(),
            false,
            now,
            now
        );
        tasks.put(id, task);
        return TaskResponse.from(task);
    }

    public TaskResponse update(long taskId, TaskRequest request) {
        TaskRecord existing = findRecord(taskId);
        TaskRecord updated = new TaskRecord(
            existing.id(),
            clean(request.title()),
            clean(request.notes()),
            clean(request.locationName()),
            normalizePriority(request.priority()),
            request.latitude(),
            request.longitude(),
            request.radiusMeters(),
            existing.reminderTriggered(),
            existing.createdAt(),
            clock.instant()
        );
        tasks.put(taskId, updated);
        return TaskResponse.from(updated);
    }

    public TaskResponse markReminderTriggered(long taskId) {
        TaskRecord existing = findRecord(taskId);
        TaskRecord updated = new TaskRecord(
            existing.id(),
            existing.title(),
            existing.notes(),
            existing.locationName(),
            existing.priority(),
            existing.latitude(),
            existing.longitude(),
            existing.radiusMeters(),
            true,
            existing.createdAt(),
            clock.instant()
        );
        tasks.put(taskId, updated);
        return TaskResponse.from(updated);
    }

    public void delete(long taskId) {
        if (tasks.remove(taskId) == null) {
            throw new TaskNotFoundException(taskId);
        }
    }

    private TaskRecord findRecord(long taskId) {
        TaskRecord task = tasks.get(taskId);
        if (task == null) {
            throw new TaskNotFoundException(taskId);
        }
        return task;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePriority(String priority) {
        return switch (clean(priority).toLowerCase()) {
            case "high" -> "High";
            case "low" -> "Low";
            default -> "Medium";
        };
    }
}
