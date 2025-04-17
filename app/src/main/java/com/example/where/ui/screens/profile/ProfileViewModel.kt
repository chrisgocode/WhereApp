package com.example.where.ui.screens.profile

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.where.model.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.example.where.controller.RestaurantController

class ProfileViewModel(
    private val dataStore: DataStore<Preferences>,
    context: Context,
    private val restaurantController: RestaurantController = RestaurantController(context)
) : ViewModel() {

    // User information
    val userName = mutableStateOf("")
    val userLocation = mutableStateOf("")
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    // User preferences
    private val _userPreferences = MutableStateFlow<UserPreferences?>(null)
    val userPreferences: StateFlow<UserPreferences?> = _userPreferences.asStateFlow()

    companion object {
        private val DIETARY_RESTRICTIONS_KEY = stringPreferencesKey("dietary_restrictions")
        private val CUISINE_PREFERENCES_KEY = stringPreferencesKey("cuisine_preferences")
        private val PRICE_RANGE_KEY = intPreferencesKey("price_range")
    }

    init {
        fetchCity()
        loadUserData()
    }

    private fun fetchCity() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            restaurantController.getUserLocation(
                onSuccess = { latLng ->
                    userLocation.value = restaurantController.userCity.value
                        ?: "Unknown Location"
                    isLoading.value = false
                },
                onError = { error ->
                    userLocation.value = error
                    isLoading.value = false
                }
            )
        }
    }


    private fun loadUserData() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                // Try loading from DataStore first
                val preferences = dataStore.data.first()
                val dietaryRestrictionsJson = preferences[DIETARY_RESTRICTIONS_KEY] ?: "[]"
                val cuisinePreferencesJson = preferences[CUISINE_PREFERENCES_KEY] ?: "[]"
                val priceRange = preferences[PRICE_RANGE_KEY] ?: 2

                val dietaryRestrictions = parseJsonToList(dietaryRestrictionsJson)
                val cuisinePreferences = parseJsonToList(cuisinePreferencesJson)

                _userPreferences.value = UserPreferences(
                    dietaryRestrictions = dietaryRestrictions,
                    cuisinePreferences = cuisinePreferences,
                    priceRange = priceRange
                )
                Log.d("ProfileViewModel", "Loaded preferences from DataStore: $dietaryRestrictions, $cuisinePreferences, $priceRange")

                // Sync with Firestore
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    Log.d("ProfileViewModel", "Fetching data for user ${currentUser.uid}")
                    val db = FirebaseFirestore.getInstance()
                    val documentSnapshot = db.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    if (documentSnapshot.exists()) {
                        userName.value = documentSnapshot.getString("displayName") ?: "Unknown User"
//                        userLocation.value = restaurantController.userCity.value ?: "Unknown Location"
                        val firestoreDietaryRestrictions = documentSnapshot.get("dietaryRestrictions") as? List<String> ?: emptyList()
                        val firestoreCuisinePreferences = documentSnapshot.get("cuisinePreferences") as? List<String> ?: emptyList()
                        val firestorePriceRange = documentSnapshot.getLong("priceRange")?.toInt() ?: 2

                        _userPreferences.value = UserPreferences(
                            dietaryRestrictions = firestoreDietaryRestrictions,
                            cuisinePreferences = firestoreCuisinePreferences,
                            priceRange = firestorePriceRange
                        )

                        // Update DataStore with Firestore data
                        dataStore.edit { prefs ->
                            prefs[DIETARY_RESTRICTIONS_KEY] = listToJson(firestoreDietaryRestrictions)
                            prefs[CUISINE_PREFERENCES_KEY] = listToJson(firestoreCuisinePreferences)
                            prefs[PRICE_RANGE_KEY] = firestorePriceRange
                        }
                        Log.d("ProfileViewModel", "Synced Firestore data to DataStore: $userName, preferences=$_userPreferences")
                    } else {
                        Log.d("ProfileViewModel", "No user document found in Firestore")
                        errorMessage.value = "User profile not found"
                        _userPreferences.value = UserPreferences()
                    }
                } else {
                    Log.d("ProfileViewModel", "No authenticated user found")
                    errorMessage.value = "Please sign in to view your profile"
                    userName.value = "Guest"
                    userLocation.value = "N/A"
                    _userPreferences.value = UserPreferences()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading user data", e)
                errorMessage.value = "Failed to load profile: ${e.message}"
                // Keep DataStore data if Firestore fails
                if (_userPreferences.value == null) {
                    _userPreferences.value = UserPreferences()
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updateUserPreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            try {
                _userPreferences.value = preferences
                // Save to DataStore
                dataStore.edit { prefs ->
                    prefs[DIETARY_RESTRICTIONS_KEY] = listToJson(preferences.dietaryRestrictions)
                    prefs[CUISINE_PREFERENCES_KEY] = listToJson(preferences.cuisinePreferences)
                    prefs[PRICE_RANGE_KEY] = preferences.priceRange
                }
                Log.d("ProfileViewModel", "Saved preferences to DataStore: $preferences")

                // Save to Firestore
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val db = FirebaseFirestore.getInstance()
                    val userData = hashMapOf(
                        "dietaryRestrictions" to preferences.dietaryRestrictions,
                        "cuisinePreferences" to preferences.cuisinePreferences,
                        "priceRange" to preferences.priceRange
                    )
                    db.collection("users").document(currentUser.uid)
                        .update(userData as Map<String, Any>)
                        .await()
                    Log.d("ProfileViewModel", "User preferences updated in Firestore")
                } else {
                    errorMessage.value = "User not authenticated"
                    Log.e("ProfileViewModel", "Cannot update preferences: No authenticated user")
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to update preferences: ${e.message}"
                Log.e("ProfileViewModel", "Error updating preferences", e)
            }
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences.clear()
                }
                Log.d("ProfileViewModel", "Cleared DataStore on sign out")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to clear DataStore", e)
            }
            userName.value = "Guest"
            userLocation.value = "N/A"
            _userPreferences.value = UserPreferences()
            errorMessage.value = "Please sign in to view your profile"
            Log.d("ProfileViewModel", "User signed out")
        }
    }

    fun refreshUserData() {
        loadUserData()
    }

    private fun parseJsonToList(json: String): List<String> {
        return json.trim('[', ']').split(",").map { it.trim('"', ' ') }.filter { it.isNotEmpty() }
    }

    private fun listToJson(list: List<String>): String {
        return list.joinToString(", ", "[", "]") { "\"$it\"" }
    }
}