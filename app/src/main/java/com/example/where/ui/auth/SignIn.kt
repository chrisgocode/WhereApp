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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.where.R

@Composable
fun SignInScreen(
    onSignUpClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Handle authentication state changes
    LaunchedEffect(viewModel.isAuthenticated.value) {
        if (viewModel.isAuthenticated.value) {
            isLoading = false
            onSignInSuccess()
        }
    }

    // Handle errors
    LaunchedEffect(viewModel.errorMessage.value) {
        viewModel.errorMessage.value?.let {
            isLoading = false
            // Error handling (e.g., show Snackbar)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet()) 64.dp else 32.dp, vertical = 32.dp),
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
            text = "Sign In",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = if (isTablet()) 32.sp else 24.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in to share your location with friends",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            fontSize = if (isTablet()) 18.sp else 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            placeholder = { Text("you@example.com") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            placeholder = { Text("••••••••") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Sign In Button
        Button(
            onClick = {
                isLoading = true
                viewModel.signInWithEmail(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text("Sign In", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Button
        Button(
            onClick = { onGoogleSignInClick() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
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
                    text = "Sign in with Google",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up Link
        Row {
            Text("Don't have an account? ")
            Text(
                text = "Sign up",
                color = Color(0xFF6200EE),
                modifier = Modifier.clickable { onSignUpClick() }
            )
        }
    }
}
