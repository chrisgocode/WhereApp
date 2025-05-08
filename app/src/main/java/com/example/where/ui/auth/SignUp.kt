package com.example.where.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.where.R

@Composable
fun SignUpScreen(
        onSignInClick: () -> Unit,
        onGoogleSignInClick: () -> Unit,
        modifier: Modifier = Modifier,
        viewModel: AuthViewModel = hiltViewModel(),
        forceReloadOnboarding: (() -> Unit)? = null
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Handle authentication state changes
    LaunchedEffect(viewModel.isAuthenticated.value) {
        if (viewModel.isAuthenticated.value) {
            isLoading = false
        }
    }

    // Handle errors from ViewModel
    LaunchedEffect(viewModel.errorMessage.value) {
        viewModel.errorMessage.value?.let { message ->
            isLoading = false
            errorMessage = message
        }
    }

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .padding(
                                    horizontal = if (isTablet()) 64.dp else 32.dp,
                                    vertical = 32.dp
                            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        // Logo and title
        Image(
                painter = painterResource(id = R.drawable.ic_location),
                contentDescription = "Location Icon",
                modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
                text = "Create an Account",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = if (isTablet()) 32.sp else 24.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = "Sign up to start sharing your location with friends",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontSize = if (isTablet()) 18.sp else 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Full Name Field
        OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                placeholder = { Text("John Doe") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null && fullName.isBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field
        OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("you@example.com") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = errorMessage != null && email.isBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("••••••••") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation =
                        if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(
                            onClick = { passwordVisible = !passwordVisible },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                                text = if (passwordVisible) "Hide" else "Show",
                                color = Color(0xFF6200EE)
                        )
                    }
                },
                isError = errorMessage != null && password.length < 8
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = "Password must be at least 8 characters long",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Terms Checkbox
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = termsAccepted, onCheckedChange = { termsAccepted = it })
            Text(
                    text = "I agree to the Terms of Service and Privacy Policy",
                    style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Sign Up Button
        Button(
                onClick = {
                    if (termsAccepted) {
                        if (fullName.isBlank()) {
                            errorMessage = "Please enter your full name"
                        } else if (email.isBlank() || !email.contains("@")) {
                            errorMessage = "Please enter a valid email"
                        } else if (password.length < 8) {
                            errorMessage = "Password must be at least 8 characters long"
                        } else {
                            isLoading = true
                            errorMessage = null
                            viewModel.signUpWithEmail(
                                    fullName,
                                    email,
                                    password,
                                    forceReloadOnboarding
                            )
                        }
                    } else {
                        errorMessage = "Please accept the terms and conditions"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Sign Up", color = Color.White)
            }
        }

        // Error Message Snackbar
        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Snackbar(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
            ) { Text(message) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Button
        Button(
                onClick = { onGoogleSignInClick() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
            ) {
                Image(
                        painter = painterResource(id = R.drawable.google),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = "Sign up with Google",
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign In Link
        Row {
            Text("Already have an account? ")
            Text(
                    text = "Sign in",
                    color = Color(0xFF6200EE),
                    modifier = Modifier.clickable { onSignInClick() }
            )
        }
    }
}

@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 600
}
