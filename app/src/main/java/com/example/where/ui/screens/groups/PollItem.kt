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
import coil3.compose.AsyncImage
import com.example.where.BuildConfig
import com.example.where.controller.RestaurantController
import com.example.where.model.PlaceDetailResult
import com.example.where.model.Poll
import com.example.where.model.RestaurantOption
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PollItem(poll: Poll, restaurantController: RestaurantController, onVote: (String) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser?.email
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val formattedDate = dateFormat.format(poll.createdAt.toDate()) + " Poll"

        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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

                        poll.restaurants.forEach { restaurantOption ->
                                var itemDetails by
                                        remember(restaurantOption.restaurantId) {
                                                mutableStateOf<Map<String, Any?>?>(null)
                                        }
                                var isLoadingItem by
                                        remember(restaurantOption.restaurantId) {
                                                mutableStateOf(true)
                                        }
                                var errorItem by
                                        remember(restaurantOption.restaurantId) {
                                                mutableStateOf<String?>(null)
                                        }

                                LaunchedEffect(restaurantOption.restaurantId) {
                                        if (isLoadingItem) {
                                                try {
                                                        val result: PlaceDetailResult? =
                                                                restaurantController
                                                                        .getPlaceDetailResult(
                                                                                restaurantOption
                                                                                        .restaurantId
                                                                        )

                                                        if (result != null) {
                                                                itemDetails =
                                                                        mapOf<String, Any?>(
                                                                                "name" to
                                                                                        (result.name
                                                                                                ?: "Unknown Restaurant"),
                                                                                "vicinity" to
                                                                                        (result.vicinity
                                                                                                ?: result.formattedAddress
                                                                                                        ?: "Address unavailable"),
                                                                                "rating" to
                                                                                        (result.rating
                                                                                                ?: 0.0),
                                                                                "price_level" to
                                                                                        (result.priceLevel
                                                                                                ?: 0),
                                                                                "photo_reference" to
                                                                                        (result.photos
                                                                                                ?.firstOrNull()
                                                                                                ?.photoReference),
                                                                                "place_id" to
                                                                                        result.placeId
                                                                        )
                                                                errorItem = null
                                                        } else {
                                                                errorItem =
                                                                        "Failed to load details for ${restaurantOption.restaurantId}"
                                                                itemDetails = null
                                                        }
                                                } catch (e: Exception) {
                                                        errorItem =
                                                                "Error fetching: ${e.message?.take(100)}"
                                                        itemDetails = null
                                                } finally {
                                                        isLoadingItem = false
                                                }
                                        }
                                }

                                if (isLoadingItem) {
                                        CircularProgressIndicator(
                                                modifier =
                                                        Modifier.align(Alignment.CenterHorizontally)
                                                                .padding(vertical = 20.dp)
                                        )
                                } else if (errorItem != null) {
                                        Text(
                                                text = errorItem ?: "Unknown error occurred",
                                                color = MaterialTheme.colorScheme.error,
                                                modifier =
                                                        Modifier.align(Alignment.CenterHorizontally)
                                                                .padding(vertical = 8.dp)
                                        )
                                } else {
                                        itemDetails?.let { detailsMap ->
                                                RestaurantOptionItem(
                                                        restaurantOption = restaurantOption,
                                                        restaurantDetailsMap = detailsMap,
                                                        isVoted =
                                                                currentUser in
                                                                        restaurantOption.votedUsers,
                                                        onVote = {
                                                                onVote(
                                                                        restaurantOption
                                                                                .restaurantId
                                                                )
                                                        },
                                                        isPollEnded = poll.isEnded
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                        }
                                                ?: Text(
                                                        "Details not available for this option.",
                                                        modifier =
                                                                Modifier.align(
                                                                                Alignment
                                                                                        .CenterHorizontally
                                                                        )
                                                                        .padding(vertical = 8.dp)
                                                )
                                }
                        }
                }
        }
}

@Composable
fun RestaurantOptionItem(
        restaurantOption: RestaurantOption,
        restaurantDetailsMap: Map<String, Any?>,
        isVoted: Boolean,
        onVote: () -> Unit,
        isPollEnded: Boolean
) {
        val name = restaurantDetailsMap["name"] as? String ?: "Unknown Restaurant"
        val vicinity = restaurantDetailsMap["vicinity"] as? String ?: "Address unavailable"
        val ratingValue = (restaurantDetailsMap["rating"] as? Number)?.toDouble() ?: 0.0
        val formattedRating = String.format(Locale.US, "%.1f", ratingValue)
        val priceLevel = (restaurantDetailsMap["price_level"] as? Int) ?: 0
        val priceLevelDisplay = "\$".repeat(priceLevel).ifEmpty { "N/A" }
        val photoReference = restaurantDetailsMap["photo_reference"] as? String
        val apiKey = BuildConfig.MAPS_API_KEY

        val imageUrl =
                if (photoReference != null && apiKey.isNotEmpty()) {
                        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=120&photoreference=$photoReference&key=$apiKey"
                } else {
                        "https://via.placeholder.com/60" // Fallback placeholder
                }

        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        if (isVoted) LighterPurple
                                        else MaterialTheme.colorScheme.surface
                        )
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        AsyncImage(
                                model = imageUrl,
                                contentDescription = "$name image",
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                        text = vicinity,
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.7f
                                                )
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                                text = "$formattedRating★",
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                                text = " • $priceLevelDisplay",
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FilledTonalButton(
                                        onClick = onVote,
                                        enabled = !isPollEnded,
                                        colors =
                                                ButtonDefaults.filledTonalButtonColors(
                                                        containerColor =
                                                                if (isVoted) PrimaryPurple
                                                                else LightGray,
                                                        contentColor =
                                                                if (isVoted)
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimary
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                )
                                ) { Text(text = if (isVoted) "Voted" else "Vote") }
                                Text(
                                        text = "${restaurantOption.votedUsers.size} votes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.7f
                                                )
                                )
                        }
                }
        }
}
