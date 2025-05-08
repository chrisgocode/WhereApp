package com.example.where.model

import com.google.firebase.Timestamp

data class Poll(
        val id: String = "",
        val groupId: String = "",
        val createdBy: String = "",
        val createdAt: Timestamp = Timestamp.now(),
        val isEnded: Boolean = false,
        val restaurants: List<RestaurantOption> = emptyList()
)

data class RestaurantOption(
        val restaurantId: String = "",
        val votedUsers: List<String> = emptyList()
)
