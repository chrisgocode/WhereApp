// Create an OnboardingViewModel to manage the data
package com.example.where.ui.onboarding

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.where.models.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first

class OnboardingViewModel(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _userPreferences = MutableStateFlow(UserPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    val onboardingComplete = mutableStateOf(false)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    companion object {
        private val DIETARY_RESTRICTIONS_KEY = stringPreferencesKey("dietary_restrictions")
        private val CUISINE_PREFERENCES_KEY = stringPreferencesKey("cuisine_preferences")
        private val PRICE_RANGE_KEY = intPreferencesKey("price_range")
        private val ONBOARDING_COMPLETED_KEY = stringPreferencesKey("onboarding_completed")
    }

    init {
        viewModelScope.launch {
            loadSavedPreferences()
        }
    }

    private suspend fun loadSavedPreferences() {
        try {
            val preferences = dataStore.data.first()
            
            val dietaryRestrictionsJson = preferences[DIETARY_RESTRICTIONS_KEY] ?: "[]"
            val cuisinePreferencesJson = preferences[CUISINE_PREFERENCES_KEY] ?: "[]"
            val priceRange = preferences[PRICE_RANGE_KEY] ?: 2
            
            // Convert JSON strings back to lists
            val dietaryRestrictions = parseJsonToList(dietaryRestrictionsJson)
            val cuisinePreferences = parseJsonToList(cuisinePreferencesJson)
            
            _userPreferences.value = UserPreferences(
                dietaryRestrictions = dietaryRestrictions,
                cuisinePreferences = cuisinePreferences,
                priceRange = priceRange
            )
            
            // Check if onboarding was previously completed
            val completed = preferences[ONBOARDING_COMPLETED_KEY]
            onboardingComplete.value = completed == "true"
            
        } catch (e: Exception) {
            errorMessage.value = "Failed to load preferences: ${e.message}"
        }
    }

    private fun parseJsonToList(json: String): List<String> {
        // Simple JSON array parsing - in a real app you would use a proper JSON library
        return json.trim('[', ']').split(",").map { it.trim('"', ' ') }.filter { it.isNotEmpty() }
    }

    private fun listToJson(list: List<String>): String {
        // Simple JSON array creation
        return list.joinToString(", ", "[", "]") { "\"$it\"" }
    }

    fun updateDietaryRestrictions(restrictions: List<String>) {
        _userPreferences.value = _userPreferences.value.copy(dietaryRestrictions = restrictions)
        savePreference(DIETARY_RESTRICTIONS_KEY, listToJson(restrictions))
    }

    fun updateCuisinePreferences(cuisines: List<String>) {
        _userPreferences.value = _userPreferences.value.copy(cuisinePreferences = cuisines)
        savePreference(CUISINE_PREFERENCES_KEY, listToJson(cuisines))
    }

    fun updatePriceRange(priceLevel: Int) {
        _userPreferences.value = _userPreferences.value.copy(priceRange = priceLevel)
        savePreference(PRICE_RANGE_KEY, priceLevel)
    }

    private fun savePreference(key: Preferences.Key<String>, value: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[key] = value
            }
        }
    }

    private fun savePreference(key: Preferences.Key<Int>, value: Int) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[key] = value
            }
        }
    }

    fun completeOnboarding() {
        isLoading.value = true
        
        try {
            // Save to Firestore
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val db = FirebaseFirestore.getInstance()
                val userData = hashMapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "dietaryRestrictions" to _userPreferences.value.dietaryRestrictions,
                    "cuisinePreferences" to _userPreferences.value.cuisinePreferences,
                    "priceRange" to _userPreferences.value.priceRange
                )
                
                db.collection("users").document(user.uid)
                    .set(userData)
                    .addOnSuccessListener {
                        // Mark onboarding as complete
                        viewModelScope.launch {
                            dataStore.edit { preferences ->
                                preferences[ONBOARDING_COMPLETED_KEY] = "true"
                            }
                            onboardingComplete.value = true
                            isLoading.value = false
                        }
                    }
                    .addOnFailureListener { e ->
                        errorMessage.value = "Failed to save preferences: ${e.message}"
                        isLoading.value = false
                    }
            } else {
                errorMessage.value = "User not authenticated"
                isLoading.value = false
            }
        } catch (e: Exception) {
            errorMessage.value = "An error occurred: ${e.message}"
            isLoading.value = false
        }
    }
}
