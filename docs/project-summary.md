# GeoTask AI Reminder Project Summary

GeoTask AI Reminder is an offline-first Android app for task reminders with manual and map-based location input, local notifications, geofencing, and optional AI parsing.

## Completed Phases

| Phase | Status | Summary |
| --- | --- | --- |
| Phase 1 | Complete | Kotlin Android app, XML Views, MVVM, Room database, task CRUD, list, create, and details screens |
| Phase 2 | Complete | Local notifications, Android 13 notification permission, notification channel, test notification |
| Phase 3 | Complete | Location permission handling for foreground/background location and notification status display |
| Phase 4 | Complete | Android Geofencing API support, enter transition notifications, geofence update/delete handling |
| Phase 5 | Complete | Gemini AI task parser using `GEMINI_API_KEY` from `local.properties` / `BuildConfig` |
| Phase 6 | Complete | Google Maps location picker using `MAPS_API_KEY` from `local.properties` / manifest placeholder |
| Phase 7 | Complete | Unit tests, shared task validation, README updates, manual testing checklist |
| Phase 8 | Complete | Optional Spring Boot backend with in-memory task CRUD REST API |

## Android App

- Package: `com.dinesh.geotaskai`
- Min SDK: 26
- UI: XML Views with View Binding
- Architecture: MVVM
- Local database: Room
- Main screens:
  - Task list
  - Add task
  - Task details
  - Map picker

## Optional Backend

- Module: `backend`
- Framework: Spring Boot
- Storage: in-memory only
- Base URL when running locally: `http://localhost:8080`
- API docs: [backend-api.md](backend-api.md)

## Local Properties

Create `local.properties` in the project root when testing AI or Maps:

```properties
GEMINI_API_KEY=your_gemini_api_key_here
MAPS_API_KEY=your_maps_api_key_here
```

Do not commit `local.properties`.

## Verification Commands

Run from the project root:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
.\gradlew.bat :backend:test
.\gradlew.bat :backend:bootJar
```

## Output Locations

- Android debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Backend jar: `backend/build/libs/backend-0.1.0.jar`

## Testing

Manual testing checklist: [testing.md](testing.md)
