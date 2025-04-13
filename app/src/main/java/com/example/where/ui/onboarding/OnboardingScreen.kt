package com.example.where.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.where.models.UserPreferences

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = viewModel(),
    onFinish: () -> Unit
) {
    val defaultPrefs = UserPreferences()
    val userPrefsState = viewModel.userPreferences.collectAsState()
    val userPreferences = userPrefsState.value ?: defaultPrefs
    var currentPage by remember { mutableStateOf(0) }

    LaunchedEffect(viewModel.onboardingCompleted.value) {
        if (viewModel.onboardingCompleted.value) {
            onFinish()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentPage) {
            0 -> CuisineAndDietaryPage(
                userPreferences = userPreferences,
                onCuisineChanged = viewModel::updateCuisinePreferences,
                onDietaryChanged = viewModel::updateDietaryRestrictions,
                onNext = { currentPage = 1 }
            )
            1 -> PriceRangePage(
                priceRange = userPreferences.priceRange,
                onPriceRangeChanged = viewModel::updatePriceRange,
                onBack = { currentPage = 0 },
                onFinish = { viewModel.completeOnboarding() }
            )
        }

        if (viewModel.isLoading.value) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }

        viewModel.errorMessage.value?.let { error ->
            Snackbar(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(error)
            }
        }
    }
}

@Composable
fun CuisineAndDietaryPage(
    userPreferences: UserPreferences,
    onCuisineChanged: (List<String>) -> Unit,
    onDietaryChanged: (List<String>) -> Unit,
    onNext: () -> Unit
) {
    val selectedCuisines = remember { mutableStateListOf<String>().apply {
        addAll(userPreferences.cuisinePreferences)
    }}

    val selectedDietary = remember { mutableStateListOf<String>().apply {
        addAll(userPreferences.dietaryRestrictions)
    }}

    val cuisineOptions = listOf(
        "Italian", "Mexican", "Chinese", "Japanese", "Indian",
        "Thai", "American", "French", "Mediterranean", "Korean"
    )

    val dietaryOptions = listOf(
        "Vegetarian", "Vegan", "Gluten Free", "Dairy Free",
        "Nut Free", "Pescatarian", "Halal", "Kosher"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet()) 64.dp else 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { 0.5f },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Cuisine Preferences",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = if (isTablet()) 28.sp else 22.sp
        )

        Text(
            text = "What types of food do you enjoy most?",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isTablet()) 3 else 2),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            items(cuisineOptions) { cuisine ->
                val isSelected = selectedCuisines.contains(cuisine)

                SelectableChip(
                    text = cuisine,
                    isSelected = isSelected,
                    onSelected = {
                        if (isSelected) {
                            selectedCuisines.remove(cuisine)
                        } else {
                            selectedCuisines.add(cuisine)
                        }
                        onCuisineChanged(selectedCuisines.toList())
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Dietary Restrictions",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = if (isTablet()) 28.sp else 22.sp
        )

        Text(
            text = "Select any dietary restrictions you have (optional)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isTablet()) 3 else 2),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            items(dietaryOptions) { dietary ->
                val isSelected = selectedDietary.contains(dietary)

                SelectableChip(
                    text = dietary,
                    isSelected = isSelected,
                    onSelected = {
                        if (isSelected) {
                            selectedDietary.remove(dietary)
                        } else {
                            selectedDietary.add(dietary)
                        }
                        onDietaryChanged(selectedDietary.toList())
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onCuisineChanged(selectedCuisines)
                onDietaryChanged(selectedDietary)
                onNext()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            Text("Next", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun PriceRangePage(
    priceRange: Int,
    onPriceRangeChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    var selectedPrice by remember { mutableStateOf(priceRange) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet()) 64.dp else 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Price Range",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = if (isTablet()) 28.sp else 22.sp
        )

        Text(
            text = "What's your preferred price range for dining?",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PriceButton(
                price = "$",
                priceLevel = 1,
                description = "Inexpensive restaurants",
                isSelected = selectedPrice == 1,
                onClick = {
                    selectedPrice = 1
                    onPriceRangeChanged(1)
                }
            )

            PriceButton(
                price = "$$",
                priceLevel = 2,
                description = "Moderately priced restaurants",
                isSelected = selectedPrice == 2,
                onClick = {
                    selectedPrice = 2
                    onPriceRangeChanged(2)
                }
            )

            PriceButton(
                price = "$$$",
                priceLevel = 3,
                description = "Expensive restaurants",
                isSelected = selectedPrice == 3,
                onClick = {
                    selectedPrice = 3
                    onPriceRangeChanged(3)
                }
            )

            PriceButton(
                price = "$$$$",
                priceLevel = 4,
                description = "Very expensive restaurants",
                isSelected = selectedPrice == 4,
                onClick = {
                    selectedPrice = 4
                    onPriceRangeChanged(4)
                }
            )
        }

        Text(
            text = when (selectedPrice) {
                1 -> "Inexpensive restaurants"
                2 -> "Moderately priced restaurants"
                3 -> "Expensive restaurants"
                4 -> "Very expensive restaurants"
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF6200EE)
                )
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                Text("Finish", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SelectableChip(
    text: String,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF6200EE) else Color.White
    val textColor = if (isSelected) Color.White else Color.Black
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.LightGray

    Surface(
        modifier = Modifier
            .padding(4.dp)
            .height(48.dp),
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        onClick = onSelected
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PriceButton(
    price: String,
    priceLevel: Int,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF6200EE) else Color.White
    val textColor = if (isSelected) Color.White else Color.Black
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.LightGray

    Surface(
        modifier = Modifier
            .width(72.dp)
            .height(48.dp),
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = price,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 600
}