package com.example.where.controller

import android.util.Log
import com.example.where.model.Group
import com.example.where.model.RestaurantRef
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class GroupController @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    val userGroups: StateFlow<List<Group>> = _userGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isAddingRestaurants = MutableStateFlow(false)
    val isAddingRestaurants: StateFlow<Boolean> = _isAddingRestaurants.asStateFlow()

    private val _addRestaurantError = MutableStateFlow<String?>(null)
    val addRestaurantError: StateFlow<String?> = _addRestaurantError.asStateFlow()

    private val _addRestaurantSuccess = MutableStateFlow(false)
    val addRestaurantSuccess: StateFlow<Boolean> = _addRestaurantSuccess.asStateFlow()

    suspend fun fetchUserGroups() {
        val userEmail = auth.currentUser?.email
        if (userEmail == null) {
            _errorMessage.value = "User not authenticated"
            Log.e("GroupController", "Cannot fetch groups: User not authenticated.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        Log.d("GroupController", "Fetching groups for user: $userEmail")

        try {
            val snapshot =
                    db.collection("groups").whereEqualTo("createdBy", userEmail).get().await()

            val groups =
                    snapshot.documents.mapNotNull { document ->
                        try {
                            // Manually map fields, including the document ID
                            val group = document.toObject(Group::class.java)?.copy(id = document.id)
                            Log.d(
                                    "GroupController",
                                    "Fetched group: ${group?.name} (ID: ${group?.id})"
                            )
                            group
                        } catch (e: Exception) {
                            Log.e(
                                    "GroupController",
                                    "Error converting document ${document.id} to Group",
                                    e
                            )
                            null // Skip documents that fail to convert
                        }
                    }
            _userGroups.value = groups
            Log.d("GroupController", "Successfully fetched ${groups.size} groups.")
        } catch (e: Exception) {
            _errorMessage.value = "Failed to fetch groups: ${e.message}"
            Log.e("GroupController", "Error fetching groups from Firestore", e)
            _userGroups.value = emptyList() // Clear groups on error
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun addRestaurantsToGroup(groupId: String, restaurantIds: Set<String>) {
        if (groupId.isBlank()) {
            _addRestaurantError.value = "Invalid group ID."
            Log.e("GroupController", "Cannot add restaurants: Invalid group ID.")
            return
        }
        if (restaurantIds.isEmpty()) {
            _addRestaurantError.value = "No restaurants selected."
            Log.w("GroupController", "No restaurants selected to add.")
            return // Nothing to add
        }

        _isAddingRestaurants.value = true
        _addRestaurantError.value = null
        _addRestaurantSuccess.value = false
        Log.d(
                "GroupController",
                "Attempting to add ${restaurantIds.size} restaurants to group ID: $groupId"
        )

        val groupDocRef = db.collection("groups").document(groupId)

        try {
            // 1. Read the current group document
            val documentSnapshot = groupDocRef.get().await()
            if (!documentSnapshot.exists()) {
                throw Exception("Group document $groupId does not exist.")
            }

            // 2. Get the current list of restaurants and their IDs
            val currentRestaurants =
                    documentSnapshot.get("restaurants") as? List<Map<String, Any>> ?: emptyList()
            val existingRestaurantIds =
                    currentRestaurants.mapNotNull { it["restaurant_id"] as? String }.toSet()
            Log.d(
                    "GroupController",
                    "Existing restaurant IDs in group $groupId: $existingRestaurantIds"
            )

            // 3. Filter the incoming IDs
            val newRestaurantIds = restaurantIds.filterNot { it in existingRestaurantIds }
            Log.d("GroupController", "New restaurant IDs to add: $newRestaurantIds")

            if (newRestaurantIds.isEmpty()) {
                Log.w(
                        "GroupController",
                        "Duplicate restaurants detected. No new restaurants to add to group $groupId."
                )
                // Set error message on duplicates
                _addRestaurantError.value = "Duplicate restaurant detected. No restaurants sent"
                _addRestaurantSuccess.value = false
                _isAddingRestaurants.value = false
                return // Exit early
            }

            // 4. Create RestaurantRef objects only for the new IDs
            val restaurantsToAdd =
                    newRestaurantIds.map { RestaurantRef(restaurant_id = it, count = 0) }

            // 5. Use FieldValue.arrayUnion with the filtered list
            groupDocRef
                    .update("restaurants", FieldValue.arrayUnion(*restaurantsToAdd.toTypedArray()))
                    .await()
            Log.d(
                    "GroupController",
                    "Successfully added ${restaurantsToAdd.size} new restaurants to group $groupId"
            )
            _addRestaurantSuccess.value = true
        } catch (e: Exception) {
            _addRestaurantError.value = "Failed to add restaurants: ${e.message}"
            Log.e("GroupController", "Error updating group $groupId in Firestore", e)
            _addRestaurantSuccess.value = false
        } finally {
            _isAddingRestaurants.value = false
        }
    }

    fun clearAddRestaurantStatus() {
        _addRestaurantError.value = null
        _addRestaurantSuccess.value = false
    }
}
