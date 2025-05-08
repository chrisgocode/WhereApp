package com.example.where.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.where.R
import com.example.where.controller.RestaurantController
import com.example.where.ui.auth.AuthViewModel
import com.example.where.ui.auth.SignInScreen
import com.example.where.ui.auth.SignUpScreen
import com.example.where.ui.screens.home.HomeScreen
import com.example.where.ui.screens.onboarding.OnboardingScreen
import com.example.where.ui.screens.onboarding.OnboardingViewModel
import com.example.where.ui.screens.profile.ProfileScreen
import com.example.where.ui.screens.shared.BottomNavBar
import com.example.where.ui.theme.WhereTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.navigation.NavController
import com.example.where.ui.screens.groups.GroupDetailScreen
import com.example.where.ui.screens.shared.BottomNavBar
import com.example.where.ui.screens.groups.GroupsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhereTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) { WhereApp() }
            }
        }
    }
}

@Composable
fun WhereApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val context = LocalContext.current

    // Initialize RestaurantController
    val restaurantController = remember { RestaurantController(context) }
    LaunchedEffect(Unit) {
        restaurantController.setApiKey(apiKey)
    }

    // Track if initial destination has been determined
    var isInitializing by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf("sign_in") }

    // Collect the initialization state
    val initializationComplete by
            onboardingViewModel.initializationComplete.collectAsState(initial = false)

    // Google Sign-In Client Setup
    val googleSignInClient = remember {
        val gso =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Google Sign-In Launcher
    val googleSignInLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val data = result.data
                    try {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                        val account = task.getResult(ApiException::class.java)
                        account.idToken?.let { token -> authViewModel.handleGoogleSignIn(token) }
                    } catch (e: ApiException) {
                        Log.e("GoogleSignIn", "Sign-in failed: ${e.statusCode}", e)
                        authViewModel.errorMessage.value = "Google sign-in failed: ${e.statusCode}"
                    }
                }
            }

    // Check authentication and onboarding status for initial destination
    LaunchedEffect(
            authViewModel.isAuthenticated.value,
            onboardingViewModel.onboardingCompleted.value,
            initializationComplete,
            authViewModel.isNewAccount.value
    ) {
        if (initializationComplete) {
            startDestination =
                    when {
                        !authViewModel.isAuthenticated.value -> "sign_in"
                        authViewModel.isNewAccount.value ||
                                !onboardingViewModel.onboardingCompleted.value -> "onboarding"
                        else -> "home"
                    }
            Log.d("WhereApp", "Start destination: $startDestination")
            isInitializing = false
        }
    }

    // Handle runtime navigation changes
    LaunchedEffect(
            authViewModel.isAuthenticated.value,
            onboardingViewModel.onboardingCompleted.value,
            authViewModel.isNewAccount.value
    ) {
        navController.currentDestination?.route?.let { currentRoute ->
            if (authViewModel.isAuthenticated.value) {
                if (authViewModel.isNewAccount.value && currentRoute != "onboarding") {
                    navController.navigate("onboarding") { popUpTo(0) }
                    authViewModel.isNewAccount.value = false
                } else if (onboardingViewModel.onboardingCompleted.value &&
                                currentRoute == "onboarding"
                ) {
                    navController.navigate("home") { popUpTo(0) }
                } else if (!onboardingViewModel.onboardingCompleted.value &&
                                currentRoute !in listOf("onboarding", "sign_in", "sign_up")
                ) {
                    navController.navigate("onboarding") { popUpTo(0) }
                }
            } else if (currentRoute !in listOf("sign_in", "sign_up")) {
                navController.navigate("sign_in") { popUpTo(0) }
            }
        }
    }

    // Observe error messages
    LaunchedEffect(authViewModel.errorMessage.value) {
        authViewModel.errorMessage.value?.let { error ->
            Log.e("AuthError", error)
            // TODO: Show snackbar on error
        }
    }

    if (isInitializing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = startDestination) {
        // Auth flow
        composable("sign_in") {
            SignInScreen(
                    onSignUpClick = { navController.navigate("sign_up") },
                    onGoogleSignInClick = {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    viewModel = authViewModel
            )
        }

        composable("sign_up") {
            SignUpScreen(
                    onSignInClick = { navController.navigate("sign_in") },
                    onGoogleSignInClick = {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    viewModel = authViewModel,
                    forceReloadOnboarding = { onboardingViewModel.forceReloadOnboardingStatus() }
            )
        }

        composable("onboarding") {
            OnboardingScreen(
                    viewModel = onboardingViewModel,
                    navController = navController,
                    onFinish = {
                        onboardingViewModel.completeOnboarding()
                        navController.navigate("home") { popUpTo(0) }
                    }
            )
        }

        // Main app flow (post-authentication)
        composable("home") {
            HomeScreen(
                navController = navController,
                onSignOut = {
                    authViewModel.signOut(dataStore)
                    onboardingViewModel.resetOnSignOut()
                    navController.navigate("sign_in") {
                        popUpTo(0)
                    }
                },
                onNavItemClick = { route ->
                    Log.d("Navigation", "HomeScreen: onNavItemClick called with route: $route")
                    Log.d("Navigation", "Current route: ${navController.currentDestination?.route}")
                    if (route != navController.currentDestination?.route) {
                        navController.navigate(route) {
                            popUpTo("home") {
                                saveState = true
                            }
                        }
                        Log.d("Navigation", "Navigating to $route")
                    } else {
                        Log.d("Navigation", "Already on route: $route")
                    }
            )
        }
        composable("groups") {
            GroupsScreen(
                navController = navController,
                onNavItemClick = { route ->
                    Log.d("Navigation", "GroupsScreen: onNavItemClick called with route: $route")
                    Log.d("Navigation", "Current route: ${navController.currentDestination?.route}")
                    if (route != navController.currentDestination?.route) {
                        navController.navigate(route) {
                            popUpTo("home") {
                                saveState = true
                            }
                        }
                        Log.d("Navigation", "Navigating to $route")
                    } else {
                        Log.d("Navigation", "Already on route: $route")
                    }
                },
                dataStore = dataStore
            )
        }
        composable("groupDetail/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupDetailScreen(
                navController = navController,
                groupId = groupId,
                restaurantController = restaurantController
            )
        }
        composable("meetup") {
            MeetupScreen(
                navController = navController,
                onNavItemClick = { route ->
                    Log.d("Navigation", "MeetupScreen: onNavItemClick called with route: $route")
                    Log.d("Navigation", "Current route: ${navController.currentDestination?.route}")
                    if (route != navController.currentDestination?.route) {
                        navController.navigate(route) {
                            popUpTo("home") {
                                saveState = true
                            }
                        }
                        Log.d("Navigation", "Navigating to $route")
                    } else {
                        Log.d("Navigation", "Already on route: $route")
                    }
            )
        }
        composable("profile") {
            ProfileScreen(
                viewModel = viewModel(factory = ProfileViewModelFactory(dataStore, context)),
                navController = navController,
                onSignOut = {
                    authViewModel.signOut(dataStore)
                    onboardingViewModel.resetOnSignOut()
                    navController.navigate("sign_in") {
                        popUpTo(0)
                    }
                },
                onNavItemClick = { route ->
                    Log.d("Navigation", "ProfileScreen: onNavItemClick called with route: $route")
                    Log.d("Navigation", "Current route: ${navController.currentDestination?.route}")
                    if (route != navController.currentDestination?.route) {
                        navController.navigate(route) {
                            popUpTo("home") { // Use "home" instead of startDestinationId
                                saveState = true
                            }
                        }
                        Log.d("Navigation", "Navigating to $route")
                    } else {
                        Log.d("Navigation", "Already on route: $route")
                    }
            )
        }
    }
}

@Composable
fun GroupsScreen(
    navController: NavController,
    onNavItemClick: (String) -> Unit,
    dataStore: DataStore<Preferences>
) {
    Scaffold(
            bottomBar = { BottomNavBar(selectedRoute = "groups", onNavItemClick = onNavItemClick) }
    ) { innerPadding ->
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
        ) { Text("Groups Screen (Placeholder)") }
    }
}

// Placeholder MeetupScreen
@Composable
fun MeetupScreen(navController: NavController, onNavItemClick: (String) -> Unit) {
    Scaffold(
            bottomBar = { BottomNavBar(selectedRoute = "meetup", onNavItemClick = onNavItemClick) }
    ) { innerPadding ->
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
        ) { Text("Meetup Screen (Placeholder)") }
    }
}
