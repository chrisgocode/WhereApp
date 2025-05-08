package com.example.where.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.where.controller.RestaurantController
import com.example.where.model.Poll
import com.example.where.model.RestaurantOption
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PollItem(
    poll: Poll,
    restaurantController: RestaurantController,
    onVote: (String) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser?.email

    // Format the createdAt timestamp to "MM/dd/yyyy Poll"
    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    val formattedDate = dateFormat.format(poll.createdAt.toDate()) + " Poll"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (poll.isEnded) {
                    Text(
                        text = "Poll Ended",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            poll.restaurants.forEach { restaurant ->
                val detailsState = remember(restaurant.restaurantId) {
                    mutableStateOf<Map<String, Any>?>(null)
                }
                val isLoading = remember(restaurant.restaurantId) { mutableStateOf(true) }
                val error = remember(restaurant.restaurantId) { mutableStateOf<String?>(null) }

                LaunchedEffect(restaurant.restaurantId) {
                    if (detailsState.value == null && error.value == null) {
                        try {
                            val result = restaurantController.fetchRestaurantDetails(restaurant.restaurantId)
                            detailsState.value = result
                            error.value = if (result == null) "Failed to load restaurant details" else null
                        } catch (e: Exception) {
                            error.value = "Error: ${e.message}"
                        } finally {
                            isLoading.value = false
                        }
                    }
                }

                if (isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (error.value != null) {
                    Text(
                        text = error.value ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    detailsState.value?.let { restaurantDetails ->
                        RestaurantOptionItem(
                            restaurant = restaurant,
                            restaurantDetails = restaurantDetails,
                            isVoted = currentUser in restaurant.votedUsers,
                            onVote = {
                                onVote(restaurant.restaurantId)
                            },
                            isPollEnded = poll.isEnded
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantOptionItem(
    restaurant: RestaurantOption,
    restaurantDetails: Map<String, Any>,
    isVoted: Boolean,
    onVote: () -> Unit,
    isPollEnded: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isVoted) LighterPurple else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https://via.placeholder.com/60",
                contentDescription = "Placeholder image",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = restaurantDetails["name"] as? String ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = restaurantDetails["vicinity"] as? String ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(restaurantDetails["rating"] as? Number)?.toDouble() ?: 0.0}★",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = " • ${"\$".repeat((restaurantDetails["price_level"] as? Number)?.toInt() ?: 0)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilledTonalButton(
                    onClick = onVote,
                    enabled = !isPollEnded,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isVoted) PrimaryPurple else LightGray,
                        contentColor = if (isVoted) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(text = if (isVoted) "Voted" else "Vote")
                }
                Text(
                    text = "${restaurant.votedUsers.size} votes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}