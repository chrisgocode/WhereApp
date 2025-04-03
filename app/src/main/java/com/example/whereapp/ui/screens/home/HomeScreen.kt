package com.example.whereapp.ui.screens.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whereapp.R
import com.example.whereapp.BuildConfig
import com.example.whereapp.controller.RestaurantController
import com.example.whereapp.model.Restaurant
import com.example.whereapp.ui.screens.map.RestaurantMapView
import com.example.whereapp.ui.screens.shared.BottomNavBar
import com.example.whereapp.ui.theme.WhereAppTheme
import com.example.whereapp.utils.LocationPermissionUtils
import kotlinx.coroutines.launch
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

val TAG = "HomeScreen"

// Define theme colors
val PrimaryPurple = Color(0xFF8A3FFC)
val BackgroundWhite = Color(0xFFFAFAFA)
val DarkGray = Color(0xFF333333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Remember controller instance
    val restaurantController = remember { RestaurantController(context) }

    // API key for Google Maps
    val apiKey = BuildConfig.MAPS_API_KEY

    Log.d(TAG, "HomeScreen initialized, API Key length: ${apiKey.length}")

    // TabIndex state
    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Pagination state
    var visibleRestaurantCount by remember { mutableStateOf(20) }
    val gridState = rememberLazyGridState()

    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(LocationPermissionUtils.hasLocationPermission(context))
    }

    Log.d(TAG, "Initial location permission state: $hasLocationPermission")

    // Initialize the controller with API key
    LaunchedEffect(Unit) {
        Log.d(TAG, "Setting API key")
        restaurantController.setApiKey(apiKey)
    }

    // Fetch restaurants when permissions granted
    LaunchedEffect(hasLocationPermission) {
        Log.d(TAG, "LaunchedEffect triggered with permission: $hasLocationPermission")
        if (hasLocationPermission) {
            coroutineScope.launch {
                Log.d(TAG, "Getting user location...")
                restaurantController.getUserLocation(
                    onSuccess = { location ->
                        Log.d(TAG, "Location success: $location")
                        coroutineScope.launch {
                            Log.d(TAG, "Fetching nearby restaurants...")
                            restaurantController.fetchNearbyRestaurants()
                        }
                    },
                    onError = { error ->
                        Log.e(TAG, "Location error: $error")
                    }
                )
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // TODO:
            //  Make notification icon interactable for real notifications
            // Top bar with title and notification icon
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
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Where?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DarkGray
                    )
                }

                Row {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search box
            // TODO:
            //  Users can search for a specific restaurant or an arbritrary string
            //  FindPlace or TextSearch
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search restaurants...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                shape = RoundedCornerShape(8.dp),
                colors =
                    TextFieldDefaults.colors(
                        focusedIndicatorColor = LightGray,
                        unfocusedIndicatorColor = LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "List",
                        color = if (selectedTabIndex == 1) Color.Gray else PrimaryPurple,
                        fontWeight =
                            if (selectedTabIndex == 1) FontWeight.Normal
                            else FontWeight.Bold,
                        modifier = Modifier.clickable { selectedTabIndex = 0 }
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (selectedTabIndex == 0) Color.Gray else PrimaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Map",
                        color = if (selectedTabIndex == 0) Color.Gray else PrimaryPurple,
                        fontWeight =
                            if (selectedTabIndex == 0) FontWeight.Normal
                            else FontWeight.Bold,
                        modifier = Modifier.clickable { selectedTabIndex = 1 }
                    )
                }
            }

            // Line indicator under the selected tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height(2.dp)
                            .align(
                                if (selectedTabIndex == 0) Alignment.CenterStart
                                else Alignment.CenterEnd
                            )
                            .background(PrimaryPurple)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on selected tab
            if (!hasLocationPermission) {
                // Request location permission
                LocationPermissionUtils.RequestLocationPermission(
                    onPermissionGranted = { hasLocationPermission = true },
                    onPermissionDenied = { /* Handle permission denied */ }
                )

                // Show permission request message
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Location permission required to show restaurants")
                }
            } else if (restaurantController.isLoading.value && restaurantController.nearbyRestaurants.value.isEmpty()) {
                // Loading indicator - only show full screen loading on initial load
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            } else if (restaurantController.errorMessage.value != null) {
                // Error message
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = restaurantController.errorMessage.value ?: "Unknown error",
                        color = Color.Red
                    )
                }
            } else {
                // Show content based on tab selection
                when (selectedTabIndex) {
                    0 -> {
                        // List View
                        val allRestaurants = restaurantController.nearbyRestaurants.value
                        val paginatedRestaurants = remember(allRestaurants, visibleRestaurantCount) {
                            allRestaurants.take(visibleRestaurantCount)
                        }

                        // Check if we've reached the end of the list to trigger loading more
                        val reachedEnd = remember {
                            derivedStateOf {
                                val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
                                lastVisibleItem?.index != null &&
                                        lastVisibleItem.index >= paginatedRestaurants.size - 2 &&
                                        paginatedRestaurants.size < allRestaurants.size
                            }
                        }

                        // When end reached, load more restaurants
                        LaunchedEffect(reachedEnd.value) {
                            if (reachedEnd.value) {
                                Log.d(TAG, "Reached end of list, loading more restaurants")
                                visibleRestaurantCount = (visibleRestaurantCount + 20).coerceAtMost(allRestaurants.size)
                            }
                        }

                        // Automatically update visibleRestaurantCount when new restaurants are loaded
                        LaunchedEffect(allRestaurants.size) {
                            Log.d(TAG, "Restaurant count changed: ${allRestaurants.size}, updating visibleRestaurantCount from $visibleRestaurantCount")
                            if (allRestaurants.size > visibleRestaurantCount && !restaurantController.isLoadingMore.value) {
                                visibleRestaurantCount = (visibleRestaurantCount + 20).coerceAtMost(allRestaurants.size)
                                Log.d(TAG, "Updated visibleRestaurantCount to $visibleRestaurantCount")
                            }
                        }

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(paginatedRestaurants) { restaurant ->
                                RestaurantCard(restaurant = restaurant)
                            }

                            // Show loading indicator at the bottom when more items are available
                            if (paginatedRestaurants.size < allRestaurants.size || restaurantController.hasMoreResults.value) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    if (paginatedRestaurants.size < allRestaurants.size) {
                                        // Show local loading spinner when loading more from already fetched data
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = PrimaryPurple,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    } else if (restaurantController.hasMoreResults.value) {
                                        // TODO:
                                        //  Consider a different approach to "Load More" button
                                        //  Just another standard pagination scroll?
                                        //  Button works better for debugging purposes at the moment
                                        // Only show "Load More" button when we've shown all locally available data
                                        // and need to fetch from the API
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (restaurantController.isLoadingMore.value) {
                                                // Show loading indicator when loading more results
                                                Log.d(TAG, "isLoadingMore is true, showing loading indicator")
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.7f)
                                                        .height(48.dp), // Match button height
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(28.dp),
                                                        color = PrimaryPurple,
                                                        strokeWidth = 3.dp
                                                    )
                                                }
                                            } else {
                                                Log.d(TAG, "isLoadingMore is false, showing button. hasMoreResults=${restaurantController.hasMoreResults.value}")
                                                Button(
                                                    onClick = {
                                                        Log.d(TAG, "Load More button clicked")
                                                        coroutineScope.launch {
                                                            restaurantController.fetchMoreRestaurants()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = PrimaryPurple
                                                    ),
                                                    modifier = Modifier.fillMaxWidth(0.7f)
                                                ) {
                                                    Text(
                                                        "Load More",
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Map View
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            RestaurantMapView(
                                userLocation = restaurantController.userLocation.value,
                                restaurants = restaurantController.nearbyRestaurants.value,
                                // TODO:
                                //  When user presses a restaurant, it should pull up its detailed
                                //  restaurant card over the map view
                                onRestaurantClick = { /* Handle restaurant click */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

// TODO:
//  Create a DetailedRestaurantCard for when a user clicks
//  on a resturant.
//  Will display more information about restaurant:
//   - Type
//   - Hours
//   - Reviews? Ratings?
//   - Potential for a match score based on their preferences?
//   - Etc.

@Composable
fun RestaurantCard(restaurant: Restaurant) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp), // TODO: Expand card height based on screen size
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background color based on category
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            // TODO:
                            //  Replace with restaurant images instead
                            when (restaurant.category) {
                                "Cafe" -> Color(0xFFC1B18B)
                                "Restaurant" -> Color(0xFFA1C6EA)
                                "Bar" -> Color(0xFF424242)
                                "Bakery" -> Color(0xFFD7CCC8)
                                "Takeaway" -> Color(0xFFFFCCBC)
                                else -> Color.LightGray
                            }
                        )
            )

            // TODO:
            //  Need to retrieve the exact type of place
            //  Some hotels come up as restaurants
            // Category tag
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
            ) { Text(text = restaurant.category, fontSize = 12.sp, color = Color.Black) }

            // TODO:
            //  Route to necessary screen
            // Bottom information overlay
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = restaurant.name,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(text = restaurant.rating.toString(), fontSize = 14.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = restaurant.address,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(text = restaurant.distance, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
val LightGray = Color(0xFFE0E0E0)


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    WhereAppTheme {
        Scaffold(bottomBar = { BottomNavBar() }) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) { HomeScreen() }
        }
    }
}
