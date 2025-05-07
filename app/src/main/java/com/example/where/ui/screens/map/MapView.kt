package com.example.where.ui.screens.map

import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.where.model.Restaurant
import com.example.where.ui.screens.map.utils.rememberMapViewWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng

@SuppressLint("MissingPermission")
@Composable
fun RestaurantMapView(
    userLocation: LatLng?,
    restaurants: List<Restaurant>,
    onRestaurantClick: (Restaurant) -> Unit,
    onClearClicked: () -> Unit,
    onSearchHereClicked: (center: LatLng, radiusInMeters: Double) -> Unit,
    mapViewModel: MapViewModel = hiltViewModel()
) {
    // Collect the GoogleMap instance from the ViewModel's StateFlow
    val googleMapInstance by mapViewModel.googleMapState.collectAsState()

    if (userLocation == null && mapViewModel.currentCameraLatLng == null) {
        Text(
            "Enable location or previous map view required",
            Modifier.fillMaxSize(),
            textAlign = TextAlign.Center
        )
        return
    }

    val mapView = rememberMapViewWithLifecycle()

    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            mapViewModel.onMapReady(map)

            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true

            if (userLocation != null) {
                map.isMyLocationEnabled = true
            }

            // Restore camera position or move to userLocation
            val camLatLng = mapViewModel.currentCameraLatLng
            val camZoom = mapViewModel.currentCameraZoom

            if (camLatLng != null && camZoom != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(camLatLng, camZoom))
                mapViewModel.initialCameraMoveDone = true
            } else if (userLocation != null && !mapViewModel.initialCameraMoveDone) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13f))
                mapViewModel.initialCameraMoveDone = true
            }

            map.setOnMarkerClickListener { marker ->
                selectedRestaurant = mapViewModel.markerToRestaurant[marker]
                selectedRestaurant?.let(onRestaurantClick)
                false
            }

            map.setOnMapClickListener { selectedRestaurant = null }

            map.setOnCameraIdleListener {
                val position = map.cameraPosition
                mapViewModel.currentCameraLatLng = position.target
                mapViewModel.currentCameraZoom = position.zoom
                mapViewModel.initialCameraMoveDone = true
            }
        }

        onDispose {}
    }

    LaunchedEffect(googleMapInstance, restaurants) {
        googleMapInstance?.let { mapViewModel.updateMarkers(restaurants) }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    mapViewModel.clearMapState()
                    onClearClicked()
                }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, contentColor = Color.DarkGray
                ), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Clear", fontSize = 14.sp) }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    googleMapInstance?.let { map ->
                        val center = map.cameraPosition.target
                        val visibleRegion = map.projection.visibleRegion
                        val farLeft = visibleRegion.farLeft

                        val results = FloatArray(1)
                        Location.distanceBetween(
                            center.latitude,
                            center.longitude,
                            farLeft.latitude,
                            farLeft.longitude,
                            results
                        )
                        val radiusInMeters = results[0].toDouble()
                        onSearchHereClicked(center, radiusInMeters)
                    }
                }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8A3FFC), contentColor = Color.White
                ), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Search here", fontSize = 14.sp) }
        }
    }
}
