package com.example.where.ui.screens.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.where.controller.RestaurantController
import com.example.where.model.Group
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    navController: NavController,
    groupId: String,
    restaurantController: RestaurantController,
    viewModel: GroupDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser?.email
    var showLeaveGroupDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    if (uiState.showEditMembersDialog) {
        EditMembersDialog(
            group = uiState.group,
            availableUsers = uiState.searchResults,
            onDismiss = { viewModel.hideEditMembersDialog() },
            onUpdateMembers = { newMembers -> viewModel.updateMembers(newMembers) },
            onSearch = { query -> viewModel.searchUsers(query) },
            allUsers = uiState.allUsers
        )
    }

    if (showLeaveGroupDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveGroupDialog = false },
            title = {
                Text(
                    text = "Leave Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to leave this group?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveGroup(navController)
                        showLeaveGroupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveGroupDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = BackgroundWhite
        )
    }

    if (showDeleteGroupDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = {
                Text(
                    text = "Delete Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this group? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(navController)
                        showDeleteGroupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = BackgroundWhite
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.group?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showEditMembersDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Members"
                        )
                    }
                    IconButton(onClick = { showLeaveGroupDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Leave Group"
                        )
                    }
                    if (uiState.group?.createdBy == currentUser) {
                        IconButton(onClick = { showDeleteGroupDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Group"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryPurple
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Members section
                    uiState.group?.let { group ->
                        Text(
                            text = "Members",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(group.members) { memberEmail ->
                                val user = uiState.allUsers.find { it.email == memberEmail }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = LighterPurple
                                ) {
                                    Text(
                                        text = user?.name ?: memberEmail,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Polls section
                    if (uiState.polls.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No polls created yet",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create a poll to start voting on restaurants",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.showCreatePollDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                            ) {
                                Text("Create a poll")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.polls) { poll ->
                                PollItem(
                                    poll = poll,
                                    restaurantController = restaurantController,
                                    onVote = { restaurantId -> viewModel.vote(poll.id, restaurantId) }
                                )
                            }
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun EditMembersDialog(
    group: Group?,
    availableUsers: List<UserSearchResult>,
    onDismiss: () -> Unit,
    onUpdateMembers: (List<String>) -> Unit,
    onSearch: (String) -> Unit,
    allUsers: List<UserSearchResult>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf(group?.members?.toSet() ?: emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BackgroundWhite,
            modifier = Modifier
                .widthIn(min = 280.dp, max = 560.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Members",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Add or Remove Members",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onSearch(it)
                    },
                    label = { Text("Search by name or email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedMembers.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedMembers.toList()) { email ->
                            val user = availableUsers.find { it.email == email }
                                ?: allUsers.find { it.email == email }
                            if (user != null) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = LighterPurple,
                                    modifier = Modifier.widthIn(max = 200.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = PrimaryPurple,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { selectedMembers = selectedMembers - email },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove member",
                                                tint = PrimaryPurple,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (searchQuery.isNotBlank() && availableUsers.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LightGray)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableUsers) { user ->
                                if (!selectedMembers.contains(user.email)) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (selectedMembers.size < 10) {
                                                    selectedMembers = selectedMembers + user.email
                                                    searchQuery = ""
                                                    onSearch("")
                                                }
                                            },
                                        color = BackgroundWhite
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .fillMaxWidth()
                                        ) {
                                            Text(
                                                text = user.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = DarkGray
                                            )
                                            Text(
                                                text = user.email,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedMembers.isNotEmpty()) {
                            onUpdateMembers(selectedMembers.toList())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedMembers.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Update Members")
                }
            }
        }
    }
}