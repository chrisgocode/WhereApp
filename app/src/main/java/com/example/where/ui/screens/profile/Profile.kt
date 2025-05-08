package com.example.where.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.where.model.UserPreferences
import com.example.where.ui.screens.shared.BottomNavBar
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

// Theme colors
val PrimaryPurple = Color(0xFF8A3FFC)
val BackgroundWhite = Color(0xFFFAFAFA)
val DarkGray = Color(0xFF333333)
val LightGray = Color(0xFFE0E0E0)
val LighterPurple = Color(0xFFF6F2FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    navController: NavController,
    onSignOut: () -> Unit,
    onNavItemClick: (String) -> Unit = {}
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
    val profileImageUrl by viewModel.profileImageUrl.collectAsState()
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val horizontalPadding = if (isTablet) 32.dp else 16.dp
    val contentWidth = if (isTablet) 600.dp else Dp.Unspecified

    // Dialog states
    var showCuisineDialog by remember { mutableStateOf(false) }
    var showRestrictionsDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Image picker
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onImagePickerRequested()
        } else {
            showPermissionRationale = true
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.uploadProfileImage(it, context) }
    }

    // Hide keyboard when not needed
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }

    Scaffold(
        bottomBar = { BottomNavBar(selectedRoute = "profile", onNavItemClick = onNavItemClick) },
        content = { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = BackgroundWhite
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryPurple)
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontalPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "An error occurred",
                            color = Color.Red,
                            fontSize = if (isTablet) 18.sp else 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refreshUserData() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Text("Retry", fontSize = if (isTablet) 16.sp else 14.sp)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = horizontalPadding)
                            .then(
                                if (isTablet) Modifier.widthIn(max = contentWidth) else Modifier
                            )
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                                )
                                Text(
                                    text = "Profile",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isTablet) 22.sp else 20.sp,
                                    color = DarkGray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 24.dp))
                        ProfileHeader(
                            name = viewModel.userName.value,
                            location = viewModel.userLocation.value,
                            profileImageUrl = profileImageUrl,
                            isTablet = isTablet,
                            onImagePick = {
                                if (ContextCompat.checkSelfPermission(
                                        context, permission
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    viewModel.onImagePickerRequested()
                                    imagePicker.launch(
                                        ActivityResultContracts.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                } else {
                                    launcher.launch(permission)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 24.dp))
                        PreferencesSection(
                            title = "Food Preferences",
                            onUpdate = { showCuisineDialog = true },
                            isTablet = isTablet
                        ) {
                            PreferenceItem(
                                label = "Favorite Cuisine",
                                value = userPreferences?.cuisinePreferences?.firstOrNull() ?: "None",
                                isTablet = isTablet
                            )
                            PreferenceItem(
                                label = "Price Range",
                                value = when (userPreferences?.priceRange) {
                                    1 -> "$  Inexpensive"
                                    2 -> "$$  Moderately priced"
                                    3 -> "$$$  Expensive"
                                    4 -> "$$$$  Very expensive"
                                    else -> "Not set"
                                },
                                isTablet = isTablet
                            )
                        }
                        Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 24.dp))
                        PreferencesSection(
                            title = "Dietary Restrictions",
                            onUpdate = { showRestrictionsDialog = true },
                            isTablet = isTablet
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                if (userPreferences?.dietaryRestrictions?.isNotEmpty() == true) {
                                    userPreferences!!.dietaryRestrictions.forEach { restriction ->
                                        RestrictionChip(text = restriction, isTablet = isTablet)
                                    }
                                } else {
                                    RestrictionChip(text = "None", isTablet = isTablet)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isTablet) 56.dp else 48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color.Red)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Log Out",
                                    tint = Color.Red,
                                    modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Log Out",
                                    color = Color.Red,
                                    fontSize = if (isTablet) 18.sp else 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    )

    // Permission rationale dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Permission Required", fontSize = if (isTablet) 20.sp else 18.sp) },
            text = {
                Text(
                    "This app needs access to your gallery to select a profile picture.",
                    fontSize = if (isTablet) 16.sp else 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        launcher.launch(permission)
                        showPermissionRationale = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryPurple)
                ) {
                    Text("Grant", fontSize = if (isTablet) 16.sp else 14.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionRationale = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Cancel", fontSize = if (isTablet) 16.sp else 14.sp)
                }
            }
        )
    }

    // Cuisine Preferences Dialog
    if (showCuisineDialog) {
        CuisinePreferencesDialog(
            currentPreferences = userPreferences,
            onDismiss = { showCuisineDialog = false },
            onSave = { updatedCuisines, updatedPriceRange ->
                viewModel.updateUserPreferences(
                    userPreferences?.copy(
                        cuisinePreferences = updatedCuisines,
                        priceRange = updatedPriceRange
                    ) ?: UserPreferences(
                        cuisinePreferences = updatedCuisines,
                        priceRange = updatedPriceRange
                    )
                )
                showCuisineDialog = false
            },
            isTablet = isTablet
        )
    }
    // Dietary Restrictions Dialog
    if (showRestrictionsDialog) {
        DietaryRestrictionsDialog(
            currentPreferences = userPreferences,
            onDismiss = { showRestrictionsDialog = false },
            onSave = { updatedRestrictions ->
                viewModel.updateUserPreferences(
                    userPreferences?.copy(dietaryRestrictions = updatedRestrictions) ?: UserPreferences(
                        dietaryRestrictions = updatedRestrictions
                    )
                )
                showRestrictionsDialog = false
            },
            isTablet = isTablet
        )
    }
}

