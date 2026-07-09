package com.example.geotaskaireminder.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.geotaskaireminder.R
import com.example.geotaskaireminder.databinding.ActivityMapPickerBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapPickerActivity : ComponentActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapPickerBinding
    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null
    private var selectedLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        binding.cancelMapButton.setOnClickListener { finish() }
        binding.confirmMapButton.setOnClickListener { confirmSelection() }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map.apply {
            uiSettings.isZoomControlsEnabled = true
            setOnMapClickListener(::selectLocation)
            setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) = Unit
                override fun onMarkerDrag(marker: Marker) = Unit
                override fun onMarkerDragEnd(marker: Marker) {
                    selectLocation(marker.position)
                }
            })
        }

        val initialLocation = initialLocation()
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, DEFAULT_ZOOM))
        if (intent.hasExtra(EXTRA_LATITUDE) && intent.hasExtra(EXTRA_LONGITUDE)) {
            selectLocation(initialLocation)
        }
    }

    private fun initialLocation(): LatLng {
        val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, DEFAULT_LATITUDE)
        val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, DEFAULT_LONGITUDE)
        return LatLng(latitude, longitude)
    }

    private fun selectLocation(latLng: LatLng) {
        selectedLatLng = latLng
        marker?.remove()
        marker = googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(getString(R.string.selected_location))
                .draggable(true),
        )
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        binding.selectedLocationText.text = getString(
            R.string.selected_coordinates,
            latLng.latitude.formatCoordinate(),
            latLng.longitude.formatCoordinate(),
        )
        binding.confirmMapButton.isEnabled = true
    }

    private fun confirmSelection() {
        val latLng = selectedLatLng ?: return
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_LATITUDE, latLng.latitude)
                .putExtra(EXTRA_LONGITUDE, latLng.longitude),
        )
        finish()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.mapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        binding.mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    private fun Double.formatCoordinate(): String = String.format(Locale.US, "%.6f", this)

    companion object {
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        private const val DEFAULT_LATITUDE = 20.5937
        private const val DEFAULT_LONGITUDE = 78.9629
        private const val DEFAULT_ZOOM = 5f
    }
}
