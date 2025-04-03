package com.example.whereapp.ui.screens.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whereapp.ui.theme.WhereAppTheme

// Define theme colors to match HomeScreen
private val PrimaryPurple = Color(0xFF8A3FFC)
private val DividerColor = Color(0xFFE0E0E0) // Light gray for the divider

@Composable
fun BottomNavBar() {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Home", "Groups", "Meetup", "Profile")
    val icons =
        listOf(
            Icons.Default.Home,
            Icons.Default.Person, // Using Person as a placeholder for Groups
            Icons.Default.LocationOn, // Using LocationOn as a placeholder for Meetup
            Icons.Default.Person
        )

    Column {
        // Thin divider line above the navigation bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )

        NavigationBar(containerColor = Color.White, contentColor = PrimaryPurple) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = item,
                            modifier = Modifier.size(24.dp),
                            tint = if (selectedItem == index) PrimaryPurple else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            text = item,
                            fontSize = 12.sp,
                            fontWeight =
                                if (selectedItem == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedItem == index) PrimaryPurple else Color.Gray
                        )
                    },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index },
                    colors =
                        NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent // Make the indicator invisible
                        )
                )
            }
        }
    }
}

@Preview
@Composable
fun BottomNavBarPreview() {
    WhereAppTheme { Column { BottomNavBar() } }
}
