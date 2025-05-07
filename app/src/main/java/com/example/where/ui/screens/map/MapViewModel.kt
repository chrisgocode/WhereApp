package com.example.where.ui.screens.map

import androidx.lifecycle.ViewModel
import com.example.where.model.Restaurant
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {
    private val _googleMapState = MutableStateFlow<GoogleMap?>(null)
    val googleMapState: StateFlow<GoogleMap?> = _googleMapState.asStateFlow()

    val markerToRestaurant = mutableMapOf<Marker, Restaurant>()

    var currentCameraLatLng: LatLng? = null
    var currentCameraZoom: Float? = null
    var initialCameraMoveDone = false

    fun clearMapState() {
        _googleMapState.value?.clear()
        markerToRestaurant.clear()
    }

    fun updateMarkers(restaurants: List<Restaurant>) {
        _googleMapState.value?.let { map ->
            map.clear() // This clears all markers from the map
            markerToRestaurant.clear()

            restaurants.forEach { r ->
                val markerOptions = MarkerOptions().position(r.location).title(r.name)
                    .snippet("${r.category} • ${r.rating}★ • ${r.distance}")

                val marker = map.addMarker(markerOptions)
                if (marker != null) {
                    markerToRestaurant[marker] = r
                }
            }
        }
    }

    // Call this when the GoogleMap instance is ready
    fun onMapReady(map: GoogleMap) {
        _googleMapState.value = map
    }

    // Call this to clean up when the view is destroyed
    override fun onCleared() {
        super.onCleared()
        _googleMapState.value = null
        markerToRestaurant.clear()
    }
}
