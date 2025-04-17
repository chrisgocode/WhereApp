package com.example.where.ui.screens.map.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.android.gms.maps.MapView

/**
 * Creates and remembers a MapView, and automatically dispatches lifecycle events from Compose’s
 * LifecycleOwner to the MapView.
 * This code is adapted from from GeeksForGeeks
 * ref: https://www.geeksforgeeks.org/android-jetpack-compose-how-to-use-google-maps/
 */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context: Context = LocalContext.current
    // 1) Remember a single MapView instance
    val mapView = remember { MapView(context) }

    // 2) Look up the LifecycleOwner that is hosting this Composable
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // 3) Add an observer that calls the appropriate MapView methods
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {
                    throw IllegalStateException()
                }
            }
        }

        // attach to lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            // clean up
            lifecycle.removeObserver(observer)
        }
    }
    return mapView
}
