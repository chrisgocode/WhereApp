package com.example.where.model

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

data class Restaurant(
        val id: String,
        val name: String,
        val location: LatLng,
        val address: String,
        val rating: Float,
        val priceLevel: Int,
        val category: String,
        val distance: String, // Formatted distance (e.g., "0.5 miles away")
        val distanceInMeters: Float, // Actual distance in meters for sorting
        val photoMetadataList: List<PhotoMetadata>?
)

// Simplified metadata for our domain model
data class PhotoMetadata(val reference: String, val attributions: List<String>?)

// Models for Google Places API responses
data class NearbySearchResponse(
        val results: List<PlaceResult>,
        val status: String,
        val next_page_token: String? = null
)

data class PhotoMetadataInternal(
        val photo_reference: String,
        val height: Int,
        val width: Int,
        val html_attributions: List<String>
)

data class PlaceResult(
        val place_id: String,
        val name: String,
        val vicinity: String,
        val geometry: Geometry,
        val rating: Float?,
        val price_level: Int?,
        val types: List<String>,
        val photos: List<PhotoMetadataInternal>?
)

data class Geometry(val location: GeographicLocation)

data class GeographicLocation(val lat: Double, val lng: Double)

data class PlaceDetailsResponse(
        val result: PlaceDetailResult?,
        val status: String,
        @SerializedName("error_message") val errorMessage: String? = null,
        @SerializedName("html_attributions") val htmlAttributions: List<String>? = null
)

data class PlaceDetailResult(
        val name: String?,
        @SerializedName("formatted_address") val formattedAddress: String?,
        @SerializedName("international_phone_number") val internationalPhoneNumber: String?,
        val website: String?,
        @SerializedName("opening_hours") val openingHours: OpeningHours?,
        val rating: Float?,
        @SerializedName("user_ratings_total") val userRatingsTotal: Int?,
        @SerializedName("price_level") val priceLevel: Int?,
        val photos: List<PhotoDetail>?,
        val geometry: GeometryDetail?,
        @SerializedName("place_id") val placeId: String?,
        val vicinity: String?,
        val types: List<String>? // List of types for the place
)

data class OpeningHours(
        @SerializedName("open_now") val openNow: Boolean?,
        @SerializedName("weekday_text") val weekdayText: List<String>?,
        val periods: List<PeriodDetail>? // For more structured opening hours
)

data class PeriodDetail(val open: TimePoint?, val close: TimePoint?)

data class TimePoint(
        val day: Int?, // 0 for Sunday, 1 for Monday, ..., 6 for Saturday
        val time: String? // "HHMM" format
)


data class PhotoDetail(
        @SerializedName("photo_reference") val photoReference: String?,
        val height: Int?,
        val width: Int?,
        @SerializedName("html_attributions") val htmlAttributions: List<String>?
)

data class GeometryDetail(val location: LocationDetailResult)

data class LocationDetailResult(val lat: Double, val lng: Double)
