package com.example.where.ui.screens.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.where.R
import com.example.where.ui.screens.shared.BottomNavBar
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog

val PrimaryPurple = Color(0xFF8A3FFC)
val BackgroundWhite = Color(0xFFFAFAFA)
val DarkGray = Color(0xFF333333)
val LightGray = Color(0xFFE0E0E0)
val LighterPurple = Color(0xFFF6F2FF)

@Composable
fun GroupsScreen(
    navController: NavController,
    onNavItemClick: (String) -> Unit,
    dataStore: DataStore<Preferences>
) {
    val viewModel: GroupsViewModel = viewModel { GroupsViewModel(dataStore) }
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val horizontalPadding = if (isTablet) 32.dp else 16.dp

    if (uiState.showCreateDialog) {
        CreateGroupDialog(
            availableUsers = uiState.searchResults,
            onDismiss = { viewModel.hideCreateGroupDialog() },
            onCreateGroup = { name, members -> viewModel.createGroup(name, members) },
            onSearch = { query -> viewModel.searchUsers(query) },
            allUsers = uiState.allUsers
        )
    }

    Scaffold(
        bottomBar = { BottomNavBar(selectedRoute = "groups", onNavItemClick = onNavItemClick) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = BackgroundWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_location),
                            contentDescription = "Location",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                        )
                        Text(
                            text = "Groups",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isTablet) 22.sp else 20.sp,
                            color = DarkGray
                        )
                    }
                    if (uiState.groups.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.showCreateGroupDialog() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Group",
                                tint = PrimaryPurple
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 24.dp))

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryPurple)
                    }
                } else if (uiState.groups.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_group),
                                contentDescription = "No Groups Icon",
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(bottom = 16.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )

                            Text(
                                text = "No groups yet",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Create your first group to start planning meals with friends and family.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .padding(bottom = 24.dp)
                                    .padding(horizontal = 32.dp)
                            )

                            Button(
                                onClick = { viewModel.showCreateGroupDialog() },
                                modifier = Modifier.padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                            ) {
                                Text("Create a group")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.groups) { group ->
                            GroupItem(
                                group = group,
                                onClick = { navController.navigate("groupDetail/${group.id}") }
                            )
                        }
                    }
                }

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GroupItem(
    group: com.example.where.model.Group,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = LighterPurple
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkGray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${group.members.size} members",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_forward),
                contentDescription = "View Group",
                tint = PrimaryPurple
            )
        }
    }
}

@Composable
fun CreateGroupDialog(
    availableUsers: List<UserSearchResult>,
    onDismiss: () -> Unit,
    onCreateGroup: (name: String, members: List<String>) -> Unit,
    onSearch: (String) -> Unit,
    allUsers: List<UserSearchResult>
) {
    var groupName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf(setOf<String>()) }

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
                        text = "Create New Group",
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

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Add Members",
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
                        if (groupName.isNotBlank() && selectedMembers.isNotEmpty()) {
                            onCreateGroup(groupName, selectedMembers.toList())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = groupName.isNotBlank() && selectedMembers.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Create Group")
                }
            }
        }
    }
}