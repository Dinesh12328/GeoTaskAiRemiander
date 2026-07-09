import java.util.Properties

fun String.toBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY", "")
val mapsApiKey = localProperties.getProperty("MAPS_API_KEY", "")

plugins {
    id("androidx.room")
    id("com.android.application")
}

android {
    namespace = "com.example.geotaskaireminder"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.geotaskaireminder"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "GEMINI_API_KEY", geminiApiKey.toBuildConfigString())
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    annotationProcessor("androidx.room:room-compiler:2.8.4")
}
