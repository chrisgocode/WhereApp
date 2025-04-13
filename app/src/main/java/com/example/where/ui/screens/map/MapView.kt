package com.example.where.ui.screens.map

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.where.model.Restaurant
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private const val TAG = "RestaurantMapView"

@Composable
fun RestaurantMapView(
    userLocation: LatLng?,
    restaurants: List<Restaurant>,
    onRestaurantClick: (Restaurant) -> Unit
) {
    // Return early if no user location is available
    if (userLocation == null) {
        Log.e(TAG, "User location is null, cannot display map")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Location not available. Please enable location services.")
        }
        return
    }

    Log.d(
        TAG,
        "Rendering map with user location: $userLocation and ${restaurants.size} restaurants"
    )

    // Remember camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 13f)
    }

    // Map UI settings
    val uiSettings = remember {
        MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true)
    }

    // Map properties
    val mapProperties = remember { MapProperties(isMyLocationEnabled = true) }

    // Effect to update camera position when user location changes
    LaunchedEffect(userLocation) {
        Log.d(TAG, "Updating camera position to $userLocation")
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation, 13f))
    }

    // Map composable
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            // User location marker
            Marker(state = MarkerState(position = userLocation), title = "Your Location")

            // Restaurant markers
            restaurants.forEach { restaurant ->
                Marker(
                    state = MarkerState(position = restaurant.location),
                    title = restaurant.name,
                    snippet = restaurant.category,
                    onClick = {
                        Log.d(TAG, "Restaurant marker clicked: ${restaurant.name}")
                        onRestaurantClick(restaurant)
                        true // Return true to indicate the click was handled
                    }
                )
            }
        }
    }
}
