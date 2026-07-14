package com.dinesh.geotaskai.data

data class TaskInput(
    val title: String,
    val notes: String,
    val locationName: String,
    val priority: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)
