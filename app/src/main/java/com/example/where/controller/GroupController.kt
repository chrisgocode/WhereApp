package com.example.where.controller

import android.util.Log
import com.example.where.model.Group
import com.example.where.model.Poll
import com.example.where.model.RestaurantOption
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

@Singleton
class GroupController @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    val userGroups: StateFlow<List<Group>> = _userGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isOperatingWithPoll = MutableStateFlow(false)
    val isOperatingWithPoll: StateFlow<Boolean> =
            _isOperatingWithPoll.asStateFlow()

    private val _operationError =
            MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()

    private val _operationSuccess = MutableStateFlow(false)
    val operationSuccess: StateFlow<Boolean> = _operationSuccess.asStateFlow()

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
                            null
                        }
                    }
            _userGroups.value = groups
            Log.d(
                    "GroupController",
                    "Successfully fetched ${groups.size} groups for user $userEmail."
            )
        } catch (e: Exception) {
            _errorMessage.value = "Failed to fetch groups: ${e.message}"
            Log.e("GroupController", "Error fetching groups from Firestore for user $userEmail", e)
            _userGroups.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createPollInGroupWithRestaurants(
            groupId: String,
            restaurantIds: Set<String>,
            currentUserEmail: String
    ): String? {
        if (groupId.isBlank() || restaurantIds.isEmpty() || currentUserEmail.isBlank()) {
            _operationError.value = "Invalid parameters for creating poll."
            Log.e(
                    "GroupController",
                    "Invalid params: g:$groupId, rIDs_empty:${restaurantIds.isEmpty()}, u:$currentUserEmail"
            )
            return null
        }

        _isOperatingWithPoll.value = true
        _operationError.value = null
        _operationSuccess.value = false

        Log.d(
                "GroupController",
                "Attempting to create new poll in group $groupId with ${restaurantIds.size} restaurants by $currentUserEmail"
        )
        val groupDocRef = db.collection("groups").document(groupId)
        val newPollId =
                db.collection("groups").document().id // Generate a unique ID for the new poll

        return try {
            val restaurantOptions =
                    restaurantIds.map {
                        RestaurantOption(restaurantId = it, votedUsers = emptyList())
                    }

            val newPoll =
                    Poll(
                            id = newPollId,
                            groupId = groupId,
                            createdBy = currentUserEmail,
                            createdAt = Timestamp.now(),
                            isEnded = false,
                            restaurants = restaurantOptions
                    )

            groupDocRef.update("polls", FieldValue.arrayUnion(newPoll)).await()

            Log.d(
                    "GroupController",
                    "Successfully created new poll $newPollId in group $groupId with ${restaurantOptions.size} restaurants."
            )
            _operationSuccess.value = true
            newPollId
        } catch (e: Exception) {
            _operationError.value = "Failed to create poll in group $groupId: ${e.message}"
            Log.e("GroupController", "Error creating poll in group $groupId", e)
            null
        } finally {
            _isOperatingWithPoll.value = false
        }
    }

    fun clearOperationStatus() {
        _operationError.value = null
        _operationSuccess.value = false
    }
}
