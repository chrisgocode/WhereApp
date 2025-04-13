package com.example.where.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.where.R
import com.example.where.ui.auth.AuthViewModel
import com.example.where.ui.auth.SignInScreen
import com.example.where.ui.auth.SignUpScreen
import com.example.where.ui.screens.onboarding.OnboardingScreen
import com.example.where.ui.screens.onboarding.OnboardingViewModel
import com.example.where.ui.theme.WhereTheme
import com.example.where.ui.screens.home.HomeScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhereTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WhereApp(dataStore = dataStore)
                }
            }
        }
    }
}

@Composable
fun WhereApp(dataStore: DataStore<Preferences>) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val onboardingViewModel: OnboardingViewModel = viewModel { OnboardingViewModel(dataStore) }
    val context = LocalContext.current
    
    // Track if initial destination has been determined
    var isInitializing by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf("sign_in") }

    // Collect the initialization state
    val initializationComplete by onboardingViewModel.initializationComplete.collectAsState(initial = false)

    // Google Sign-In Client Setup
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    authViewModel.handleGoogleSignIn(token, dataStore)
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Sign-in failed: ${e.statusCode}", e)
                authViewModel.errorMessage.value = "Google sign-in failed: ${e.statusCode}"
            }
        }
    }

    // Check authentication and onboarding status before showing any UI
    LaunchedEffect(authViewModel.isAuthenticated.value, onboardingViewModel.onboardingCompleted.value, initializationComplete, authViewModel.isNewAccount.value) {
        // Check navigation state
        Log.d("WhereApp", "Nav state: auth=${authViewModel.isAuthenticated.value}, onboarding=${onboardingViewModel.onboardingCompleted.value}, init=$initializationComplete")
        
        if (initializationComplete) {
            if (authViewModel.isAuthenticated.value) {
                if (authViewModel.isNewAccount.value) {
                    startDestination = "onboarding"
                    Log.d("WhereApp", "New user -> onboarding")
                } else if (onboardingViewModel.onboardingCompleted.value) {
                    startDestination = "home"
                    Log.d("WhereApp", "Existing user with completed onboarding -> home")
                } else {
                    startDestination = "onboarding"
                    Log.d("WhereApp", "Existing user with incomplete onboarding -> onboarding")
                }
            } else {
                startDestination = "sign_in"
                Log.d("WhereApp", "Not authenticated -> sign_in")
            }
            
            isInitializing = false
        }
    }

    if (isInitializing) {
        Box(modifier = Modifier.fillMaxSize()) {
            // TODO: Add a loading indicator or splash screen here
        }
        return
    }
    
    // Runtime navigation - separate from initialization
    LaunchedEffect(authViewModel.isAuthenticated.value, onboardingViewModel.onboardingCompleted.value, authViewModel.isNewAccount.value) {
        // Only handle navigation if we have a current destination
        navController.currentDestination?.let { currentDestination ->
            if (authViewModel.isAuthenticated.value) {
                if (authViewModel.isNewAccount.value && currentDestination.route != "onboarding") {
                    // Newly created account should go to onboarding
                    navController.navigate("onboarding") {
                        popUpTo(0) // Clear back stack
                    }
                    // Reset new account flag after navigation
                    authViewModel.isNewAccount.value = false
                } else if (onboardingViewModel.onboardingCompleted.value && 
                    currentDestination.route == "onboarding") {
                    // If onboarding is complete but we're on onboarding screen, go to home
                    navController.navigate("home") {
                        popUpTo(0)
                    }
                } else if (!onboardingViewModel.onboardingCompleted.value && 
                          currentDestination.route != "onboarding" &&
                          currentDestination.route != "sign_in" && 
                          currentDestination.route != "sign_up") {
                    // If onboarding not complete and we're not on onboarding or auth screens, go to onboarding
                    navController.navigate("onboarding") {
                        popUpTo(0)
                    }
                }
            } else if (currentDestination.route !in listOf("sign_in", "sign_up")) {
                // If not authenticated and not on auth screens, go to sign in
                navController.navigate("sign_in") {
                    popUpTo(0)
                }
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

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth flow
        composable("sign_in") {
            SignInScreen(
                onSignUpClick = { navController.navigate("sign_up") },
                onGoogleSignInClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                viewModel = authViewModel,
                dataStore = dataStore
            )
        }

        composable("sign_up") {
            SignUpScreen(
                onSignInClick = { navController.navigate("sign_in") },
                onGoogleSignInClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                viewModel = authViewModel,
                dataStore = dataStore,
                forceReloadOnboarding = { onboardingViewModel.forceReloadOnboardingStatus() }
            )
        }

        composable("onboarding") {
            OnboardingScreen(
                viewModel = onboardingViewModel,
                navController = navController,
                onFinish = {
                    onboardingViewModel.completeOnboarding()
                    navController.navigate("home") {
                       popUpTo(0) // Clear back stack
                    }
                }
            )
        }

        // Main app flow (post-authentication)
        composable("home") {
            HomeScreen(
                onSignOut = {
                    authViewModel.signOut(dataStore)
                    onboardingViewModel.resetOnSignOut()
                    navController.navigate("sign_in") {
                        popUpTo(0) // Clear back stack
                    }
                }
            )
        }
    }
}
