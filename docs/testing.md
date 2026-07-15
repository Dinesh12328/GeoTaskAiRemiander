# GeoTask AI Reminder Testing Checklist

Use this checklist before submitting or demoing the app.

## Automated Checks

Run these from the project root:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
.\gradlew.bat :backend:test
```

## Setup

Create `local.properties` in the project root when testing AI or Maps:

```properties
GEMINI_API_KEY=your_gemini_api_key_here
MAPS_API_KEY=your_maps_api_key_here
```

Use a real Android device for notification, background location, geofencing, and Maps checks.

## Manual Test Flow

1. Create a task manually with title, description, priority, location name, latitude, longitude, and radius.
2. Confirm the task appears on the task list.
3. Open the task details screen and edit each field.
4. Save changes and confirm the list/details screen shows the updated data.
5. Tap **Test Notification** and confirm a local notification appears with task title and location name.
6. Tap **Enable Location Reminder** and grant notification, foreground location, and background location when requested.
7. Confirm the app shows that the location reminder is enabled.
8. Enter the configured geofence radius on a real device and confirm the notification appears.
9. Tap the geofence notification and confirm it opens the task details screen.
10. Delete the task and confirm it disappears from the list.

## Gemini Parser

1. Add `GEMINI_API_KEY` to `local.properties`.
2. Open **Create Task**.
3. Enter `Remind me to submit assignment when I reach college`.
4. Tap **Generate Task with AI**.
5. Confirm title, description, location name, and priority are filled.
6. Edit the generated fields manually before saving if needed.

## Maps Picker

1. Add `MAPS_API_KEY` to `local.properties`.
2. Open **Create Task**.
3. Tap **Pick on Map**.
4. Tap or drag the marker to select a location.
5. Tap **Use Location**.
6. Confirm latitude and longitude are filled automatically.
7. Edit latitude and longitude manually if needed.

## Permission Edge Cases

1. Deny notification permission and confirm the app does not crash.
2. Deny foreground location and confirm the app shows a clear message.
3. Deny background location and confirm CRUD features still work.
4. Turn off device Location and confirm geofence registration shows a clear failure message.
5. Leave `MAPS_API_KEY` empty and confirm **Pick on Map** shows the missing-key message.

## Optional Backend

1. Run `.\gradlew.bat :backend:test`.
2. Run `.\gradlew.bat :backend:bootRun`.
3. Open `http://localhost:8080/api/tasks` and confirm it returns an empty JSON list.
4. Create a task with `POST /api/tasks`.
5. Confirm `GET /api/tasks` returns the created task.
6. Stop and restart the backend; confirm tasks are cleared because storage is in memory.
