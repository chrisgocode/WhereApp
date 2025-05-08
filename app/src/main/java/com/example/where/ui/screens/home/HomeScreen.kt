package com.example.where.ui.screens.home

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.where.BuildConfig
import com.example.where.R
import com.example.where.model.Group
import com.example.where.model.PlaceDetailResult
import com.example.where.model.Restaurant
import com.example.where.ui.screens.map.RestaurantMapView
import com.example.where.ui.screens.shared.BottomNavBar
import com.example.where.utils.LocationPermissionUtils
import kotlinx.coroutines.flow.collectLatest

// Define theme colors
val PrimaryPurple = Color(0xFF8A3FFC)
val BackgroundWhite = Color(0xFFFAFAFA)
val DarkGray = Color(0xFF333333)
val LightGray = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        navController: NavController,
        onNavItemClick: (String) -> Unit = {},
        viewModel: HomeViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect state from ViewModel
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()
    val nearbyRestaurantsFromVM by viewModel.nearbyRestaurants.collectAsState()
    val restaurantIsLoading by viewModel.restaurantIsLoading.collectAsState()
    val restaurantErrorMessage by viewModel.restaurantErrorMessage.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val restaurantIsLoadingMore by viewModel.restaurantIsLoadingMore.collectAsState()
    val restaurantHasMoreResults by viewModel.restaurantHasMoreResults.collectAsState()

    // Collect Filter Modal state
    val showFilterModal by viewModel.showFilterModal.collectAsState()
    val selectedCuisineFilters by viewModel.selectedCuisineFilters.collectAsState()
    val currentUserPreferences by viewModel.currentUserPreferences.collectAsState()
    val selectedGroupFilters by viewModel.selectedGroupFilters.collectAsState()

    val apiKey = BuildConfig.MAPS_API_KEY

    // Collect Restaurant List & Detail Modal Logic from ViewModel
    val checkedRestaurantIds by viewModel.checkedRestaurantIds.collectAsState()
    val showRestaurantDetailModal by viewModel.showRestaurantDetailModal.collectAsState()
    val selectedRestaurantSummary by viewModel.selectedRestaurantForDetail.collectAsState()
    val paginatedRestaurants by viewModel.paginatedRestaurants.collectAsState()
    val gridState = rememberLazyGridState()

    // Collect Send Modal Logic from ViewModel
    val showSendModal by viewModel.showSendModal.collectAsState()
    val selectedGroupForModal by viewModel.selectedGroupForModal.collectAsState()
    val userGroupsForModal by viewModel.userGroups.collectAsState()
    val groupsLoadingInModal by viewModel.groupsLoading.collectAsState()
    val groupsErrorMessageInModal by viewModel.groupsErrorMessage.collectAsState()
    val isSendingToGroupInModal by viewModel.isAddingRestaurantsToGroup.collectAsState()

    // Use new per-restaurantId accessors for details modal
    val selectedRestaurantId = selectedRestaurantSummary?.id
    val restaurantDetailsData = selectedRestaurantId?.let { viewModel.getRestaurantDetails(it) }
    val detailsAreLoading = selectedRestaurantId?.let { viewModel.isDetailsLoading(it) } ?: false
    val detailsErrorMessageText = selectedRestaurantId?.let { viewModel.getDetailsError(it) }

    // LaunchedEffect to show snackbar based on ViewModel state
    LaunchedEffect(snackbarHostState, viewModel) {
        viewModel.snackbarMessage.collectLatest { message ->
            if (message != null) {
                Log.d(TAG, "Showing snackbar: $message")
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                viewModel.clearSnackbarMessage()
            }
        }
    }

    // LaunchedEffect to refetch restaurants for the List tab if it's empty and selected
    LaunchedEffect(selectedTabIndex, hasLocationPermission, nearbyRestaurantsFromVM.isEmpty()) {
        if (selectedTabIndex == 0 && nearbyRestaurantsFromVM.isEmpty() && hasLocationPermission) {
            Log.d(
                    TAG,
                    "List tab is active and empty, and permission granted. Fetching restaurants."
            )
            viewModel.fetchNearbyRestaurants()
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp >= 600

    // Estimate paddings, toolbar, etc. (adjust as needed)
    val verticalPadding = 16 * 3 // top, between, bottom
    val toolbarHeight = 56 // e.g., top bar
    val tabHeight = 48 // e.g., tab row
    val totalReserved = verticalPadding + toolbarHeight + tabHeight

    val cardHeightDp =
            if (isTablet) {
                ((screenHeightDp - totalReserved) / 2).dp
            } else {
                180.dp
            }

    Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = { BottomNavBar(selectedRoute = "home", onNavItemClick = onNavItemClick) },
            content = { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            color = BackgroundWhite
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                                            modifier =
                                                    Modifier.size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.Gray)
                                                            .clickable {
                                                                navController.navigate("profile")
                                                            }
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
                            OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search restaurants...") },
                                    leadingIcon = {
                                        Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search"
                                        )
                                    },
                                    trailingIcon = {
                                        Button(
                                                onClick = { viewModel.onFilterButtonClicked() },
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = Color.Transparent,
                                                                contentColor = PrimaryPurple
                                                        )
                                        ) { Text("Filter") }
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
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                            text = "List",
                                            color =
                                                    if (selectedTabIndex == 1) Color.Gray
                                                    else PrimaryPurple,
                                            fontWeight =
                                                    if (selectedTabIndex == 1) FontWeight.Normal
                                                    else FontWeight.Bold,
                                            modifier = Modifier.clickable { viewModel.selectTab(0) }
                                    )
                                }

                                Row(
                                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint =
                                                    if (selectedTabIndex == 0) Color.Gray
                                                    else PrimaryPurple,
                                            modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                            text = "Map",
                                            color =
                                                    if (selectedTabIndex == 0) Color.Gray
                                                    else PrimaryPurple,
                                            fontWeight =
                                                    if (selectedTabIndex == 0) FontWeight.Normal
                                                    else FontWeight.Bold,
                                            modifier = Modifier.clickable { viewModel.selectTab(1) }
                                    )
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth(0.5f)
                                                        .height(2.dp)
                                                        .align(
                                                                if (selectedTabIndex == 0)
                                                                        Alignment.CenterStart
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
                                        onPermissionGranted = {
                                            viewModel.updateLocationPermission(true)
                                        },
                                        onPermissionDenied = { /* TODO: Permission Denied */}
                                )

                                // Show permission request message
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) { Text("Location permission required to show restaurants") }
                            } else if (restaurantIsLoading && nearbyRestaurantsFromVM.isEmpty()) {
                                // Loading indicator
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = PrimaryPurple) }
                            } else if (restaurantErrorMessage != null) {
                                // Error message
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                            text = restaurantErrorMessage ?: "Unknown error",
                                            color = Color.Red
                                    )
                                }
                            } else {
                                // Show content based on tab selection
                                when (selectedTabIndex) {
                                    0 -> {
                                        // List View
                                        val reachedEnd = remember {
                                            derivedStateOf {
                                                val lastVisibleItem =
                                                        gridState.layoutInfo.visibleItemsInfo
                                                                .lastOrNull()
                                                lastVisibleItem?.index != null &&
                                                        lastVisibleItem.index >=
                                                                paginatedRestaurants.size - 2 &&
                                                        paginatedRestaurants.size <
                                                                nearbyRestaurantsFromVM.size
                                            }
                                        }

                                        // When end reached, request
                                        // more local restaurants
                                        // from ViewModel
                                        LaunchedEffect(reachedEnd.value) {
                                            if (reachedEnd.value) {
                                                Log.d(
                                                        TAG,
                                                        "Reached end of local list, requesting more local restaurants from ViewModel"
                                                )
                                                viewModel.requestMoreLocalRestaurants()
                                            }
                                        }

                                        LazyVerticalGrid(
                                                state = gridState,
                                                columns = GridCells.Fixed(2),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(paginatedRestaurants, key = { it.id }) {
                                                    restaurant ->
                                                RestaurantCard(
                                                        restaurant = restaurant,
                                                        isChecked =
                                                                restaurant.id in
                                                                        checkedRestaurantIds,
                                                        onCheckedChange = { isChecked ->
                                                            viewModel.onRestaurantCheckedChange(
                                                                    restaurant.id,
                                                                    isChecked
                                                            )
                                                        },
                                                        onClick = {
                                                            viewModel.onRestaurantCardClick(
                                                                    restaurant
                                                            )
                                                        },
                                                        apiKey = apiKey,
                                                        cardHeight = cardHeightDp
                                                )
                                            }

                                            // Show loading
                                            // indicator at the
                                            // bottom when more
                                            // items
                                            // are
                                            // available
                                            if (paginatedRestaurants.size <
                                                            nearbyRestaurantsFromVM.size ||
                                                            restaurantHasMoreResults
                                            ) {
                                                item(span = { GridItemSpan(maxLineSpan) }) {
                                                    if (restaurantIsLoadingMore) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .padding(16.dp),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator(
                                                                    modifier = Modifier.size(24.dp),
                                                                    color = PrimaryPurple,
                                                                    strokeWidth = 2.dp
                                                            )
                                                        }
                                                    } else if (restaurantHasMoreResults) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .padding(16.dp),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                            Log.d(
                                                                    TAG,
                                                                    "isLoadingMore is false, showing button. hasMoreResults=$restaurantHasMoreResults"
                                                            )
                                                            Button(
                                                                    onClick = {
                                                                        Log.d(
                                                                                TAG,
                                                                                "Load More button clicked - calling ViewModel"
                                                                        )
                                                                        viewModel
                                                                                .fetchMoreRestaurantsFromApi()
                                                                    },
                                                                    colors =
                                                                            ButtonDefaults
                                                                                    .buttonColors(
                                                                                            containerColor =
                                                                                                    PrimaryPurple
                                                                                    ),
                                                                    modifier =
                                                                            Modifier.fillMaxWidth(
                                                                                    0.7f
                                                                            )
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
                                    1 -> {
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .clip(RoundedCornerShape(12.dp))
                                        ) {
                                            RestaurantMapView(
                                                    userLocation = userLocation,
                                                    restaurants = nearbyRestaurantsFromVM,
                                                    onRestaurantClick = { restaurant ->
                                                        viewModel.onRestaurantCardClick(restaurant)
                                                    },
                                                    onClearClicked = {
                                                        viewModel.clearMapAndRestaurantList()
                                                    },
                                                    onSearchHereClicked = { center, radius ->
                                                        viewModel.searchMapArea(center, radius)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (checkedRestaurantIds.isNotEmpty()) {
                        Button(
                                onClick = { viewModel.onSendButtonInHomeScreenClicked() },
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .padding(bottom = 80.dp)
                                                .fillMaxWidth(0.7f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) { Text("Send (${checkedRestaurantIds.size})", color = Color.White) }
                    }
                }

                // Conditionally display the Send Modal
                if (showSendModal) {
                    SendModal(
                            groups = userGroupsForModal,
                            isLoading = groupsLoadingInModal,
                            errorMessage = groupsErrorMessageInModal,
                            isSending = isSendingToGroupInModal,
                            selectedGroup = selectedGroupForModal,
                            onDismiss = { viewModel.onDismissSendModal() },
                            onGroupSelected = { group -> viewModel.onGroupSelectedInModal(group) },
                            onSend = { viewModel.onSendToGroupConfirmedInModal() }
                    )
                }

                // Conditionally display the Restaurant Detail Modal
                if (showRestaurantDetailModal && selectedRestaurantSummary != null) {
                    RestaurantDetailModal(
                            restaurantSummary = selectedRestaurantSummary!!,
                            restaurantDetails = restaurantDetailsData,
                            isLoading = detailsAreLoading,
                            errorMessage = detailsErrorMessageText,
                            onDismiss = { viewModel.onDismissRestaurantDetailModal() },
                            apiKey = apiKey
                    )
                }

                // Conditionally display the Filter Modal
                if (showFilterModal) {
                    FilterModal(
                            onDismiss = { viewModel.onDismissFilterModal() },
                            allCuisines = currentUserPreferences.cuisinePreferences,
                            selectedCuisines = selectedCuisineFilters,
                            onCuisineSelected = { cuisine, isChecked ->
                                viewModel.onCuisineFilterChanged(cuisine, isChecked)
                            },
                            userGroups = userGroupsForModal,
                            selectedGroupIds = selectedGroupFilters,
                            onGroupSelected = viewModel::onGroupFilterChanged,
                            onApplyClick = viewModel::applyFilters
                    )
                }
            }
    )
}

@OptIn(
        ExperimentalMaterial3Api::class,
        androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun SendModal(
        groups: List<Group>,
        isLoading: Boolean,
        errorMessage: String?,
        isSending: Boolean,
        selectedGroup: Group?,
        onDismiss: () -> Unit,
        onGroupSelected: (Group) -> Unit,
        onSend: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Title and Close Button
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Send to ...", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Loading or Error indicator for groups
                    if (isLoading) {
                        CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else if (errorMessage != null) {
                        Text(
                                errorMessage,
                                color = Color.Red,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        // Dropdown Menu
                        ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                        ) {
                            TextField(
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    readOnly = true,
                                    value = selectedGroup?.name ?: "Select a group",
                                    onValueChange = {},
                                    label = { Text("Group") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = expanded
                                        )
                                    },
                                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            )
                            ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                            ) {
                                if (groups.isEmpty()) {
                                    DropdownMenuItem(
                                            text = { Text("No groups found") },
                                            enabled = false,
                                            onClick = {},
                                            contentPadding =
                                                    ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                } else {
                                    groups.forEach { group ->
                                        DropdownMenuItem(
                                                text = { Text(group.name) },
                                                onClick = {
                                                    onGroupSelected(group)
                                                    expanded = false
                                                },
                                                contentPadding =
                                                        ExposedDropdownMenuDefaults
                                                                .ItemContentPadding,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Send Button (Modal)
                    Button(
                            onClick = onSend,
                            enabled = selectedGroup != null && !isLoading && !isSending,
                            modifier =
                                    Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.6f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(
        ExperimentalMaterial3Api::class,
        androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun RestaurantCard(
        restaurant: Restaurant,
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        onClick: () -> Unit,
        apiKey: String,
        cardHeight: Dp
) {
    Card(
            modifier = Modifier.fillMaxWidth().height(cardHeight).clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val firstPhotoMeta = restaurant.photoMetadataList?.firstOrNull()

            if (firstPhotoMeta != null) {
                val imageUrl =
                        "https://maps.googleapis.com/maps/api/place/photo"
                                .plus("?maxwidth=400")
                                .plus("&photoreference=${firstPhotoMeta.reference}")
                                .plus("&key=$apiKey")

                // Use Coil to load the image asynchronously
                AsyncImage(
                        model = imageUrl,
                        contentDescription = restaurant.name + " image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // Crop the image to fill the bounds
                )

                // Display attributions
                firstPhotoMeta.attributions?.firstOrNull()?.let { attributionText ->
                    Text(
                            text = AnnotatedString.fromHtml(attributionText),
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier =
                                    Modifier.align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(2.dp)
                    )
                }
            } else {
                // Fallback to category color if no image
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(
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
            }

            // Checkbox in the top-left corner
            Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
            )

            // TODO:
            //  Need to retrieve the exact type of place
            //  Some hotels come up as restaurants
            // Category tag
            Box(
                    modifier =
                            Modifier.align(Alignment.TopEnd)
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
                            Modifier.align(Alignment.BottomStart)
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
                            modifier = Modifier.weight(1f, fill = false)
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

@Composable
fun RestaurantDetailModal(
        restaurantSummary: Restaurant,
        restaurantDetails: PlaceDetailResult?,
        isLoading: Boolean,
        errorMessage: String?,
        onDismiss: () -> Unit,
        apiKey: String
) {
    Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
                modifier =
                        Modifier.fillMaxWidth(0.9f)
                                .heightIn(min = 400.dp, max = 700.dp)
                                .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Bar
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                start = 16.dp,
                                                end = 8.dp,
                                                top = 12.dp,
                                                bottom = 8.dp
                                        ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = restaurantDetails?.name ?: restaurantSummary.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = DarkGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkGray)
                    }
                }

                // Image Display
                val photoToDisplay = restaurantDetails?.photos?.firstOrNull()
                val photoAttributions = photoToDisplay?.htmlAttributions
                val photoReference = photoToDisplay?.photoReference

                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(LightGray)) {
                    if (photoReference != null) {
                        val imageUrl =
                                "https://maps.googleapis.com/maps/api/place/photo"
                                        .plus("?maxwidth=800")
                                        .plus("&photoreference=$photoReference")
                                        .plus("&key=$apiKey")
                        AsyncImage(
                                model = imageUrl,
                                contentDescription =
                                        "${restaurantDetails.name ?: restaurantSummary.name} photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )
                        photoAttributions?.firstOrNull()?.let { attributionText ->
                            Text(
                                    text = AnnotatedString.fromHtml(attributionText),
                                    fontSize = 8.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier =
                                            Modifier.align(Alignment.BottomStart)
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Box(
                                modifier = Modifier.fillMaxSize().background(Color.Gray),
                                contentAlignment = Alignment.Center
                        ) { Text("No photo available", color = Color.White, fontSize = 16.sp) }
                    }
                }

                // Scrollable content for details
                if (isLoading) {
                    Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = PrimaryPurple) }
                } else if (errorMessage != null) {
                    Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                    ) { Text(errorMessage, color = Color.Red, fontSize = 16.sp) }
                } else if (restaurantDetails != null) {
                    Column(
                            modifier =
                                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                            .verticalScroll(rememberScrollState())
                    ) {
                        // Rating and Reviews
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    painter = painterResource(id = R.drawable.ic_star),
                                    contentDescription = "Rating",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                    text = "${restaurantDetails.rating ?: "N/A"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = DarkGray
                            )
                            Text(
                                    text = " (${restaurantDetails.userRatingsTotal ?: 0} reviews)",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Address
                        restaurantDetails.formattedAddress?.let {
                            DetailItem(icon = Icons.Default.LocationOn, text = it, type = "address")
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Phone Number
                        restaurantDetails.internationalPhoneNumber?.let {
                            DetailItem(icon = Icons.Default.Call, text = it, type = "phone")
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Website
                        restaurantDetails.website?.let {
                            DetailItem(icon = Icons.Default.Info, text = it, type = "website")
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Price Level
                        restaurantDetails.priceLevel?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                        painter = painterResource(id = R.drawable.ic_star),
                                        contentDescription = "Price Level",
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = "$".repeat(it.coerceAtLeast(1)),
                                        fontSize = 16.sp,
                                        color = DarkGray,
                                        fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Opening Hours
                        restaurantDetails.openingHours?.let { hours ->
                            Text(
                                    "Opening Hours",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = DarkGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                    text = if (hours.openNow == true) "Open now" else "Closed",
                                    fontSize = 16.sp,
                                    color =
                                            if (hours.openNow == true) Color(0xFF4CAF50)
                                            else Color.Red,
                                    fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            hours.weekdayText?.forEach { daySchedule ->
                                Text(text = daySchedule, fontSize = 14.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) { Text("Close", color = Color.White, fontSize = 16.sp) }
                    }
                } else {
                    // Fallback or initial state before details load and no
                    // error
                    Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                    ) { Text("Loading details...", fontSize = 16.sp, color = DarkGray) }
                }
            }
        }
    }
}

@Composable
fun DetailItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        text: String,
        type: String? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.clickable(enabled = type != null) {
                        when (type) {
                            "address" -> {
                                val gmmIntentUri = "geo:0,0?q=${Uri.encode(text)}".toUri()
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    // Fallback if Google Maps is not installed
                                    // or no app can handle geo intent
                                    uriHandler.openUri(
                                            "https://maps.google.com/?q=${Uri.encode(text)}"
                                    )
                                }
                            }
                            "phone" -> {
                                val phoneUri = "tel:$text".toUri()
                                val dialIntent = Intent(Intent.ACTION_DIAL, phoneUri)
                                if (dialIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(dialIntent)
                                }
                            }
                            "website" -> {
                                var url = text
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    url = "http://$url"
                                }
                                uriHandler.openUri(url)
                            }
                        }
                    }
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 16.sp, color = DarkGray)
    }
}

@Composable
fun FilterModal(
        onDismiss: () -> Unit,
        allCuisines: List<String>,
        selectedCuisines: Set<String>,
        onCuisineSelected: (String, Boolean) -> Unit,
        userGroups: List<Group>,
        selectedGroupIds: Set<String>,
        onGroupSelected: (String, Boolean) -> Unit,
        onApplyClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filter Results", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cuisine Preferences", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (allCuisines.isEmpty()) {
                        Text("No cuisine preferences set.", color = Color.Gray)
                    } else {
                        allCuisines.forEach { cuisine ->
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .clickable {
                                                        onCuisineSelected(
                                                                cuisine,
                                                                !selectedCuisines.contains(cuisine)
                                                        )
                                                    }
                                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                        checked = selectedCuisines.contains(cuisine),
                                        onCheckedChange = { isChecked ->
                                            onCuisineSelected(cuisine, isChecked)
                                        }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cuisine)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Group Preferences", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (userGroups.isEmpty()) {
                        Text("No groups found.", color = Color.Gray)
                    } else {
                        userGroups.forEach { group ->
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .clickable {
                                                        onGroupSelected(
                                                                group.id,
                                                                !selectedGroupIds.contains(group.id)
                                                        )
                                                    }
                                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                        checked = selectedGroupIds.contains(group.id),
                                        onCheckedChange = { isChecked ->
                                            onGroupSelected(group.id, isChecked)
                                        }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(group.name)
                            }
                        }
                    }

                    // TODO: Add other filter options here (e.g., price, dietary restrictions)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                            onClick = onApplyClick,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) { Text("Apply Filters", color = Color.White) }
                }
            }
        }
    }
}

// @Preview(showBackground = true)
// @Composable
// fun HomeScreenPreview() {
//    val navController = rememberNavController() // Example NavController
//    Scaffold(bottomBar = { BottomNavBar() }) { paddingValues ->
//        Box(modifier = Modifier.padding(paddingValues)) { HomeScreen(NavController()) }
//    }
// }
