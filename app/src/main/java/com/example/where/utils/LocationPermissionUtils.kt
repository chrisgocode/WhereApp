package com.example.where.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

object LocationPermissionUtils {
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    fun hasLocationPermission(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    @Composable
    fun RequestLocationPermission(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        var permissionRequested by remember { mutableStateOf(false) }
        
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = REQUIRED_PERMISSIONS.all { permission ->
                permissions[permission] == true
            }
            
            if (allGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
        
        DisposableEffect(key1 = true) {
            if (!permissionRequested) {
                launcher.launch(REQUIRED_PERMISSIONS)
                permissionRequested = true
            }
            onDispose { }
        }
    }
}