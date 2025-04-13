package com.example.where.ui.screens.onboarding

import android.util.Log
import androidx.compose.runtime.mutableStateOf
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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

class OnboardingViewModel(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _userPreferences = MutableStateFlow(UserPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()
    val onboardingCompleted = mutableStateOf(false)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    
    // Add a flag to track if initialization is complete
    private val _initializationComplete = MutableStateFlow(false)
    val initializationComplete: StateFlow<Boolean> = _initializationComplete.asStateFlow()

    companion object {
        private val DIETARY_RESTRICTIONS_KEY = stringPreferencesKey("dietary_restrictions")
        private val CUISINE_PREFERENCES_KEY = stringPreferencesKey("cuisine_preferences")
        private val PRICE_RANGE_KEY = intPreferencesKey("price_range")
        private val ONBOARDING_COMPLETED_KEY = stringPreferencesKey("onboarding_completed")
    }

    init {
        viewModelScope.launch {
            loadSavedPreferences()
            checkOnboardingStatus()
            _initializationComplete.value = true
            Log.d("OnboardingViewModel", "Initialization complete, onboarding status: ${onboardingCompleted.value}")
        }
    }

    private suspend fun checkOnboardingStatus() {
        try {
            val preferences = dataStore.data.first()
            val completed = preferences[ONBOARDING_COMPLETED_KEY] == "true"
            onboardingCompleted.value = completed
            if (completed) {
                Log.d("OnboardingViewModel", "Onboarding marked as completed in DataStore")
            } else {
                Log.d("OnboardingViewModel", "Onboarding not completed in DataStore")
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to check onboarding status: ${e.message}"
            Log.e("OnboardingViewModel", "Error checking onboarding status", e)
        }
    }

    fun saveOnboardingCompleted() {
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[ONBOARDING_COMPLETED_KEY] = "true"
                }
                onboardingCompleted.value = true
                Log.d("OnboardingViewModel", "Onboarding completed saved to DataStore")
            } catch (e: Exception) {
                errorMessage.value = "Failed to save onboarding status: ${e.message}"
                Log.e("OnboardingViewModel", "Error saving onboarding status", e)
            }
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[ONBOARDING_COMPLETED_KEY] = "false"
                    preferences.remove(DIETARY_RESTRICTIONS_KEY)
                    preferences.remove(CUISINE_PREFERENCES_KEY)
                    preferences.remove(PRICE_RANGE_KEY)
                }
                onboardingCompleted.value = false
                _userPreferences.value = UserPreferences()
                Log.d("OnboardingViewModel", "Onboarding and preferences reset in DataStore")
            } catch (e: Exception) {
                errorMessage.value = "Failed to reset onboarding: ${e.message}"
                Log.e("OnboardingViewModel", "Error resetting onboarding", e)
            }
        }
    }

    private suspend fun loadSavedPreferences() {
        try {
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
            Log.d("OnboardingViewModel", "Loaded preferences: $dietaryRestrictions, $cuisinePreferences, $priceRange")
        } catch (e: Exception) {
            errorMessage.value = "Failed to load preferences: ${e.message}"
            Log.e("OnboardingViewModel", "Error loading preferences", e)
        }
    }

    private fun parseJsonToList(json: String): List<String> {
        return json.trim('[', ']').split(",").map { it.trim('"', ' ') }.filter { it.isNotEmpty() }
    }

    private fun listToJson(list: List<String>): String {
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
            try {
                dataStore.edit { preferences ->
                    preferences[key] = value
                }
                Log.d("OnboardingViewModel", "Saved preference: $key = $value")
            } catch (e: Exception) {
                errorMessage.value = "Failed to save preference: ${e.message}"
                Log.e("OnboardingViewModel", "Error saving preference", e)
            }
        }
    }

    private fun savePreference(key: Preferences.Key<Int>, value: Int) {
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[key] = value
                }
                Log.d("OnboardingViewModel", "Saved preference: $key = $value")
            } catch (e: Exception) {
                errorMessage.value = "Failed to save preference: ${e.message}"
                Log.e("OnboardingViewModel", "Error saving preference", e)
            }
        }
    }

    fun completeOnboarding() {
        isLoading.value = true
        try {
            // First set the onboarding completion locally
            viewModelScope.launch {
                try {
                    dataStore.edit { preferences ->
                        preferences[ONBOARDING_COMPLETED_KEY] = "true"
                    }
                    onboardingCompleted.value = true
                    Log.d("OnboardingViewModel", "Onboarding marked as completed locally")
                } catch (e: Exception) {
                    Log.e("OnboardingViewModel", "Error setting onboarding locally", e)
                    // Continue with Firestore sync attempt even if local save fails
                }
            }
            
            // Then sync to Firestore
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val db = FirebaseFirestore.getInstance()
                val userData = hashMapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "dietaryRestrictions" to _userPreferences.value.dietaryRestrictions,
                    "cuisinePreferences" to _userPreferences.value.cuisinePreferences,
                    "priceRange" to _userPreferences.value.priceRange,
                    "onboardingCompleted" to true // Added this flag to sync onboarding
                )

                db.collection("users").document(user.uid)
                    .set(userData)
                    .addOnSuccessListener {
                        isLoading.value = false
                        Log.d("OnboardingViewModel", "User preferences saved to Firestore")
                    }
                    .addOnFailureListener { e ->
                        errorMessage.value = "Failed to save preferences to cloud: ${e.message}"
                        isLoading.value = false
                        Log.e("OnboardingViewModel", "Error saving to Firestore", e)
                        // Onboarding still completes locally even if Firestore sync fails
                    }
            } else {
                errorMessage.value = "User not authenticated"
                isLoading.value = false
                Log.e("OnboardingViewModel", "No authenticated user found")
            }
        } catch (e: Exception) {
            errorMessage.value = "An error occurred: ${e.message}"
            isLoading.value = false
            Log.e("OnboardingViewModel", "Unexpected error during onboarding", e)
        }
    }
}