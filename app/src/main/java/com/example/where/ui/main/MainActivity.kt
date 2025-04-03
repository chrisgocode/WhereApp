package com.example.where.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.where.R
import com.example.where.ui.auth.SignInScreen
import com.example.where.ui.auth.SignUpScreen
import com.example.where.ui.theme.WhereTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.where.ui.auth.AuthViewModel
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhereTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val authViewModel: AuthViewModel = viewModel()

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
                        if (result.resultCode == RESULT_OK) {
                            val data = result.data
                            try {
                                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                                val account = task.getResult(ApiException::class.java)
                                account.idToken?.let { token ->
                                    authViewModel.handleGoogleSignIn(token)
                                }
                            } catch (e: ApiException) {
                                Log.e("GoogleSignIn", "Sign-in failed: ${e.statusCode}", e)
                                authViewModel.errorMessage.value = "Google sign-in failed: ${e.statusCode}"
                            }
                        }
                    }

                    // Observe authentication state
                    LaunchedEffect(authViewModel.isAuthenticated.value) {
                        if (authViewModel.isAuthenticated.value) {
                            navController.navigate("home") {
                                popUpTo(0) // Clear back stack
                            }
                        }
                    }

                    // Observe error messages
                    LaunchedEffect(authViewModel.errorMessage.value) {
                        authViewModel.errorMessage.value?.let { error ->
                            Log.e("AuthError", error)
                            // Here you could show a Snackbar with the error message
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "sign_in"
                    ) {
                        composable("sign_in") {
                            SignInScreen(
                                onSignUpClick = { navController.navigate("sign_up") },
                                onGoogleSignInClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                                viewModel = authViewModel
                            )
                        }
                        composable("sign_up") {
                            SignUpScreen(
                                onSignInClick = { navController.navigate("sign_in") },
                                onGoogleSignInClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                                viewModel = authViewModel
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                onSignOut = { authViewModel.signOut() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onSignOut: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to the Home Screen!")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}