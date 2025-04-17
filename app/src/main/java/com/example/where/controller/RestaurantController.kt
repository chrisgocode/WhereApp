package com.example.where.controller

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.where.model.NearbySearchResponse
import com.example.where.model.Restaurant
import com.example.where.model.RestaurantApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import android.Manifest
import android.location.Geocoder
import java.io.IOException
import java.util.Locale

class RestaurantController(private val context: Context) {

    // API key from BuildConfig (will be accessed from MainActivity)
    private lateinit var apiKey: String

    // User's current location
    private var _userLocation = mutableStateOf<LatLng?>(null)
    val userLocation = _userLocation

    // User's current city
    private var _userCity = mutableStateOf<String?>(null)
    val userCity = _userCity

    // List of nearby restaurants
    private var _nearbyRestaurants = mutableStateOf<List<Restaurant>>(emptyList())
    val nearbyRestaurants = _nearbyRestaurants

    // Loading state
    private var _isLoading = mutableStateOf(false)
    val isLoading = _isLoading

    // Loading more state (for pagination)
    private var _isLoadingMore = mutableStateOf(false)
    val isLoadingMore = _isLoadingMore

    // Error state
    private var _errorMessage = mutableStateOf<String?>(null)
    val errorMessage = _errorMessage

    // Pagination token (for loading more results)
    private var nextPageToken: String? = null

    // Are more results available from the API?
    private var _hasMoreResults = mutableStateOf(false)
    val hasMoreResults = _hasMoreResults

    // Location client
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Set the API key
    fun setApiKey(key: String) {
        Log.d("RestaurantController", "Setting API key: ${key.take(5)}...")
        apiKey = key
    }

    // Check if we have location permissions
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
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
            
            // For emulator testing - use a default location on permission error
            Log.d(
                "RestaurantController",
                "Permission not granted, using default location for emulator testing"
            )
            val defaultLatLng = LatLng(37.7749, -122.4194)
            _userLocation.value = defaultLatLng
            onSuccess(defaultLatLng)
        } catch (e: Exception) {
            Log.e("RestaurantController", "Error getting location: ${e.message}")
            
            // For emulator testing - use a default location on failure
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
                val list = geocoder.getFromLocation(
                    loc.latitude,
                    loc.longitude,
                    /* maxResults = */ 1
                )
                val cityName = if (list.isNullOrEmpty()) {
                    "Unknown"
                } else {
                    val addr = list[0]
                    // prefer locality (city), then subAdminArea, then adminArea
                    addr.locality
                        ?: addr.subAdminArea
                        ?: addr.adminArea
                        ?: "Unknown"
                }
                _userCity.value = cityName
                Log.d("RestaurantController", "Resolved city: $cityName")
            }
            catch (ioe: IOException) {
                Log.e("RestaurantController", "Geocoder I/O exception", ioe)
                _userCity.value = "Unknown"
            }
            catch (e: Exception) {
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
                continuation.resumeWithException(SecurityException("Location permission not granted"))
                return@suspendCancellableCoroutine
            }
            
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        Log.d("RestaurantController", "Location found: $latLng")
                        continuation.resume(latLng)
                    } else {
                        // For emulator testing - if no location available, use a default location
                        Log.d(
                            "RestaurantController",
                            "No location available, using default for emulator testing"
                        )
                        // San Francisco coordinates
                        val defaultLatLng = LatLng(37.7749, -122.4194)
                        continuation.resume(defaultLatLng)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RestaurantController", "Location failure: ${exception.message}")
                    
                    // For emulator testing - use a default location on failure
                    Log.d(
                        "RestaurantController",
                        "Location failure, using default for emulator testing"
                    )
                    val defaultLatLng = LatLng(37.7749, -122.4194)
                    continuation.resume(defaultLatLng)
                    
                    // In a production app, you might want to propagate the error instead:
                    // continuation.resumeWithException(exception)
                }

            // Register cancellation handler
            continuation.invokeOnCancellation {
                // Clean up resources if needed
                Log.d("RestaurantController", "Location request cancelled")
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    // TODO:
    //  Centralize radius variable, maybe a filter for users to change?
    // Fetch nearby restaurants (initial load)
    suspend fun fetchNearbyRestaurants(radius: Int = 8046) { // 5 mile radius
        _isLoading.value = true
        _errorMessage.value = null
        nextPageToken = null
        _nearbyRestaurants.value = emptyList() // Clear existing results for a fresh search

        Log.d("RestaurantController", "Fetching nearby restaurants with radius: $radius")

        if (!::apiKey.isInitialized || apiKey.isEmpty()) {
            Log.e("RestaurantController", "API key not initialized or empty")
            _errorMessage.value = "Maps API key not configured"
            _isLoading.value = false
            return
        }

        val location = _userLocation.value
        if (location == null) {
            Log.e("RestaurantController", "User location is null")
            _errorMessage.value = "Location not available"
            _isLoading.value = false
            return
        }

        fetchRestaurantsPage(location, radius)
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
                    rankby = "distance" // Sort by distance instead of the default (prominence/popularity)
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

    private suspend fun fetchRestaurantsPage(location: LatLng, radius: Int) {
        try {
            withContext(Dispatchers.IO) {
                val locationString = "${location.latitude},${location.longitude}"
                Log.d("RestaurantController", "Making API request with location: $locationString")

                val response =
                    RestaurantApiClient.placesApiService.getNearbyRestaurants(
                        location = locationString,
                        apiKey = apiKey,
                        rankby = "distance" // Sort by distance instead of the default (prominence/popularity)
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
            val newRestaurants =
                response.results.map { place ->
                    val placeLocation =
                        LatLng(
                            place.geometry.location.lat,
                            place.geometry.location.lng
                        )

                    // Calculate distance from user
                    val distanceInMeters =
                        calculateDistance(
                            location.latitude,
                            location.longitude,
                            placeLocation.latitude,
                            placeLocation.longitude
                        )

                    // Format distance string (convert meters to miles)
                    val distanceInMiles = (distanceInMeters / 1609.34)
                    val formattedDistance =
                        if (distanceInMiles < 0.1) {
                            "< 0.1 miles away"
                        } else {
                            "${(distanceInMiles * 10).roundToInt() / 10.0} miles away"
                        }

                    // Determine category from types
                    val category =
                        when {
                            place.types.contains("cafe") -> "Cafe"
                            place.types.contains("bar") -> "Bar"
                            place.types.contains("restaurant") -> "Restaurant"
                            place.types.contains("bakery") -> "Bakery"
                            place.types.contains("meal_takeaway") -> "Takeaway"
                            else -> "Restaurant"
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
                        distanceInMeters = distanceInMeters
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

        // Always reset loading states after processing response
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
}
