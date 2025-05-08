package com.example.where.controller

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
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

    private lateinit var apiKey: String

    private var _userLocation = mutableStateOf<LatLng?>(null)
    val userLocation = _userLocation

    private var _userCity = mutableStateOf<String?>(null)
    val userCity = _userCity

    private var _nearbyRestaurants = mutableStateOf<List<Restaurant>>(emptyList())
    val nearbyRestaurants = _nearbyRestaurants

    private var _isLoading = mutableStateOf(false)
    val isLoading = _isLoading

    private var _isLoadingMore = mutableStateOf(false)
    val isLoadingMore = _isLoadingMore

    private var _errorMessage = mutableStateOf<String?>(null)
    val errorMessage = _errorMessage

    private var nextPageToken: String? = null

    private var _hasMoreResults = mutableStateOf(false)
    val hasMoreResults = _hasMoreResults

    private var _detailsLoading = mutableStateOf(false)
    val detailsLoading = _detailsLoading

    private var _detailsErrorMessage = mutableStateOf<String?>(null)
    val detailsErrorMessage = _detailsErrorMessage

    private var _restaurantDetails = mutableStateOf<Map<String, Any>?>(null)
    val restaurantDetails = _restaurantDetails

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun setApiKey(key: String) {
        Log.d("RestaurantController", "Setting API key: ${key.take(5)}...")
        apiKey = key
    }

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
            val defaultLatLng = LatLng(37.7749, -122.4194)
            _userLocation.value = defaultLatLng
            onSuccess(defaultLatLng)
        } catch (e: Exception) {
            Log.e("RestaurantController", "Error getting location: ${e.message}")
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
                    1
                )
                val cityName = if (list.isNullOrEmpty()) {
                    "Unknown"
                } else {
                    val addr = list[0]
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

    private suspend fun getLocationSuspend(): LatLng = suspendCancellableCoroutine { continuation ->
        try {
            if (!hasLocationPermission()) {
                continuation.resumeWithException(SecurityException("Location permission not granted"))
                return@suspendCancellableCoroutine
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                        Log.d("RestaurantController", "No location available, using default for emulator testing")
                        val defaultLatLng = LatLng(37.7749, -122.4194)
                        continuation.resume(defaultLatLng)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RestaurantController", "Location failure: ${exception.message}")
                    Log.d("RestaurantController", "Location failure, using default for emulator testing")
                    val defaultLatLng = LatLng(37.7749, -122.4194)
                    continuation.resume(defaultLatLng)
                }

            continuation.invokeOnCancellation {
                Log.d("RestaurantController", "Location request cancelled")
            }
        } catch (e: SecurityException) {
            Log.e("RestaurantController", "Security exception: ${e.message}")
            val defaultLatLng = LatLng(37.7749, -122.4194)
            continuation.resume(defaultLatLng)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    suspend fun fetchNearbyRestaurants(radius: Int = 8046) {
        _isLoading.value = true
        _errorMessage.value = null
        nextPageToken = null
        _nearbyRestaurants.value = emptyList()

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

    suspend fun fetchMoreRestaurants() {
        if (!hasMoreResults.value || _isLoadingMore.value || nextPageToken == null) {
            Log.d(
                "RestaurantController",
                "Cannot fetch more: hasMoreResults=${hasMoreResults.value}, isLoadingMore=${_isLoadingMore.value}, hasToken=${nextPageToken != null}"
            )
            return
        }

        Log.d("RestaurantController", "Starting to load more, setting isLoadingMore=true")
        _isLoadingMore.value = true

        val location = _userLocation.value
        if (location == null) {
            resetLoadingState()
            Log.d("RestaurantController", "User location is null, setting isLoadingMore=false")
            return
        }

        val tokenToUse = nextPageToken
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
                    rankby = "distance"
                )

                processResponse(response, location)
            }
        } catch (e: Exception) {
            Log.e("RestaurantController", "Error fetching more restaurants: ${e.message}", e)
            Log.d("RestaurantController", "Error caught, setting isLoadingMore=false")
            _isLoadingMore.value = false
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

                val response = RestaurantApiClient.placesApiService.getNearbyRestaurants(
                    location = locationString,
                    apiKey = apiKey,
                    rankby = "distance"
                )

                processResponse(response, location)
            }
        } catch (e: Exception) {
            Log.e("RestaurantController", "Exception fetching restaurants: ${e.message}", e)
            _errorMessage.value = "Error fetching restaurants: ${e.message}"
            _isLoading.value = false
        }
    }

    private fun processResponse(response: Map<String, Any>, location: LatLng) {
        val status = response["status"] as? String
        if (status == "OK") {
            Log.d(
                "RestaurantController",
                "API response successful: ${(response["results"] as? List<*>?)?.size ?: 0} restaurants found"
            )

            nextPageToken = response["next_page_token"] as? String
            _hasMoreResults.value = nextPageToken != null

            val results = response["results"] as? List<Map<String, Any>> ?: emptyList()
            if (results.isEmpty()) {
                Log.d(
                    "RestaurantController",
                    "No new restaurants in response, setting hasMoreResults=false"
                )
                nextPageToken = null
                _hasMoreResults.value = false
                resetLoadingState()
                return
            }

            val newRestaurants = results.mapNotNull { place ->
                val geometry = place["geometry"] as? Map<String, Any> ?: return@mapNotNull null
                val locationData = geometry["location"] as? Map<String, Any> ?: return@mapNotNull null
                val lat = locationData["lat"] as? Double ?: return@mapNotNull null
                val lng = locationData["lng"] as? Double ?: return@mapNotNull null
                val placeLocation = LatLng(lat, lng)

                val distanceInMeters = calculateDistance(
                    location.latitude,
                    location.longitude,
                    placeLocation.latitude,
                    placeLocation.longitude
                )

                val distanceInMiles = (distanceInMeters / 1609.34)
                val formattedDistance = if (distanceInMiles < 0.1) {
                    "< 0.1 miles away"
                } else {
                    "${(distanceInMiles * 10).roundToInt() / 10.0} miles away"
                }

                val types = place["types"] as? List<String> ?: emptyList()
                val category = when {
                    types.contains("cafe") -> "Cafe"
                    types.contains("bar") -> "Bar"
                    types.contains("restaurant") -> "Restaurant"
                    types.contains("bakery") -> "Bakery"
                    types.contains("meal_takeaway") -> "Takeaway"
                    else -> "Restaurant"
                }

                Restaurant(
                    id = place["place_id"] as? String ?: return@mapNotNull null,
                    name = place["name"] as? String ?: return@mapNotNull null,
                    location = placeLocation,
                    address = place["vicinity"] as? String ?: "",
                    rating = (place["rating"] as? Number)?.toFloat() ?: 0f,
                    priceLevel = (place["price_level"] as? Number)?.toInt() ?: 0,
                    category = category,
                    distance = formattedDistance,
                    distanceInMeters = distanceInMeters
                )
            }

            val allRestaurants = _nearbyRestaurants.value + newRestaurants
            val sortedRestaurants = allRestaurants.distinctBy { it.id }.sortedBy { it.distanceInMeters }
            Log.d("RestaurantController", "Processed ${sortedRestaurants.size} total restaurants")

            _nearbyRestaurants.value = sortedRestaurants
        } else {
            Log.e("RestaurantController", "API Error: $status")
            _errorMessage.value = "API Error: $status"
            _hasMoreResults.value = false
            nextPageToken = null
        }

        resetLoadingState()
        Log.d(
            "RestaurantController",
            "End of processResponse: isLoading=false, isLoadingMore=false, hasMoreResults=${_hasMoreResults.value}"
        )
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun resetLoadingState() {
        Log.d("RestaurantController", "Explicitly resetting loading states")
        _isLoading.value = false
        _isLoadingMore.value = false
    }

    suspend fun fetchRestaurantDetails(placeId: String): Map<String, Any>? {
        _detailsLoading.value = true
        _detailsErrorMessage.value = null
        _restaurantDetails.value = null
        Log.d("RestaurantController", "Fetching details for placeId: $placeId")

        if (!::apiKey.isInitialized || apiKey.isEmpty()) {
            Log.e("RestaurantController", "API key not initialized or empty for details fetch")
            _detailsErrorMessage.value = "Maps API key not configured"
            _detailsLoading.value = false
            return null
        }

        val fieldsToFetch =
            "name,formatted_address,international_phone_number,website,opening_hours,rating,user_ratings_total,price_level,photo,geometry,place_id,vicinity,types"

        return try {
            val response = withContext(Dispatchers.IO) {
                RestaurantApiClient.placesApiService.getRestaurantDetails(
                    placeId = placeId, fields = fieldsToFetch, apiKey = apiKey
                )
            }

            val status = response["status"] as? String
            if (status == "OK") {
                val result = response["result"] as? Map<String, Any>
                _restaurantDetails.value = result
                Log.d(
                    "RestaurantController", "Successfully fetched details: ${result?.get("name")}"
                )
                result
            } else {
                Log.e(
                    "RestaurantController",
                    "Error fetching details: $status - ${response["error_message"]}"
                )
                _detailsErrorMessage.value =
                    response["error_message"] as? String ?: "Error fetching details: $status"
                null
            }
        } catch (e: Exception) {
            Log.e("RestaurantController", "Exception fetching details: ${e.message}", e)
            _detailsErrorMessage.value = "Exception: ${e.message}"
            null
        } finally {
            _detailsLoading.value = false
        }
    }
}