package com.dinesh.geotaskai.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,

    @ColumnInfo(name = "title")
    var title: String? = "",

    @ColumnInfo(name = "notes")
    var notes: String? = "",

    @ColumnInfo(name = "location_name")
    var locationName: String? = "",

    @ColumnInfo(name = "priority", defaultValue = "'Medium'")
    var priority: String? = "Medium",

    @ColumnInfo(name = "latitude")
    var latitude: Double = 0.0,

    @ColumnInfo(name = "longitude")
    var longitude: Double = 0.0,

    @ColumnInfo(name = "radius_meters")
    var radiusMeters: Double = 0.0,

    @ColumnInfo(name = "created_at")
    var createdAt: Long = 0L,

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = 0L,

    @ColumnInfo(name = "reminder_triggered", defaultValue = "0")
    var reminderTriggered: Boolean = false,
)
