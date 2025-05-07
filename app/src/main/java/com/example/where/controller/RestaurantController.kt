package com.example.where.controller

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.where.model.NearbySearchResponse
import com.example.where.model.PlaceDetailResult
import com.example.where.model.Restaurant
import com.example.where.model.RestaurantApiClient
import com.example.where.model.UserPreferences
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

class RestaurantController
@Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("mapsApiKey") private val apiKey: String
) {

    // User's current location
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    // User's current city
    private val _userCity = MutableStateFlow<String?>(null)
    val userCity: StateFlow<String?> = _userCity.asStateFlow()

    // List of nearby restaurants
    private val _nearbyRestaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val nearbyRestaurants: StateFlow<List<Restaurant>> = _nearbyRestaurants.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Loading more state (for pagination)
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Pagination token (for loading more results)
    private var nextPageToken: String? = null

    // Are more results available from the API?
    private val _hasMoreResults = MutableStateFlow(false)
    val hasMoreResults: StateFlow<Boolean> = _hasMoreResults.asStateFlow()

    // State for individual restaurant details
    private val _restaurantDetails = MutableStateFlow<PlaceDetailResult?>(null)
    val restaurantDetails: StateFlow<PlaceDetailResult?> = _restaurantDetails.asStateFlow()

    private val _detailsLoading = MutableStateFlow(false)
    val detailsLoading: StateFlow<Boolean> = _detailsLoading.asStateFlow()

    private val _detailsErrorMessage = MutableStateFlow<String?>(null)
    val detailsErrorMessage: StateFlow<String?> = _detailsErrorMessage.asStateFlow()

    // Location client
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Check if we have location permissions
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Get user's current location
    suspend fun getUserLocation(onSuccess: (LatLng) -> Unit, onError: (String) -> Unit) {
        Log.d("RestaurantController", "Getting user location...")
        try {
            if (!hasLocationPermission()) {
                throw SecurityException("Location permission not granted")
            }

            val location = getLocationSuspend()
            _userLocation.value = location
            getUserCity(location)
            onSuccess(location)
        } catch (e: SecurityException) {
            Log.e("RestaurantController", "Security exception: ${e.message}")
            onError("Location permission not granted")

            // use a default location on permission error
            Log.d(
                "RestaurantController",
                "Permission not granted, using default location for emulator testing"
            )
            val defaultLatLng = LatLng(37.7749, -122.4194)
            _userLocation.value = defaultLatLng
            onSuccess(defaultLatLng)
        } catch (e: Exception) {
            Log.e("RestaurantController", "Error getting location: ${e.message}")

            // use a default location on failure
            Log.d(
                "RestaurantController",
                "Error occurred, using default location for emulator testing"
            )
            val defaultLatLng = LatLng(37.7749, -122.4194)
            _userLocation.value = defaultLatLng
            onSuccess(defaultLatLng)
        }
    }

    private suspend fun getUserCity(loc: LatLng) {
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val list =
                    geocoder.getFromLocation(loc.latitude, loc.longitude, /* maxResults = */ 1)
                val cityName = if (list.isNullOrEmpty()) {
                    "Unknown"
                } else {
                    val addr = list[0]
                    // prefer locality (city), then subAdminArea, then adminArea
                    addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Unknown"
                }
                _userCity.value = cityName
                Log.d("RestaurantController", "Resolved city: $cityName")
            } catch (ioe: IOException) {
                Log.e("RestaurantController", "Geocoder I/O exception", ioe)
                _userCity.value = "Unknown"
            } catch (e: Exception) {
                Log.e("RestaurantController", "Unexpected error in geocoding", e)
                _userCity.value = "Unknown"
            }
        }
    }

    // Properly suspending function to get location
    private suspend fun getLocationSuspend(): LatLng = suspendCancellableCoroutine { continuation ->
        try {
            // Double-check permission before accessing location
            if (!hasLocationPermission()) {
                continuation.resumeWithException(
                    SecurityException("Location permission not granted")
                )
                return@suspendCancellableCoroutine
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        Log.d("RestaurantController", "Location found: $latLng")
                        continuation.resume(latLng)
                    } else {
                        // if no location available, use a default
                        // location
                        Log.d(
                            "RestaurantController",
                            "No location available, using default for emulator testing"
                        )
                        // San Francisco coordinates
                        val defaultLatLng = LatLng(37.7749, -122.4194)
                        continuation.resume(defaultLatLng)
                    }
                }.addOnFailureListener { exception ->
                    Log.e("RestaurantController", "Location failure: ${exception.message}")

                    // use a default location on failure
                    Log.d(
                        "RestaurantController",
                        "Location failure, using default for emulator testing"
                    )
                    val defaultLatLng = LatLng(37.7749, -122.4194)
                    continuation.resume(defaultLatLng)
                }

            // cancellation handler
            continuation.invokeOnCancellation {
                Log.d("RestaurantController", "Location request cancelled")
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    // Fetch nearby restaurants (initial load)
    suspend fun fetchNearbyRestaurants(
        userPreferences: UserPreferences? = null, searchQuery: String? = null, radius: Int = 8046
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        nextPageToken = null
        _nearbyRestaurants.value = emptyList() // Clear existing results for a fresh search

        Log.d(
            "RestaurantController",
            "Fetching nearby restaurants with radius: $radius, preferences: $userPreferences, searchQuery: '$searchQuery'"
        )

        if (apiKey.isEmpty()) {
            Log.e("RestaurantController", "API key not initialized or empty")
            _errorMessage.value = "Maps API key not configured"
            _isLoading.value = false
            return
        }

        // Use the stored user location for this function
        val locationToSearch = _userLocation.value

        if (locationToSearch == null) {
            Log.e("RestaurantController", "Location to search is null")
            _errorMessage.value = "Location not available"
            _isLoading.value = false
            return
        }

        // Pass the determined location and the explicit rankBy logic to the page fetcher
        fetchRestaurantsPage(
            locationToSearch,
            radius,
            userPreferences,
            searchQuery,
            explicitLocationProvided = false // This is key for original behavior
        )
    }

    // function for searching a map area without a text query
    suspend fun fetchNearbyRestaurantsByArea(
        location: LatLng, radius: Int, userPreferences: UserPreferences?
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        nextPageToken = null
        _nearbyRestaurants.value = emptyList()

        Log.d(
            "RestaurantController",
            "Fetching nearby restaurants by AREA: location=$location, radius=$radius, preferences: $userPreferences"
        )

        if (apiKey.isEmpty()) {
            Log.e("RestaurantController", "API key not initialized or empty")
            _errorMessage.value = "Maps API key not configured"
            _isLoading.value = false
            return
        }

        fetchRestaurantsPage(
            location, radius, userPreferences, searchQuery = null, explicitLocationProvided = true
        )
    }

    // Fetch more results (pagination)
    suspend fun fetchMoreRestaurants() {
        if (!hasMoreResults.value || _isLoadingMore.value || nextPageToken == null) {
            Log.d(
                "RestaurantController",
                "Cannot fetch more: hasMoreResults=${hasMoreResults.value}, isLoadingMore=${_isLoadingMore.value}, hasToken=${nextPageToken != null}"
            )
            return // No more results or already loading
        }

        Log.d("RestaurantController", "Starting to load more, setting isLoadingMore=true")
        _isLoadingMore.value = true

        val location = _userLocation.value
        if (location == null) {
            resetLoadingState()
            Log.d("RestaurantController", "User location is null, setting isLoadingMore=false")
            return
        }

        // Store the current token before fetching
        val tokenToUse = nextPageToken

        // Places API requires a short delay before using the next_page_token
        delay(2000)

        try {
            withContext(Dispatchers.IO) {
                Log.d(
                    "RestaurantController",
                    "Fetching next page with token: ${tokenToUse?.take(10)}..."
                )

                val response = RestaurantApiClient.placesApiService.getNearbyRestaurants(
                    location = "${location.latitude},${location.longitude}",
                    apiKey = apiKey,
                    pageToken = tokenToUse,
                )

                processResponse(response, location)
            }
        } catch (e: Exception) {
            Log.e("RestaurantController", "Error fetching more restaurants: ${e.message}", e)
            Log.d("RestaurantController", "Error caught, setting isLoadingMore=false")
            _isLoadingMore.value = false

            // If we've used the token but got an error, clear it to prevent reuse
            if (nextPageToken == tokenToUse) {
                nextPageToken = null
                _hasMoreResults.value = false
            }
        }
    }

    private suspend fun fetchRestaurantsPage(
        location: LatLng,
        radius: Int,
        userPreferences: UserPreferences? = null,
        searchQuery: String? = null,
        explicitLocationProvided: Boolean = false
    ) {
        try {
            withContext(Dispatchers.IO) {
                val locationString = "${location.latitude},${location.longitude}"

                var minPrice: Int? = null
                var maxPrice: Int? = null
                val keywordsCollector = mutableListOf<String>()

                // Prioritize user's direct search query
                if (!searchQuery.isNullOrBlank()) {
                    keywordsCollector.add(searchQuery.trim().lowercase())
                    // If user searches, ignore saved cuisine preferences, but keep dietary.
                    userPreferences?.dietaryRestrictions?.forEach { restriction ->
                        keywordsCollector.add(restriction.lowercase())
                    }
                } else {
                    // No direct search query, so use saved preferences (cuisine and dietary)
                    userPreferences?.let {
                        it.cuisinePreferences.forEach { cuisine ->
                            keywordsCollector.add(cuisine.lowercase())
                        }
                        it.dietaryRestrictions.forEach { restriction ->
                            keywordsCollector.add(restriction.lowercase())
                        }
                    }
                }

                // Apply price preferences regardless of search query
                userPreferences?.let {
                    when (it.priceRange) {
                        1 -> maxPrice = 1
                        2 -> maxPrice = 2
                        3 -> maxPrice = 3
                        4 -> minPrice = 4
                    }
                }

                val finalKeyword =
                    if (keywordsCollector.isNotEmpty()) keywordsCollector.joinToString(" ")
                    else null

                var rankByQuery: String? = "distance"

                if (finalKeyword != null || explicitLocationProvided) {
                    rankByQuery =
                        null
                }

                Log.d(
                    "RestaurantController",
                    "Making API call: loc=$locationString, finalKeyword='$finalKeyword', minPrice=$minPrice, maxPrice=$maxPrice, rankBy=$rankByQuery, radius=$radius (eff radius: ${if (rankByQuery == "distance") null else radius})"
                )

                val response = RestaurantApiClient.placesApiService.getNearbyRestaurants(
                    location = locationString,
                    apiKey = apiKey,
                    radius = if (rankByQuery == "distance") null else radius,
                    keyword = finalKeyword,
                    minprice = minPrice,
                    maxprice = maxPrice,
                    rankby = rankByQuery
                )

                processResponse(response, location)
            }
        } catch (e: Exception) {
            Log.e("RestaurantController", "Exception fetching restaurants: ${e.message}", e)
            _errorMessage.value = "Error fetching restaurants: ${e.message}"
            _isLoading.value = false
        }
    }

    private fun processResponse(response: NearbySearchResponse, location: LatLng) {
        if (response.status == "OK") {
            Log.d(
                "RestaurantController",
                "API response successful: ${response.results.size} restaurants found"
            )

            // Save next page token if available
            nextPageToken = response.next_page_token
            _hasMoreResults.value = nextPageToken != null

            // Skip processing if no new results
            if (response.results.isEmpty()) {
                Log.d(
                    "RestaurantController",
                    "No new restaurants in response, setting hasMoreResults=false"
                )
                nextPageToken = null
                _hasMoreResults.value = false
                resetLoadingState()
                return
            }

            // Convert API results to Restaurant objects
            val newRestaurants = response.results.map { place ->
                val placeLocation = LatLng(place.geometry.location.lat, place.geometry.location.lng)

                // Determine the location to calculate distance from
                val locationForDistanceCalc = _userLocation.value ?: location

                // Calculate distance from user's actual location (or fallback)
                val distanceInMeters = calculateDistance(
                    locationForDistanceCalc.latitude,
                    locationForDistanceCalc.longitude,
                    placeLocation.latitude,
                    placeLocation.longitude
                )

                // Format distance string (convert meters to miles)
                val distanceInMiles = (distanceInMeters / 1609.34)
                val formattedDistance = if (distanceInMiles < 0.1) {
                    "< 0.1 miles away"
                } else {
                    "${(distanceInMiles * 10).roundToInt() / 10.0} miles away"
                }

                // Determine category from types
                val category = when {
                    place.types.contains("cafe") -> "Cafe"
                    place.types.contains("bar") -> "Bar"
                    place.types.contains("restaurant") -> "Restaurant"
                    place.types.contains("bakery") -> "Bakery"
                    else -> "Restaurant"
                }

                val photoMetadataList = place.photos?.map {
                    com.example.where.model.PhotoMetadata(
                        reference = it.photo_reference, attributions = it.html_attributions
                    )
                }

                Restaurant(
                    id = place.place_id,
                    name = place.name,
                    location = placeLocation,
                    address = place.vicinity,
                    rating = place.rating ?: 0f,
                    priceLevel = place.price_level ?: 0,
                    category = category,
                    distance = formattedDistance,
                    distanceInMeters = distanceInMeters,
                    photoMetadataList = photoMetadataList
                )
            }

            // Add new restaurants to existing list and sort by distance
            val allRestaurants = _nearbyRestaurants.value + newRestaurants
            val sortedRestaurants =
                allRestaurants.distinctBy { it.id }.sortedBy { it.distanceInMeters }
            Log.d("RestaurantController", "Processed ${sortedRestaurants.size} total restaurants")

            _nearbyRestaurants.value = sortedRestaurants
        } else {
            Log.e("RestaurantController", "API Error: ${response.status}")
            _errorMessage.value = "API Error: ${response.status}"
            _hasMoreResults.value = false
            nextPageToken = null
        }

        resetLoadingState()
        Log.d(
            "RestaurantController",
            "End of processResponse: isLoading=false, isLoadingMore=false, hasMoreResults=${_hasMoreResults.value}"
        )
    }

    // Calculate distance between two coordinates (in meters)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    // Helper method to reset loading states
    fun resetLoadingState() {
        Log.d("RestaurantController", "Explicitly resetting loading states")
        _isLoading.value = false
        _isLoadingMore.value = false
    }

    // Fetch details for a specific restaurant
    suspend fun fetchRestaurantDetails(placeId: String) {
        _detailsLoading.value = true
        _detailsErrorMessage.value = null
        _restaurantDetails.value = null
        Log.d("RestaurantController", "Fetching details for placeId: $placeId")

        if (apiKey.isEmpty()) {
            Log.e("RestaurantController", "API key not initialized or empty for details fetch")
            _detailsErrorMessage.value = "Maps API key not configured"
            _detailsLoading.value = false
            return
        }

        val fieldsToFetch =
            "name,formatted_address,international_phone_number,website,opening_hours,rating,user_ratings_total,price_level,photo,geometry,place_id,vicinity,types"

        try {
            val response = withContext(Dispatchers.IO) {
                RestaurantApiClient.placesApiService.getRestaurantDetails(
                    placeId = placeId, fields = fieldsToFetch, apiKey = apiKey
                )
            }

            if (response.status == "OK" && response.result != null) {
                _restaurantDetails.value = response.result
                Log.d(
                    "RestaurantController", "Successfully fetched details: ${response.result.name}"
                )
            } else {
                Log.e(
                    "RestaurantController",
                    "Error fetching details: ${response.status} - ${response.errorMessage}"
                )
                _detailsErrorMessage.value =
                    response.errorMessage ?: "Error fetching details: ${response.status}"
            }
        } catch (e: Exception) {
            Log.e("RestaurantController", "Exception fetching details: ${e.message}", e)
            _detailsErrorMessage.value = "Exception: ${e.message}"
        } finally {
            _detailsLoading.value = false
        }
    }
}
