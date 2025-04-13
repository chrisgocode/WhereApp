package com.example.where.model

data class UserPreferences(
    val dietaryRestrictions: List<String> = emptyList(),
    val cuisinePreferences: List<String> = emptyList(),
    val priceRange: Int = 2 // Default to moderate ($$)
)