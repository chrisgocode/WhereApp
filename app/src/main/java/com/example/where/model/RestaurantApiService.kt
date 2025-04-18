package com.example.where.model

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApiService {
    @GET("nearbysearch/json")
    suspend fun getNearbyRestaurants(
        @Query("location") location: String,
        @Query("radius") radius: Int? = null,
        @Query("type") type: String = "restaurant",
        @Query("key") apiKey: String,
        @Query("pagetoken") pageToken: String? = null,
        @Query("rankby") rankby: String? = null
    ): NearbySearchResponse
}

object RestaurantApiClient {
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/place/"

    private val retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val placesApiService: PlacesApiService = retrofit.create(PlacesApiService::class.java)
}
