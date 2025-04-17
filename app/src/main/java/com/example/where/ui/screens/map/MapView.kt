package com.example.where.ui.screens.map

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import com.example.where.model.Restaurant
import com.example.where.ui.screens.map.utils.rememberMapViewWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

@SuppressLint("MissingPermission")
@Composable
fun RestaurantMapView(
        userLocation: LatLng?,
        restaurants: List<Restaurant>,
        onRestaurantClick: (Restaurant) -> Unit
) {
    if (userLocation == null) {
        Text("Enable location", Modifier.fillMaxSize(), textAlign = TextAlign.Center)
        return
    }

    val mapView = rememberMapViewWithLifecycle()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val markerToRestaurant = remember { mutableMapOf<Marker, Restaurant>() }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    DisposableEffect(mapView) {
        // this will run exactly once when mapView first enters composition
        mapView.getMapAsync { map ->
            googleMap = map

            // UI & permissions
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            map.isMyLocationEnabled = true

            // when the map content is ready, move camera
            map.setOnMapLoadedCallback {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13f))
            }

            // Handle marker taps
            map.setOnMarkerClickListener { m ->
                selectedRestaurant = markerToRestaurant[m]
                selectedRestaurant?.let(onRestaurantClick)
                false // Return false to show default info window
            }

            // Handle map taps
            map.setOnMapClickListener { selectedRestaurant = null }
        }

        onDispose {
            // Clean up map resources
            markerToRestaurant.clear()

            // Stop the map view to prevent leaks
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(googleMap, restaurants) {
        googleMap?.let { map ->
            map.clear()
            markerToRestaurant.clear()
            restaurants.forEach { r ->
                val markerOptions =
                        MarkerOptions()
                                .position(r.location)
                                .title(r.name)
                                .snippet("${r.category} • ${r.rating}★ • ${r.distance}")

                val marker = map.addMarker(markerOptions)!!
                markerToRestaurant[marker] = r
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
    }
}
