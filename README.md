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
- Android Geofencing API registration on task creation
- Geofence enter notifications
- Gemini task parser for natural language task entry
- Google Maps location picker for latitude and longitude selection

Not included yet: a Spring Boot backend.

## Structure

- `data`: input models used by the ViewModel and repository.
- `model`: Room task entity.
- `dao`: Room DAO for task queries and CRUD operations.
- `database`: Room database singleton.
- `repository`: local data access layer.
- `viewmodel`: task ViewModel.
- `ai`: Gemini parsing service and parsed task model.
- `ui`: XML-backed activities for list, create, and details.

## Gemini Setup

Create or update `local.properties` in the project root:

```properties
GEMINI_API_KEY=your_api_key_here
MAPS_API_KEY=your_maps_api_key_here
```

The Gradle build reads the Gemini key into `BuildConfig.GEMINI_API_KEY` and passes the Maps key to the manifest as `MAPS_API_KEY`. Do not commit `local.properties`.

## Maps Setup

In Google Cloud Console, enable **Maps SDK for Android** for your project, create an Android-restricted API key, and place it in `local.properties` as `MAPS_API_KEY`.

## Run

Open this folder in Android Studio, let Gradle sync, and run the `app` configuration.

Command-line build:

```powershell
.\gradlew.bat :app:assembleDebug
```
