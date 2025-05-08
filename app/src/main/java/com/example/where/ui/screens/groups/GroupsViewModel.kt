package com.example.where.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.where.model.Group
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class UserSearchResult(
    val email: String,
    val name: String
)

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val searchResults: List<UserSearchResult> = emptyList(),
    val allUsers: List<UserSearchResult> = emptyList()
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    init {
        loadGroups()
        loadAllUsers()
    }

    fun showCreateGroupDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun hideCreateGroupDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = false,
            searchResults = emptyList()
        )
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            try {
                val currentUserEmail = auth.currentUser?.email ?: return@launch
                val usersSnapshot = db.collection("users").get().await()
                val users = usersSnapshot.documents
                    .mapNotNull { doc ->
                        val email = doc.getString("email") ?: return@mapNotNull null
                        val displayName = doc.getString("displayName")
                        if (email != currentUserEmail) {
                            UserSearchResult(email, displayName ?: email)
                        } else null
                    }
                _uiState.value = _uiState.value.copy(allUsers = users)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load users: ${e.message}"
                )
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        val lowercaseQuery = query.lowercase()
        val results = _uiState.value.allUsers.filter { user ->
            user.email.lowercase().contains(lowercaseQuery) ||
                    user.name.lowercase().contains(lowercaseQuery)
        }
        _uiState.value = _uiState.value.copy(searchResults = results)
    }

    fun createGroup(name: String, selectedMembers: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }

                val group = Group(
                    name = name,
                    members = selectedMembers + currentUser.email!!,
                    createdBy = currentUser.email!!,
                    createdAt = Timestamp.now(),
                    polls = emptyList()
                )

                // Add group to Firestore
                val docRef = db.collection("groups").add(group).await()

                // Update the group with its ID
                val groupId = docRef.id
                db.collection("groups").document(groupId)
                    .update("id", groupId)
                    .await()

                // Optimistically update UI state with the new group
                val newGroup = group.copy(id = groupId)
                _uiState.value = _uiState.value.copy(
                    groups = _uiState.value.groups + newGroup,
                    isLoading = false,
                    error = null,
                    showCreateDialog = false,
                    searchResults = emptyList()
                )

                // Reload groups to ensure consistency
                loadGroups()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to create group: ${e.message}"
                )
            }
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    println("No authenticated user found")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated. Please sign in."
                    )
                    return@launch
                }
                val email = currentUser.email ?: throw IllegalStateException("User email is null")
                println("Authenticated user email: $email")

                val groupsSnapshot = db.collection("groups")
                    .whereArrayContains("members", email)
                    .get()
                    .await()

                val groups = groupsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Group::class.java)
                }
                println("Loaded groups: ${groups.size}, IDs: ${groups.map { it.id }}")

                _uiState.value = _uiState.value.copy(
                    groups = groups,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                println("Error loading groups: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load groups: ${e.message}"
                )
            }
        }
    }
}