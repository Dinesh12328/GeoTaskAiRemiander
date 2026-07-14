package com.dinesh.geotaskai.location

import android.content.Context
import com.dinesh.geotaskai.R
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofenceStatusCodes

object GeofenceErrorMessages {
    fun fromGeofenceStatusCode(context: Context, errorCode: Int): String {
        return when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> {
                context.getString(R.string.geofence_error_not_available)
            }
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> {
                context.getString(R.string.geofence_error_too_many_geofences)
            }
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> {
                context.getString(R.string.geofence_error_too_many_pending_intents)
            }
            else -> context.getString(R.string.geofence_error_unknown, errorCode)
        }
    }

    fun fromException(context: Context, throwable: Throwable): String {
        val apiException = throwable as? ApiException
        return if (apiException != null) {
            fromGeofenceStatusCode(context, apiException.statusCode)
        } else {
            throwable.localizedMessage ?: context.getString(R.string.geofence_registration_failed)
        }
    }

    fun fromGooglePlayServicesCode(context: Context, errorCode: Int): String {
        val statusText = GoogleApiAvailability.getInstance().getErrorString(errorCode)
        return context.getString(R.string.google_play_services_error, statusText)
    }
}
