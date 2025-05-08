package com.example.where.ui.screens.groups

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.where.model.Group
import com.example.where.model.Poll
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val group: Group? = null,
    val polls: List<Poll> = emptyList(),
    val error: String? = null,
    val showCreatePollDialog: Boolean = false,
    val showEditMembersDialog: Boolean = false,
    val searchResults: List<UserSearchResult> = emptyList(),
    val allUsers: List<UserSearchResult> = emptyList()
)

class GroupDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        loadAllUsers()
    }

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                firestore.collection("groups").document(groupId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    error = "Failed to load group: ${error.message}"
                                )
                            }
                            return@addSnapshotListener
                        }
                        val group = snapshot?.toObject(Group::class.java)
                        val polls = group?.polls?.sortedByDescending { it.createdAt.seconds } ?: emptyList()
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                group = group,
                                polls = polls,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Failed to load group: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            try {
                val currentUserEmail = auth.currentUser?.email ?: return@launch
                val usersSnapshot = firestore.collection("users").get().await()
                val users = usersSnapshot.documents
                    .mapNotNull { doc ->
                        val email = doc.getString("email") ?: return@mapNotNull null
                        val displayName = doc.getString("displayName")
                        if (email != currentUserEmail) {
                            UserSearchResult(email, displayName ?: email)
                        } else null
                    }
                _uiState.update { state ->
                    state.copy(allUsers = users)
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(error = "Failed to load users: ${e.message}")
                }
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        val lowercaseQuery = query.lowercase()
        val results = _uiState.value.allUsers.filter { user ->
            user.email.lowercase().contains(lowercaseQuery) ||
                    user.name.lowercase().contains(lowercaseQuery)
        }
        _uiState.update { it.copy(searchResults = results) }
    }

    fun showCreatePollDialog() {
        _uiState.update { it.copy(showCreatePollDialog = true) }
    }

    fun hideCreatePollDialog() {
        _uiState.update { it.copy(showCreatePollDialog = false) }
    }

    fun showEditMembersDialog() {
        _uiState.update { it.copy(showEditMembersDialog = true) }
    }

    fun hideEditMembersDialog() {
        _uiState.update { it.copy(showEditMembersDialog = false, searchResults = emptyList()) }
    }

    fun updateMembers(newMembers: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val groupId = _uiState.value.group?.id ?: return@launch
                firestore.collection("groups").document(groupId)
                    .update("members", newMembers)
                    .await()
                hideEditMembersDialog()
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Failed to update members: ${e.message}"
                    )
                }
            }
        }
    }

    fun leaveGroup(navController: NavController) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentUser = auth.currentUser?.email ?: return@launch
                val groupId = _uiState.value.group?.id ?: return@launch
                val currentMembers = _uiState.value.group?.members ?: emptyList()
                val updatedMembers = currentMembers - currentUser

                if (updatedMembers.isEmpty()) {
                    firestore.collection("groups").document(groupId).delete().await()
                    val polls = firestore.collection("polls")
                        .whereEqualTo("groupId", groupId)
                        .get()
                        .await()
                    for (poll in polls.documents) {
                        firestore.collection("polls").document(poll.id).delete().await()
                    }
                } else {
                    firestore.collection("groups").document(groupId)
                        .update("members", updatedMembers)
                        .await()
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        group = null,
                        polls = emptyList(),
                        error = null
                    )
                }
                navController.navigate("groups") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Failed to leave group: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteGroup(navController: NavController) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val groupId = _uiState.value.group?.id ?: return@launch
                firestore.collection("groups").document(groupId).delete().await()
                val polls = firestore.collection("polls")
                    .whereEqualTo("groupId", groupId)
                    .get()
                    .await()
                for (poll in polls.documents) {
                    firestore.collection("polls").document(poll.id).delete().await()
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        group = null,
                        polls = emptyList(),
                        error = null
                    )
                }
                navController.navigate("groups") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Failed to delete group: ${e.message}"
                    )
                }
            }
        }
    }

    fun vote(pollId: String, restaurantId: String) {
        viewModelScope.launch {
            try {
                Log.d("GroupDetailViewModel", "Attempting to vote/unvote for pollId: $pollId, restaurantId: $restaurantId")
                val currentUser = auth.currentUser?.email ?: return@launch
                val groupId = _uiState.value.group?.id ?: return@launch
                val pollRef = firestore.collection("polls").document(pollId)
                val groupRef = firestore.collection("groups").document(groupId)

                firestore.runTransaction { transaction ->
                    // Get group to access polls array
                    val groupDoc = transaction.get(groupRef)
                    val group = groupDoc.toObject(Group::class.java)
                        ?: throw IllegalStateException("Group not found")

                    // Find poll in group's polls array
                    val groupPoll = group.polls.find { it.id == pollId }
                        ?: throw IllegalStateException("Poll not found in group")

                    // Get poll from polls collection (optional, for consistency check)
                    val pollDoc = transaction.get(pollRef)
                    val poll = if (pollDoc.exists()) {
                        pollDoc.toObject(Poll::class.java)
                            ?: throw IllegalStateException("Failed to deserialize poll")
                    } else {
                        Log.w("GroupDetailViewModel", "Poll $pollId not found in polls collection, using group poll")
                        groupPoll
                    }

                    if (poll.isEnded) throw IllegalStateException("Poll has ended")

                    // Update restaurants
                    val updatedRestaurants = poll.restaurants.map { restaurant ->
                        when {
                            restaurant.restaurantId == restaurantId -> {
                                if (restaurant.votedUsers.contains(currentUser)) {
                                    restaurant.copy(votedUsers = restaurant.votedUsers - currentUser) // Unvote
                                } else {
                                    restaurant.copy(votedUsers = restaurant.votedUsers + currentUser) // Vote
                                }
                            }
                            restaurant.votedUsers.contains(currentUser) -> {
                                restaurant.copy(votedUsers = restaurant.votedUsers - currentUser) // Remove vote from other restaurants
                            }
                            else -> restaurant
                        }
                    }

                    // Update polls collection
                    transaction.set(pollRef, poll.copy(restaurants = updatedRestaurants))

                    // Update polls array in group
                    val updatedPolls = group.polls.map { groupPoll ->
                        if (groupPoll.id == pollId) {
                            groupPoll.copy(restaurants = updatedRestaurants)
                        } else {
                            groupPoll
                        }
                    }
                    transaction.update(groupRef, "polls", updatedPolls)
                }.await()

                Log.d("GroupDetailViewModel", "Vote/unvote successful for pollId: $pollId")
            } catch (e: Exception) {
                Log.e("GroupDetailViewModel", "Vote/unvote failed: ${e.message}", e)
                _uiState.update { it.copy(error = "Failed to vote/unvote: ${e.message}") }
            }
        }
    }
}