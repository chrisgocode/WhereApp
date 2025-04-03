package com.example.where.ui.auth

import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    val isAuthenticated = mutableStateOf(auth.currentUser != null)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val currentUser = mutableStateOf(auth.currentUser)

    // Email/Password Authentication
    fun signUpWithEmail(name: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                )?.await()
                isAuthenticated.value = true
                currentUser.value = auth.currentUser
                errorMessage.value = null
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Registration failed"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                auth.signInWithEmailAndPassword(email, password).await()
                isAuthenticated.value = true
                currentUser.value = auth.currentUser
                errorMessage.value = null
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Authentication failed"
            } finally {
                isLoading.value = false
            }
        }
    }

    // Google Authentication
    fun handleGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                isAuthenticated.value = true
                currentUser.value = auth.currentUser
                errorMessage.value = null
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Google sign-in failed"
            } finally {
                isLoading.value = false
            }
        }
    }

    // Password Reset
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                auth.sendPasswordResetEmail(email).await()
                errorMessage.value = "Reset email sent"
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to send reset email"
            } finally {
                isLoading.value = false
            }
        }
    }

    // Sign Out
    fun signOut() {
        auth.signOut()
        isAuthenticated.value = false
        currentUser.value = null
        errorMessage.value = null
    }

    // Initialize Google Sign-In Client
    fun getGoogleSignInClient(activity: Activity, webClientId: String): GoogleSignInClient {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activity, gso)
    }
}