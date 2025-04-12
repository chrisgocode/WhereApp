package com.example.where.ui.auth

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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

    init {
        Log.d("AuthViewModel", "Initialized with isAuthenticated: ${isAuthenticated.value}")
    }

    // Email/Password Authentication
    fun signUpWithEmail(name: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = null
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                )?.await()
                isAuthenticated.value = true
                currentUser.value = auth.currentUser
                Log.d("AuthViewModel", "Sign-up successful for ${email}")
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Registration failed"
                Log.e("AuthViewModel", "Sign-up failed: ${e.message}", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = null
                auth.signInWithEmailAndPassword(email, password).await()
                isAuthenticated.value = true
                currentUser.value = auth.currentUser
                Log.d("AuthViewModel", "Sign-in successful for ${email}")
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Authentication failed"
                Log.e("AuthViewModel", "Sign-in failed: ${e.message}", e)
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
                errorMessage.value = null
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                isAuthenticated.value = true
                currentUser.value = auth.currentUser
                Log.d("AuthViewModel", "Google sign-in successful for ${auth.currentUser?.email}")
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Google sign-in failed"
                Log.e("AuthViewModel", "Google sign-in failed: ${e.message}", e)
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
                errorMessage.value = null
                auth.sendPasswordResetEmail(email).await()
                errorMessage.value = "Reset email sent"
                Log.d("AuthViewModel", "Password reset email sent to ${email}")
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to send reset email"
                Log.e("AuthViewModel", "Password reset failed: ${e.message}", e)
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
        Log.d("AuthViewModel", "User signed out, isAuthenticated: ${isAuthenticated.value}")
    }

    // Initialize Google Sign-In Client
    fun getGoogleSignInClient(activity: Activity, webClientId: String): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activity, gso)
    }
}