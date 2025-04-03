package com.example.whereapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.whereapp.ui.screens.home.HomeScreen
import com.example.whereapp.ui.screens.shared.BottomNavBar
import com.example.whereapp.ui.theme.WhereAppTheme
import com.example.whereapp.utils.LocationPermissionUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request permissions at startup if needed
        if (!LocationPermissionUtils.hasLocationPermission(this)) {
            requestPermissions(
                LocationPermissionUtils.REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        enableEdgeToEdge()
        setContent { WhereAppTheme { WhereApp() } }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}

@Composable
fun WhereApp() {
    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = { BottomNavBar() }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) { HomeScreen() }
    }
}

@Preview(showBackground = true)
@Composable
fun WhereAppPreview() {
    WhereAppTheme { WhereApp() }
}
