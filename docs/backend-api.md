# Optional Spring Boot Backend API

The backend is a standalone optional module. The Android app still works offline with Room and does not require this server.

## Run

From the project root:

```powershell
.\gradlew.bat :backend:bootRun
```

Default URL:

```text
http://localhost:8080
```

## Endpoints

### List Tasks

```http
GET /api/tasks
```

### Get Task

```http
GET /api/tasks/{taskId}
```

### Create Task

```http
POST /api/tasks
Content-Type: application/json
```

```json
{
  "title": "Submit assignment",
  "notes": "Submit assignment when reaching college.",
  "locationName": "College",
  "priority": "Medium",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "radiusMeters": 200.0
}
```

### Update Task

```http
PUT /api/tasks/{taskId}
Content-Type: application/json
```

Use the same JSON body as create.

### Mark Reminder Triggered

```http
PATCH /api/tasks/{taskId}/reminder-triggered
```

### Delete Task

```http
DELETE /api/tasks/{taskId}
```

## Notes

- Data is stored in memory only.
- Restarting the backend clears all backend tasks.
- The backend does not replace the Android Room database.
- No Firebase or external database is used.
