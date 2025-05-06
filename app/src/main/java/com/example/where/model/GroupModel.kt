package com.example.where.model

import com.google.firebase.Timestamp

data class RestaurantRef(val count: Int = 0, val restaurant_id: String = "")

data class Group(
        val id: String = "",
        val name: String = "",
        val members: List<String> = emptyList(),
        val createdBy: String = "",
        val createdAt: Timestamp? = null,
        val restaurants: List<RestaurantRef> = emptyList()
)
