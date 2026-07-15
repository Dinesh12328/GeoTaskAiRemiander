# GeoTask AI Reminder

Android app for local task reminders with manual location fields.

## Scope

- Kotlin Android app
- XML Views with View Binding
- Minimum SDK 26
- MVVM with Jetpack ViewModel and LiveData
- Room database for local task storage
- Task list, task creation, and task details screens
- CRUD operations for tasks
- Manual location fields: location name, latitude, longitude, radius
- Local test notifications
- Android Geofencing API registration from task details
- Geofence enter notifications
- Gemini task parser for natural language task entry
- Google Maps location picker for latitude and longitude selection
- Optional Spring Boot backend with in-memory task CRUD API

The Android app remains offline-first and uses Room locally. The Spring Boot backend is optional and is not required for normal app use.

## Structure

- `data`: Room entity, DAO, database, and task input model.
- `repository`: local data access layer.
- `viewmodel`: task ViewModel.
- `ai`: Gemini parsing service and parsed task model.
- `location`: geofence manager, receiver, and error messages.
- `notification`: local notification helper.
- `ui`: XML-backed activities for list, create, and details.
- `backend`: optional Spring Boot REST API module.

## Gemini Setup

Create or update `local.properties` in the project root:

```properties
GEMINI_API_KEY=your_api_key_here
MAPS_API_KEY=your_maps_api_key_here
```

The Gradle build reads the Gemini key into `BuildConfig.GEMINI_API_KEY`. Do not commit `local.properties`.

## Maps Setup

In Google Cloud Console:

1. Enable **Maps SDK for Android**.
2. Create an API key.
3. Restrict the key to Android apps using this package name:

```text
com.dinesh.geotaskai
```

4. Add the app's SHA-1 certificate fingerprint for the debug or release keystore you are using.
5. Place the key in `local.properties`:

```properties
MAPS_API_KEY=your_maps_api_key_here
```

Gradle passes the key to the Android manifest as `MAPS_API_KEY` for Google Maps and also exposes it as `BuildConfig.MAPS_API_KEY` so the app can show a clear message when the key is missing.

## Test Maps Picker

1. Add `MAPS_API_KEY` to `local.properties`.
2. Sync Gradle or rebuild the app.
3. Open **Create Task**.
4. Tap **Pick on Map**.
5. Tap a location on the map.
6. Tap **Use Location**.
7. Confirm latitude and longitude are filled automatically.
8. Edit latitude and longitude manually if needed before saving.

## Testing

Automated checks:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
.\gradlew.bat :backend:test
```

Manual testing checklist: [docs/testing.md](docs/testing.md)

## Optional Backend

Run the Spring Boot backend:

```powershell
.\gradlew.bat :backend:bootRun
```

The backend starts on:

```text
http://localhost:8080
```

API documentation: [docs/backend-api.md](docs/backend-api.md)

## Run

Open this folder in Android Studio, let Gradle sync, and run the `app` configuration.

Command-line build:

```powershell
.\gradlew.bat :app:assembleDebug
```
