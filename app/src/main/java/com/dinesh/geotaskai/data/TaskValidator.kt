package com.dinesh.geotaskai.data

object TaskValidator {
    const val MIN_LATITUDE = -90.0
    const val MAX_LATITUDE = 90.0
    const val MIN_LONGITUDE = -180.0
    const val MAX_LONGITUDE = 180.0

    @JvmStatic
    fun hasRequiredText(value: String?): Boolean {
        return !value.isNullOrBlank()
    }

    @JvmStatic
    fun isValidLatitude(latitude: Double?): Boolean {
        return latitude != null &&
            !latitude.isNaN() &&
            !latitude.isInfinite() &&
            latitude in MIN_LATITUDE..MAX_LATITUDE
    }

    @JvmStatic
    fun isValidLongitude(longitude: Double?): Boolean {
        return longitude != null &&
            !longitude.isNaN() &&
            !longitude.isInfinite() &&
            longitude in MIN_LONGITUDE..MAX_LONGITUDE
    }

    @JvmStatic
    fun isValidRadius(radiusMeters: Double?): Boolean {
        return radiusMeters != null &&
            !radiusMeters.isNaN() &&
            !radiusMeters.isInfinite() &&
            radiusMeters > 0.0
    }

    @JvmStatic
    fun isValidTaskInput(input: TaskInput): Boolean {
        return hasRequiredText(input.title) &&
            hasRequiredText(input.locationName) &&
            isValidLatitude(input.latitude) &&
            isValidLongitude(input.longitude) &&
            isValidRadius(input.radiusMeters)
    }
}
