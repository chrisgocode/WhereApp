package com.example.where.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.where.controller.GroupController
import com.example.where.controller.RestaurantController
import com.example.where.model.Group
import com.example.where.model.PlaceDetailResult
import com.example.where.model.Restaurant
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val restaurantController: RestaurantController,
    private val groupController: GroupController
) : ViewModel() {

    // Tab State
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Permission State
    private val _hasLocationPermission = MutableStateFlow(false) // Default to false
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    // Restaurant Controller State
    private val _nearbyRestaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val nearbyRestaurants: StateFlow<List<Restaurant>> = _nearbyRestaurants.asStateFlow()

    private val _restaurantIsLoading = MutableStateFlow(false)
    val restaurantIsLoading: StateFlow<Boolean> = _restaurantIsLoading.asStateFlow()

    private val _restaurantErrorMessage = MutableStateFlow<String?>(null)
    val restaurantErrorMessage: StateFlow<String?> = _restaurantErrorMessage.asStateFlow()

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    private val _userCity = MutableStateFlow<String?>(null)
    val userCity: StateFlow<String?> = _userCity.asStateFlow()

    private val _restaurantIsLoadingMore = MutableStateFlow(false)
    val restaurantIsLoadingMore: StateFlow<Boolean> = _restaurantIsLoadingMore.asStateFlow()

    private val _restaurantHasMoreResults = MutableStateFlow(false)
    val restaurantHasMoreResults: StateFlow<Boolean> = _restaurantHasMoreResults.asStateFlow()

    // State for checked restaurant IDs
    private val _checkedRestaurantIds = MutableStateFlow(emptySet<String>())
    val checkedRestaurantIds: StateFlow<Set<String>> = _checkedRestaurantIds.asStateFlow()

    // State for Restaurant Detail Modal
    private val _showRestaurantDetailModal = MutableStateFlow(false)
    val showRestaurantDetailModal: StateFlow<Boolean> = _showRestaurantDetailModal.asStateFlow()

    private val _selectedRestaurantForDetail = MutableStateFlow<Restaurant?>(null)
    val selectedRestaurantForDetail: StateFlow<Restaurant?> =
        _selectedRestaurantForDetail.asStateFlow()

    // States for fetched restaurant details from Controller
    val restaurantDetails: StateFlow<PlaceDetailResult?> =
        restaurantController.restaurantDetails.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )
    val detailsLoading: StateFlow<Boolean> = restaurantController.detailsLoading.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val detailsErrorMessage: StateFlow<String?> = restaurantController.detailsErrorMessage.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    private val _visibleRestaurantCount = MutableStateFlow(20) // Initial count from HomeScreen

    val paginatedRestaurants: StateFlow<List<Restaurant>> = combine(
        nearbyRestaurants, // This is the ViewModel's StateFlow mirroring the
        // controller's
        _visibleRestaurantCount
    ) { allRestaurantsList, count -> allRestaurantsList.take(count) }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            emptyList()
        )

    // GroupController State
    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    val userGroups: StateFlow<List<Group>> = _userGroups.asStateFlow()

    private val _groupsLoading = MutableStateFlow(false)
    val groupsLoading: StateFlow<Boolean> = _groupsLoading.asStateFlow()

    private val _groupsErrorMessage = MutableStateFlow<String?>(null)
    val groupsErrorMessage: StateFlow<String?> = _groupsErrorMessage.asStateFlow()

    private val _isAddingRestaurantsToGroup = MutableStateFlow(false)
    val isAddingRestaurantsToGroup: StateFlow<Boolean> = _isAddingRestaurantsToGroup.asStateFlow()

    // SendModal Logic
    private val _showSendModal = MutableStateFlow(false)
    val showSendModal: StateFlow<Boolean> = _showSendModal.asStateFlow()

    private val _selectedGroupForModal = MutableStateFlow<Group?>(null)
    val selectedGroupForModal: StateFlow<Group?> = _selectedGroupForModal.asStateFlow()

    // Snackbar Logic
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        Log.d(TAG, "HomeViewModel initialized by Hilt")

        // Observe RestaurantController states
        observeRestaurantControllerState()

        // Observe GroupController states
        observeGroupControllerState()

        // Trigger initial data fetch if permission already granted
        if (_hasLocationPermission.value) {
            fetchInitialRestaurantsIfPermitted()
        }
        setupAutomaticLocalPagination()
    }

    private fun observeRestaurantControllerState() {
        viewModelScope.launch {
            restaurantController.nearbyRestaurants.collect { _nearbyRestaurants.value = it }
        }
        viewModelScope.launch {
            restaurantController.isLoading.collect { _restaurantIsLoading.value = it }
        }
        viewModelScope.launch {
            restaurantController.errorMessage.collect { _restaurantErrorMessage.value = it }
        }
        viewModelScope.launch {
            restaurantController.userLocation.collect { _userLocation.value = it }
        }
        viewModelScope.launch { restaurantController.userCity.collect { _userCity.value = it } }
        viewModelScope.launch {
            restaurantController.isLoadingMore.collect { _restaurantIsLoadingMore.value = it }
        }
        viewModelScope.launch {
            restaurantController.hasMoreResults.collect { _restaurantHasMoreResults.value = it }
        }
    }

    private fun observeGroupControllerState() {
        viewModelScope.launch { groupController.userGroups.collect { _userGroups.value = it } }
        viewModelScope.launch { groupController.isLoading.collect { _groupsLoading.value = it } }
        viewModelScope.launch {
            groupController.errorMessage.collect { _groupsErrorMessage.value = it }
        }
        viewModelScope.launch {
            groupController.isAddingRestaurants.collect { _isAddingRestaurantsToGroup.value = it }
        }

        viewModelScope.launch {
            groupController.addRestaurantSuccess.collect { success ->
                if (success) {
                    Log.d(TAG, "Observed addRestaurantSuccess = true")
                    _snackbarMessage.value =
                        "Restaurant(s) added successfully!" // Set Snackbar message
                    _checkedRestaurantIds.value = emptySet() // Clear selection on success
                    _showSendModal.value = false // Dismiss modal on success

                    // Reset the success flag in the ViewModel state after setting the message.
                    // The UI will observe the snackbar message and call clearSnackbarMessage.
                    groupController.clearAddRestaurantStatus()
                }
            }
        }
        viewModelScope.launch {
            groupController.addRestaurantError.collect { error ->
                if (error != null) {
                    Log.d(TAG, "Observed addRestaurantError = $error")
                    _snackbarMessage.value = "Error: $error" // Set Snackbar message

                    // Reset the error flag in the ViewModel state after setting the message.
                    // The UI will observe the snackbar message and call clearSnackbarMessage.
                    groupController.clearAddRestaurantStatus()
                }
            }
        }
    }

    // --- Events / User Actions ---

    fun selectTab(index: Int) {
        _selectedTabIndex.update { index }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.update { query }
    }

    fun updateLocationPermission(granted: Boolean) {
        val permissionChanged = _hasLocationPermission.value != granted
        _hasLocationPermission.update { granted }
        // Fetch restaurants only if permission was just granted
        if (granted && permissionChanged) {
            fetchInitialRestaurantsIfPermitted()
        }
    }

    fun fetchMoreRestaurants() {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Fetching more restaurants...")
            restaurantController.fetchMoreRestaurants()
        }
    }

    fun fetchNearbyRestaurants() {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Fetching restaurants...")
            restaurantController.fetchNearbyRestaurants()
        }
    }

    // Event Handlers for Restaurant List and Detail Modal

    fun onRestaurantCheckedChange(id: String, isChecked: Boolean) {
        _checkedRestaurantIds.update { currentIds ->
            if (isChecked) {
                currentIds + id
            } else {
                currentIds - id
            }
        }
    }

    fun onRestaurantCardClick(restaurant: Restaurant) {
        _selectedRestaurantForDetail.value = restaurant // Keep for initial display if needed
        _showRestaurantDetailModal.value = true
        viewModelScope.launch {
            Log.d(TAG, "Fetching details for restaurant ID: ${restaurant.id}")
            restaurantController.fetchRestaurantDetails(restaurant.id)
        }
    }

    fun onDismissRestaurantDetailModal() {
        _showRestaurantDetailModal.value = false
        // Clear details when modal is dismissed
        viewModelScope.launch {
            clearRestaurantDetails()
        }
        _selectedRestaurantForDetail.value = null // Clear the summary restaurant
    }

    fun requestMoreLocalRestaurants() {
        if (_visibleRestaurantCount.value < nearbyRestaurants.value.size && !restaurantIsLoadingMore.value) {
            _visibleRestaurantCount.update { currentCount ->
                (currentCount + 20).coerceAtMost(nearbyRestaurants.value.size)
            }
        }
    }

    /** Called by the UI's "Load More" button to fetch additional restaurants from the API. */
    fun fetchMoreRestaurantsFromApi() {
        // Check if there are more results to fetch from API and not already loading
        if (restaurantHasMoreResults.value && !restaurantIsLoadingMore.value) {
            viewModelScope.launch { restaurantController.fetchMoreRestaurants() }
        }
    }

    private fun setupAutomaticLocalPagination() {
        viewModelScope.launch {
            combine(
                nearbyRestaurants, // ViewModel's StateFlow from controller
                restaurantIsLoadingMore // ViewModel's StateFlow from controller
            ) { restaurants, isLoadingMore ->
                Pair(
                    restaurants,
                    isLoadingMore
                )
            }.collect { (restaurants, isLoadingMore) ->
                    if (restaurants.size > _visibleRestaurantCount.value && !isLoadingMore) {
                        _visibleRestaurantCount.update { currentCount ->
                            (currentCount + 20).coerceAtMost(restaurants.size)
                        }
                    }
                }
        }
    }

    // Event Handlers for Send Modal
    fun onSendButtonInHomeScreenClicked() {
        viewModelScope.launch {
            groupController.fetchUserGroups() // Fetch groups when modal is about to be shown
        }
        _selectedGroupForModal.value = null // Reset selected group
        _showSendModal.value = true
    }

    fun onDismissSendModal() {
        _showSendModal.value = false
    }

    fun onGroupSelectedInModal(group: Group) {
        _selectedGroupForModal.value = group
    }

    fun onSendToGroupConfirmedInModal() {
        _selectedGroupForModal.value?.let { group ->
            if (group.id.isNotBlank()) {
                viewModelScope.launch {
                    groupController.addRestaurantsToGroup(group.id, _checkedRestaurantIds.value)
                }
            } else {
                Log.e(TAG, "Selected group has no ID.")
                _snackbarMessage.value = "Error: Selected group is invalid."
            }
        } ?: run {
            Log.e(TAG, "No group selected to send.")
            _snackbarMessage.value = "Error: No group selected."
        }
    }

    private fun fetchInitialRestaurantsIfPermitted() {
        if (!_hasLocationPermission.value) {
            Log.d(TAG, "Attempted to fetch restaurants without permission.")
            return
        }

        Log.d(TAG, "Permission granted, launching fetch process in ViewModel.")
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: Getting user location...")
            restaurantController.getUserLocation(onSuccess = { location ->
                Log.d(TAG, "ViewModel: Location success: $location")
                Log.d(TAG, "ViewModel: Fetching nearby restaurants...")
                fetchNearbyRestaurants()
            }, onError = { error ->
                Log.e(TAG, "ViewModel: Location error: $error")
                _restaurantErrorMessage.value = error // Update error state
            })
        }
    }

    // Function to clear the snackbar message after it has been shown
    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    // Function to clear restaurant details and error
    fun clearRestaurantDetails() {
        viewModelScope.launch {
            restaurantController.fetchRestaurantDetails(
                ""
            )
        }
    }
}
