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
import com.example.where.controller.UserController
import com.example.where.controller.UserPreferencesController
import com.example.where.model.Group
import com.example.where.model.PlaceDetailResult
import com.example.where.model.Restaurant
import com.example.where.model.UserPreferences
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
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
        private val userPreferencesController: UserPreferencesController,
        private val userController: UserController,
        private val firebaseAuth: FirebaseAuth,
        dataStore: DataStore<Preferences>
) : ViewModel() {

    // User Preferences for the CURRENT user (from DataStore)
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
                            initialValue =
                                    UserPreferences()
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

    // State for Filter Modal
    private val _showFilterModal = MutableStateFlow(false)
    val showFilterModal: StateFlow<Boolean> = _showFilterModal.asStateFlow()

    private val _selectedCuisineFilters = MutableStateFlow(emptySet<String>())
    val selectedCuisineFilters: StateFlow<Set<String>> = _selectedCuisineFilters.asStateFlow()

    private var hasOpenedFilterModal = false // Flag to track first opening

    // State for Group Filters
    private val _selectedGroupFilters = MutableStateFlow(emptySet<String>())
    val selectedGroupFilters: StateFlow<Set<String>> = _selectedGroupFilters.asStateFlow()

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

    // This state reflects the poll operation loading state from the controller
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
        // Observe the renamed state for poll operations
        viewModelScope.launch {
            groupController.isOperatingWithPoll.collect { _isAddingRestaurantsToGroup.value = it }
        }

        // Observe the renamed success/error states for poll operations
        viewModelScope.launch {
            groupController.operationSuccess.collect { success ->
                if (success) {
                    Log.d(TAG, "Observed generic operationSuccess = true")
                    groupController.clearOperationStatus()
                }
            }
        }
        viewModelScope.launch {
            groupController.operationError.collect { error ->
                if (error != null) {
                    Log.d(TAG, "Observed generic operationError = $error")
                    groupController.clearOperationStatus()
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

    // Called by the UIs "Load More" button to fetch additional restaurants from the API.
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
        val selectedGroup = _selectedGroupForModal.value
        val currentUserEmail = firebaseAuth.currentUser?.email
        val restaurantIdsToSend = _checkedRestaurantIds.value

        if (selectedGroup == null) {
            Log.e(TAG, "No group selected to send restaurants to.")
            _snackbarMessage.value = "Error: No group selected."
            return
        }
        if (currentUserEmail == null) {
            Log.e(TAG, "User not authenticated to send restaurants.")
            _snackbarMessage.value = "Error: User not authenticated."
            return
        }
        if (restaurantIdsToSend.isEmpty()) {
            Log.w(TAG, "No restaurants selected to send.")
            _snackbarMessage.value = "No restaurants selected to send."
            _showSendModal.value = false
            return
        }

        viewModelScope.launch {

            val newPollId =
                    groupController.createPollInGroupWithRestaurants(
                            groupId = selectedGroup.id,
                            restaurantIds = restaurantIdsToSend,
                            currentUserEmail = currentUserEmail
                    )

            if (newPollId != null) {
                _snackbarMessage.value = "New poll created with selected restaurants!"
                _checkedRestaurantIds.value = emptySet() // Clear selection on success
                _showSendModal.value = false // Dismiss modal on success
            } else {
                // Failure occurred, get the error message from the controller
                _snackbarMessage.value =
                        groupController.operationError.value ?: "Failed to create poll."
            }
            // Clear controller status flags regardless of success/failure after the operation
            // attempt
            groupController.clearOperationStatus()
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

    // Event Handlers for Filter Modal
    fun onFilterButtonClicked() {
        viewModelScope.launch { groupController.fetchUserGroups() }
        if (!hasOpenedFilterModal) {
            // First time opening: Initialize with all user preferences
            val currentPrefs = currentUserPreferences.value.cuisinePreferences
            _selectedCuisineFilters.value = currentPrefs.toSet()
            _selectedGroupFilters.value = emptySet() // Initialize group filters as empty
        }
        // For subsequent openings, _selectedCuisineFilters retains its last state
        _showFilterModal.value = true
    }

    fun onDismissFilterModal() {
        _showFilterModal.value = false
    }

    fun onCuisineFilterChanged(cuisine: String, isChecked: Boolean) {
        if (isChecked && _selectedGroupFilters.value.isNotEmpty()) {
            // If a cuisine is being checked AND a group filter is active, clear group filters
            _selectedGroupFilters.value = emptySet()
        }
        _selectedCuisineFilters.update { currentFilters ->
            if (isChecked) {
                currentFilters + cuisine
            } else {
                currentFilters - cuisine
            }
        }
    }

    fun onGroupFilterChanged(groupId: String, isChecked: Boolean) {
        if (isChecked) {
            // A group is being checked
            _selectedGroupFilters.value = setOf(groupId) // Set this group as the only selected one
            _selectedCuisineFilters.value = emptySet() // Clear all cuisine filters
        } else {
            // A group is being unchecked
            _selectedGroupFilters.value = emptySet() // No group is selected
        }
    }

    fun applyFilters() {
        viewModelScope.launch {
            // Determine if a group filter is active
            val selectedGroupId = _selectedGroupFilters.value.firstOrNull()

            var preferencesToApply: UserPreferences = currentUserPreferences.value

            if (selectedGroupId != null) {
                val group = userGroups.value.find { it.id == selectedGroupId }
                if (group != null) {
                    val memberEmails = group.members
                    Log.d(TAG, "Fetching preferences for group members (emails): $memberEmails")

                    val memberPreferencesList = mutableListOf<UserPreferences>()
                    if (memberEmails.isEmpty()) {
                        Log.d(TAG, "Group $selectedGroupId has no members listed.")
                    } else {
                        memberEmails.forEach { email ->
                            val uid = userController.getUidForEmail(email) // Get UID for email
                            if (uid != null) {
                                val prefsResult = userPreferencesController.getUserPreferences(uid)
                                if (prefsResult.isSuccess) {
                                    prefsResult.getOrNull()?.let {
                                        Log.d(
                                                TAG,
                                                "Successfully fetched preferences for member (UID $uid, Email $email): $it"
                                        )
                                        memberPreferencesList.add(it)
                                    }
                                } else {
                                    Log.e(
                                            TAG,
                                            "Failed to fetch preferences for member (UID $uid, Email $email): ${prefsResult.exceptionOrNull()?.message}"
                                    )
                                }
                            } else {
                                Log.w(
                                        TAG,
                                        "Could not find UID for email: $email. Skipping preferences for this member."
                                )
                            }
                        }
                    }

                    if (memberPreferencesList.isNotEmpty()) {
                        Log.d(
                                TAG,
                                "Aggregating preferences for ${memberPreferencesList.size} members."
                        )
                        preferencesToApply = aggregateGroupPreferences(memberPreferencesList)
                        Log.d(TAG, "Aggregated group preferences: $preferencesToApply")
                    } else {
                        Log.d(
                                TAG,
                                "No member preferences available for group $selectedGroupId, using default group search parameters."
                        )
                        preferencesToApply =
                                UserPreferences(
                                        cuisinePreferences = emptyList(),
                                        dietaryRestrictions = emptyList(),
                                        priceRange = 1
                                )
                    }
                } else {
                    Log.w(TAG, "Selected group with ID $selectedGroupId not found in userGroups.")
                    preferencesToApply =
                            currentUserPreferences.value.copy(
                                    cuisinePreferences = _selectedCuisineFilters.value.toList()
                            )
                }
            } else {
                Log.d(
                        TAG,
                        "No group filter active. Applying individual cuisine filters: ${_selectedCuisineFilters.value}"
                )
                preferencesToApply =
                        currentUserPreferences.value.copy(
                                cuisinePreferences = _selectedCuisineFilters.value.toList()
                        )
            }

            Log.d(TAG, "Applying filters. Final effective preferences: $preferencesToApply")

            if (_userLocation.value == null) {
                Log.d(
                        TAG,
                        "User location not available, attempting to get it first for filter application."
                )
                restaurantController.getUserLocation(
                        onSuccess = { latLng ->
                            Log.d(
                                    TAG,
                                    "Location obtained: $latLng. Now fetching restaurants with effective prefs: $preferencesToApply"
                            )
                            viewModelScope.launch {
                                restaurantController.fetchNearbyRestaurants(
                                        userPreferences = preferencesToApply,
                                        searchQuery = null
                                )
                            }
                        },
                        onError = { error ->
                            Log.e(TAG, "Failed to get location for filter application: $error")
                            _restaurantErrorMessage.value = "Failed to get location: $error"
                        }
                )
            } else {
                Log.d(
                        TAG,
                        "User location available. Fetching restaurants with effective prefs: $preferencesToApply"
                )
                restaurantController.fetchNearbyRestaurants(
                        userPreferences = preferencesToApply,
                        searchQuery = null
                )
            }

            hasOpenedFilterModal = true
            _showFilterModal.value = false
        }
    }

    private fun aggregateGroupPreferences(
            memberPreferencesList: List<UserPreferences>
    ): UserPreferences {
        if (memberPreferencesList.isEmpty()) {
            return UserPreferences(
                    priceRange = 1, // Default to lowest price
                    dietaryRestrictions = emptyList(),
                    cuisinePreferences = emptyList()
            )
        }

        // Aggregate Price Level (lowest preference)
        val minPriceRange = memberPreferencesList.minOfOrNull { it.priceRange } ?: 1

        // Aggregate Dietary Restrictions (all unique)
        val aggregatedDietaryRestrictions =
                memberPreferencesList.flatMap { it.dietaryRestrictions }.distinct()

        // Aggregate Cuisine Preferences (top 2 by count, random tie-breaking)
        val cuisineCounts = mutableMapOf<String, Int>()
        memberPreferencesList.forEach { userPrefs ->
            userPrefs.cuisinePreferences.forEach {
                cuisineCounts[it] = (cuisineCounts[it] ?: 0) + 1
            }
        }

        val finalSelectedCuisines = mutableListOf<String>()
        if (cuisineCounts.isNotEmpty()) {
            val sortedCuisineEntries = cuisineCounts.entries.sortedByDescending { it.value }
            val maxCount = sortedCuisineEntries.first().value
            val cuisinesWithMaxCount =
                    sortedCuisineEntries.filter { it.value == maxCount }.map { it.key }

            if (cuisinesWithMaxCount.size >= 2) {
                finalSelectedCuisines.addAll(cuisinesWithMaxCount.shuffled(Random).take(2))
            } else { // cuisinesWithMaxCount.size == 1
                finalSelectedCuisines.add(cuisinesWithMaxCount.first())
                val remainingCuisineEntries = sortedCuisineEntries.filter { it.value < maxCount }
                if (remainingCuisineEntries.isNotEmpty()) {
                    val secondMaxCount = remainingCuisineEntries.first().value
                    val cuisinesWithSecondMaxCount =
                            remainingCuisineEntries.filter { it.value == secondMaxCount }.map {
                                it.key
                            }
                    finalSelectedCuisines.addAll(
                            cuisinesWithSecondMaxCount.shuffled(Random).take(1)
                    )
                }
            }
        }

        return UserPreferences(
                dietaryRestrictions = aggregatedDietaryRestrictions,
                cuisinePreferences = finalSelectedCuisines,
                priceRange = minPriceRange
        )
    }

    fun clearOperationStatus() {
        groupController.clearOperationStatus()
    }
}