@Composable
fun ProfileHeader(
    name: String,
    location: String,
    profileImageUrl: String?,
    isTablet: Boolean,
    onImagePick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(if (isTablet) 150.dp else 120.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onImagePick() },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = profileImageUrl ?: "https://via.placeholder.com/150", // Default placeholder
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                // Overlay for visual feedback
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Upload Image",
                        tint = Color.White,
                        modifier = Modifier.size(if (isTablet) 40.dp else 32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(if (isTablet) 20.dp else 16.dp))
            Text(
                text = name,
                fontSize = if (isTablet) 24.sp else 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color.Gray,
                    modifier = Modifier.size(if (isTablet) 20.dp else 16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = location,
                    color = Color.Gray,
                    fontSize = if (isTablet) 16.sp else 14.sp
                )
            }
            Spacer(modifier = Modifier.height(if (isTablet) 20.dp else 16.dp))
        }
    }
}

@Composable
fun CuisinePreferencesDialog(
    currentPreferences: UserPreferences?,
    onDismiss: () -> Unit,
    onSave: (List<String>, Int) -> Unit,
    isTablet: Boolean
) {
    val availableCuisines = listOf("Italian", "Mexican", "Thai", "Indian", "Chinese", "Japanese", "American", "French")
    var selectedCuisines by remember { mutableStateOf(currentPreferences?.cuisinePreferences ?: emptyList()) }
    var priceRange by remember { mutableStateOf(currentPreferences?.priceRange?.toFloat() ?: 2f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Food Preferences", fontSize = if (isTablet) 20.sp else 18.sp) },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.heightIn(max = if (isTablet) 400.dp else 300.dp)
                ) {
                    items(availableCuisines) { cuisine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedCuisines.contains(cuisine),
                                onCheckedChange = { isChecked ->
                                    selectedCuisines = if (isChecked) {
                                        selectedCuisines + cuisine
                                    } else {
                                        selectedCuisines - cuisine
                                    }
                                }
                            )
                            Text(
                                text = cuisine,
                                modifier = Modifier.padding(start = 8.dp),
                                fontSize = if (isTablet) 16.sp else 14.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Price Range",
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = priceRange,
                    onValueChange = { priceRange = it },
                    valueRange = 1f..4f,
                    steps = 2,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text(
                    text = when (priceRange.toInt()) {
                        1 -> "$  Inexpensive"
                        2 -> "$$  Moderately priced"
                        3 -> "$$$  Expensive"
                        4 -> "$$$$  Very expensive"
                        else -> "Not set"
                    },
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selectedCuisines, priceRange.toInt()) },
                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryPurple)
            ) {
                Text("Save", fontSize = if (isTablet) 16.sp else 14.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("Cancel", fontSize = if (isTablet) 16.sp else 14.sp)
            }
        },
        modifier = Modifier.widthIn(max = if (isTablet) 500.dp else 300.dp)
    )
}

@Composable
fun DietaryRestrictionsDialog(
    currentPreferences: UserPreferences?,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
    isTablet: Boolean
) {
    val availableRestrictions = listOf("Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free", "Nut-Free", "Halal", "Kosher")
    var selectedRestrictions by remember { mutableStateOf(currentPreferences?.dietaryRestrictions ?: emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Dietary Restrictions", fontSize = if (isTablet) 20.sp else 18.sp) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = if (isTablet) 400.dp else 300.dp)
            ) {
                items(availableRestrictions) { restriction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedRestrictions.contains(restriction),
                            onCheckedChange = { isChecked ->
                                selectedRestrictions = if (isChecked) {
                                    selectedRestrictions + restriction
                                } else {
                                    selectedRestrictions - restriction
                                }
                            }
                        )
                        Text(
                            text = restriction,
                            modifier = Modifier.padding(start = 8.dp),
                            fontSize = if (isTablet) 16.sp else 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selectedRestrictions) },
                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryPurple)
            ) {
                Text("Save", fontSize = if (isTablet) 16.sp else 14.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("Cancel", fontSize = if (isTablet) 16.sp else 14.sp)
            }
        },
        modifier = Modifier.widthIn(max = if (isTablet) 500.dp else 300.dp)
    )
}

@Composable
fun PreferencesSection(
    title: String,
    onUpdate: () -> Unit,
    isTablet: Boolean,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = if (isTablet) 20.sp else 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Update",
                color = PrimaryPurple,
                fontSize = if (isTablet) 16.sp else 14.sp,
                modifier = Modifier.clickable { onUpdate() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun PreferenceItem(
    label: String,
    value: String,
    isTablet: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = if (isTablet) 16.sp else 14.sp
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = if (isTablet) 16.sp else 14.sp
        )
    }
}

@Composable
fun RestrictionChip(
    text: String,
    isTablet: Boolean
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = LighterPurple,
        modifier = Modifier.height(if (isTablet) 40.dp else 36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                color = PrimaryPurple,
                fontSize = if (isTablet) 16.sp else 14.sp
            )
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0
        val maxWidth = constraints.maxWidth

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = maxWidth))
            if (currentRowWidth + placeable.width > maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf(placeable)
                currentRowWidth = placeable.width
            } else {
                currentRow.add(placeable)
                currentRowWidth += placeable.width
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val totalHeight = rows.sumOf { row ->
            row.maxOfOrNull { it.height } ?: 0
        }
        val layoutHeight = totalHeight.coerceAtLeast(constraints.minHeight)
        val layoutWidth = constraints.maxWidth

        layout(layoutWidth, layoutHeight) {
            var yPosition = 0
            rows.forEach { row ->
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                var xPosition = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x = xPosition, y = yPosition)
                    xPosition += placeable.width
                }
                yPosition += rowHeight
            }
        }
    }
}