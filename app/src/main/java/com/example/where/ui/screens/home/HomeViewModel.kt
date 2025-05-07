package com.example.where.ui.screens.home

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.where.controller.GroupController
import com.example.where.controller.RestaurantController
import com.example.where.model.Group
import com.example.where.model.PlaceDetailResult
import com.example.where.model.Restaurant
import com.example.where.model.UserPreferences
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel
@Inject
constructor(
        private val restaurantController: RestaurantController,
        private val groupController: GroupController,
        dataStore: DataStore<Preferences>
) : ViewModel() {

    // User Preferences
    val currentUserPreferences: StateFlow<UserPreferences> =
            dataStore
                    .data
                    .map { preferences ->
                        val dietaryRestrictionsJson = preferences[DIETARY_RESTRICTIONS_KEY]
                        val cuisinePreferencesJson = preferences[CUISINE_PREFERENCES_KEY]
                        val priceRange = preferences[PRICE_RANGE_KEY] ?: 2 // Default price range

                        val dietaryRestrictions = parseJsonToList(dietaryRestrictionsJson)
                        val cuisinePreferences = parseJsonToList(cuisinePreferencesJson)

                        UserPreferences(
                                dietaryRestrictions = dietaryRestrictions,
                                cuisinePreferences = cuisinePreferences,
                                priceRange = priceRange
                        )
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = UserPreferences() // Provide a default initial value
                    )

    companion object {
        private val DIETARY_RESTRICTIONS_KEY = stringPreferencesKey("dietary_restrictions")
        private val CUISINE_PREFERENCES_KEY = stringPreferencesKey("cuisine_preferences")
        private val PRICE_RANGE_KEY = intPreferencesKey("price_range")
    }

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
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    null
            )
    val detailsLoading: StateFlow<Boolean> =
            restaurantController.detailsLoading.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    false
            )
    val detailsErrorMessage: StateFlow<String?> =
            restaurantController.detailsErrorMessage.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    null
            )

    private val _visibleRestaurantCount = MutableStateFlow(20) // Initial count from HomeScreen

    // Helper to parse JSON string to List<String>
    private fun parseJsonToList(json: String?): List<String> {
        return json?.trim('[', ']')?.split(",")?.map { it.trim('"', ' ') }?.filter {
            it.isNotEmpty()
        }
                ?: emptyList()
    }

    val paginatedRestaurants: StateFlow<List<Restaurant>> =
            combine(
                            nearbyRestaurants, // This is the ViewModel's StateFlow mirroring the
                            // controller's
                            _visibleRestaurantCount
                    ) { allRestaurantsList, count -> allRestaurantsList.take(count) }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

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

        // React to changes in permission, preferences, or search query to fetch restaurants
        viewModelScope.launch {
            combine(
                            hasLocationPermission,
                            currentUserPreferences,
                            searchQuery.debounce(500) // Debounce search query by 500ms
                    ) { hasPermission, preferences, query ->
                Triple(hasPermission, preferences, query)
            }
                    .collectLatest { (hasPermission, preferences, query) ->
                        if (hasPermission) {
                            Log.d(
                                    TAG,
                                    "Criteria met for fetch: HasPermission=$hasPermission, Prefs: $preferences, Query: '$query'"
                            )
                            // ViewModel's own fetchNearbyRestaurants will handle passing all
                            // necessary params
                            fetchNearbyRestaurants()
                        } else {
                            Log.d(
                                    TAG,
                                    "Criteria NOT met for fetch: HasPermission=$hasPermission, Prefs: $preferences, Query: '$query'"
                            )
                        }
                    }
        }
        setupAutomaticLocalPagination()
        _snackbarMessage.value = null
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
        val permissionPreviouslyGranted = _hasLocationPermission.value
        _hasLocationPermission.update { granted }
        // The combine block in init will handle fetching if permission is newly granted
        if (granted && !permissionPreviouslyGranted) {
            Log.d(TAG, "Location permission newly granted.")
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
            if (!_hasLocationPermission.value) {
                Log.d(TAG, "fetchNearbyRestaurants: Location permission not granted.")
                _restaurantErrorMessage.value = "Location permission required."
                return@launch
            }

            val prefs = currentUserPreferences.value
            val currentQuery = searchQuery.value // Get current search query
            Log.d(
                    TAG,
                    "ViewModel: Fetching nearby restaurants. Preferences: $prefs, SearchQuery: '$currentQuery'"
            )

            if (userLocation.value == null) {
                Log.d(
                        TAG,
                        "User location not available, attempting to get it first for restaurant fetch."
                )
                restaurantController.getUserLocation(
                        onSuccess = { latLng ->
                            Log.d(
                                    TAG,
                                    "Location obtained: $latLng. Now fetching restaurants with prefs: $prefs, query: '$currentQuery'"
                            )
                            viewModelScope.launch { // Launch new coroutine for restaurant fetch
                                restaurantController.fetchNearbyRestaurants(
                                        userPreferences = prefs,
                                        searchQuery = currentQuery
                                )
                            }
                        },
                        onError = { error ->
                            Log.e(TAG, "Failed to get location for restaurant fetch: $error")
                            _restaurantErrorMessage.value = "Failed to get location: $error"
                        }
                )
            } else {
                Log.d(
                        TAG,
                        "User location already available. Fetching restaurants with prefs: $prefs, query: '$currentQuery'"
                )
                restaurantController.fetchNearbyRestaurants(
                        userPreferences = prefs,
                        searchQuery = currentQuery
                )
            }
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
        _selectedRestaurantForDetail.value = restaurant
        _showRestaurantDetailModal.value = true
        viewModelScope.launch {
            Log.d(TAG, "Fetching details for restaurant ID: ${restaurant.id}")
            restaurantController.fetchRestaurantDetails(restaurant.id)
        }
    }

    fun onDismissRestaurantDetailModal() {
        _showRestaurantDetailModal.value = false
        viewModelScope.launch { clearRestaurantDetails() }
        _selectedRestaurantForDetail.value = null
    }

    fun requestMoreLocalRestaurants() {
        if (_visibleRestaurantCount.value < nearbyRestaurants.value.size &&
                        !restaurantIsLoadingMore.value
        ) {
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
                    ) { restaurants, isLoadingMore -> Pair(restaurants, isLoadingMore) }
                    .collectLatest { (restaurants, isLoadingMore) ->
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
        }
                ?: run {
                    Log.e(TAG, "No group selected to send.")
                    _snackbarMessage.value = "Error: No group selected."
                }
    }

    // Function to clear the snackbar message after it has been shown
    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    // Function to clear restaurant details and error
    fun clearRestaurantDetails() {
        viewModelScope.launch { restaurantController.fetchRestaurantDetails("") }
    }

    // Function to clear all restaurant data for the map and list
    fun clearMapAndRestaurantList() {
        _nearbyRestaurants.update { emptyList() }
        _checkedRestaurantIds.update { emptySet() } // Clear any selected items
        _visibleRestaurantCount.update { 0 } // Reset pagination for the list view
        Log.d(TAG, "Cleared all restaurant data from ViewModel.")
    }

    // Function to search based on map area and current preferences
    fun searchMapArea(center: LatLng, radiusInMeters: Double) {
        viewModelScope.launch {
            if (!_hasLocationPermission.value) {
                Log.d(TAG, "searchMapArea: Location permission not granted.")
                _restaurantErrorMessage.value = "Location permission required."
                return@launch
            }

            val prefs = currentUserPreferences.value
            // We ignore the text searchQuery for "Search Here"
            Log.d(
                    TAG,
                    "ViewModel: Searching map area. Center: $center, Radius: $radiusInMeters, Preferences: $prefs"
            )

            restaurantController.fetchNearbyRestaurantsByArea(
                    location = center,
                    radius = radiusInMeters.toInt(),
                    userPreferences = prefs
            )
        }
    }
}
