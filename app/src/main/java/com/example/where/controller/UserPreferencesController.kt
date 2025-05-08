package com.example.where.controller

import android.util.Log
import com.example.where.model.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class UserPreferencesController
@Inject
constructor(
        private val firestore: FirebaseFirestore // Inject FirebaseFirestore
) {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FIELD_DIETARY_RESTRICTIONS = "dietaryRestrictions"
        private const val FIELD_CUISINE_PREFERENCES = "cuisinePreferences"
        private const val FIELD_PRICE_RANGE = "priceRange"
    }

    /** Fetches user preferences for a given Firebase UID from Firestore. */
    suspend fun getUserPreferences(uid: String): Result<UserPreferences> {
        Log.d("UserPreferencesController", "Fetching preferences from Firestore for UID: $uid")
        return try {
            val documentSnapshot =
                    firestore.collection(USERS_COLLECTION).document(uid).get().await()

            if (documentSnapshot.exists()) {
                val dietaryRestrictions =
                        documentSnapshot.get(FIELD_DIETARY_RESTRICTIONS) as? List<String>
                                ?: emptyList()
                val cuisinePreferences =
                        documentSnapshot.get(FIELD_CUISINE_PREFERENCES) as? List<String>
                                ?: emptyList()
                val priceRangeLong = documentSnapshot.getLong(FIELD_PRICE_RANGE)
                val priceRange = priceRangeLong?.toInt() ?: 2

                val preferences =
                        UserPreferences(
                                dietaryRestrictions = dietaryRestrictions,
                                cuisinePreferences = cuisinePreferences,
                                priceRange = priceRange
                        )
                Log.d(
                        "UserPreferencesController",
                        "Successfully fetched preferences for UID $uid: $preferences"
                )
                Result.success(preferences)
            } else {
                Log.d(
                        "UserPreferencesController",
                        "No preferences document found for UID $uid. Returning default preferences."
                )
                Result.success(UserPreferences())
            }
        } catch (e: Exception) {
            Log.e(
                    "UserPreferencesController",
                    "Error fetching preferences for UID $uid from Firestore",
                    e
            )
            Result.failure(e)
        }
    }
}
