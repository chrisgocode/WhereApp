package com.example.where.model

import com.google.android.gms.maps.model.LatLng

data class Restaurant(
    val id: String,
    val name: String,
    val location: LatLng,
    val address: String,
    val rating: Float,
    val priceLevel: Int,
    val category: String,
    val distance: String, // Formatted distance (e.g., "0.5 miles away")
    val distanceInMeters: Float // Actual distance in meters for sorting
)

// Models for Google Places API responses
data class NearbySearchResponse(
    val results: List<PlaceResult>,
    val status: String,
    val next_page_token: String? = null
)

data class PlaceResult(
    val place_id: String,
    val name: String,
    val vicinity: String,
    val geometry: Geometry,
    val rating: Float?,
    val price_level: Int?,
    val types: List<String>,
    val photo: String? = null // Assuming this is a photo_reference
)

data class Geometry(val location: GeographicLocation)

data class GeographicLocation(val lat: Double, val lng: Double)