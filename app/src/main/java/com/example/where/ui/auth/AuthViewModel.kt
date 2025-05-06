package com.example.where.ui.auth

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class AuthViewModel @Inject constructor(private val dataStore: DataStore<Preferences>) :
        ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    val isAuthenticated = mutableStateOf(auth.currentUser != null)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val currentUser = mutableStateOf(auth.currentUser)

    // Add a flag to track newly created accounts
    val isNewAccount = mutableStateOf(false)

    // Key for onboarding status in DataStore
    companion object {
        private val ONBOARDING_COMPLETED_KEY = stringPreferencesKey("onboarding_completed")
    }

    init {
        Log.d("AuthViewModel", "Initialized with isAuthenticated: ${isAuthenticated.value}")
    }

    // Email/Password Authentication
    fun signUpWithEmail(
            name: String,
            email: String,
            password: String,
            forceReloadOnboarding: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = null
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user
                        ?.updateProfile(
                                UserProfileChangeRequest.Builder().setDisplayName(name).build()
                        )
                        ?.await()

                // Mark as new account
                isNewAccount.value = true
                Log.d("AuthViewModel", "New account created for $email")

                // Reset onboarding in DataStore
                try {
                    dataStore.edit { preferences ->
                        preferences[ONBOARDING_COMPLETED_KEY] = "false"
                    }

                    // Force reload if callback provided
                    forceReloadOnboarding?.invoke()
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed to reset onboarding in DataStore", e)
                }

                // Create Firestore document
                val user = auth.currentUser
                if (user != null) {
                    val db = FirebaseFirestore.getInstance()
                    val userData =
                            hashMapOf(
                                    "uid" to user.uid,
                                    "email" to email,
                                    "displayName" to name,
                                    "dietaryRestrictions" to listOf<String>(),
                                    "cuisinePreferences" to listOf<String>(),
                                    "priceRange" to 2,
                                    "onboardingCompleted" to false,
                                    "createdAt" to Timestamp.now(),
                                    "updatedAt" to Timestamp.now()
                            )

                    db.collection("users")
                            .document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d("AuthViewModel", "User document created in Firestore")
                            }
                            .addOnFailureListener { e ->
                                Log.e("AuthViewModel", "Error creating user document", e)
                            }
                }

                isAuthenticated.value = true
                currentUser.value = auth.currentUser
                Log.d("AuthViewModel", "Sign-up successful for $email")
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
                val authResult = auth.signInWithEmailAndPassword(email, password).await()

                // After successful sign-in, check onboarding status in Firestore
                val user = authResult.user
                if (user != null) {
                    val db = FirebaseFirestore.getInstance()
                    val userDoc = db.collection("users").document(user.uid).get().await()

                    if (userDoc.exists()) {
                        // Check onboarding status
                        val onboardingCompleted = userDoc.getBoolean("onboardingCompleted") ?: false
                        Log.d(
                                "AuthViewModel",
                                "User ${user.email} onboardingCompleted: $onboardingCompleted"
                        )

                        // Update DataStore with Firestore onboarding status
                        dataStore.edit { preferences ->
                            preferences[ONBOARDING_COMPLETED_KEY] =
                                    if (onboardingCompleted) "true" else "false"
                        }
                        Log.d(
                                "AuthViewModel",
                                "Updated DataStore onboarding status to: $onboardingCompleted"
                        )

                        // Set isNewAccount to false if onboarding is completed
                        isNewAccount.value = !onboardingCompleted
                    } else {
                        Log.d("AuthViewModel", "No Firestore document found for email user")
                        // If no document exists (rare case), treat as new account
                        isNewAccount.value = true
                    }
                }

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
                val authResult = auth.signInWithCredential(credential).await()

                // Check if this is a new or existing user
                val user = authResult.user
                if (user != null) {
                    // Check if user document exists in Firestore
                    val db = FirebaseFirestore.getInstance()
                    val userDocRef = db.collection("users").document(user.uid)
                    val userDoc = userDocRef.get().await()

                    if (!userDoc.exists()) {
                        // Document doesn't exist, create one with default values
                        Log.d(
                                "AuthViewModel",
                                "Creating new Firestore document for Google user ${user.email}"
                        )
                        val userData =
                                hashMapOf(
                                        "uid" to user.uid,
                                        "email" to user.email,
                                        "displayName" to user.displayName,
                                        "dietaryRestrictions" to listOf<String>(),
                                        "cuisinePreferences" to listOf<String>(),
                                        "priceRange" to 2,
                                        "onboardingCompleted" to false,
                                        "createdAt" to Timestamp.now(),
                                        "updatedAt" to Timestamp.now()
                                )

                        userDocRef.set(userData).await()

                        // Update DataStore with onboarding status (false for new account)
                        dataStore.edit { preferences ->
                            preferences[ONBOARDING_COMPLETED_KEY] = "false"
                        }
                        Log.d("AuthViewModel", "Updated DataStore onboarding status to: false")

                        // Mark as new account to trigger onboarding
                        isNewAccount.value = true
                        Log.d(
                                "AuthViewModel",
                                "New Google account created and marked for onboarding"
                        )
                    } else {
                        // Document exists, check onboarding status
                        val onboardingCompleted = userDoc.getBoolean("onboardingCompleted") ?: false

                        // Update DataStore with Firestore onboarding status
                        dataStore.edit { preferences ->
                            preferences[ONBOARDING_COMPLETED_KEY] =
                                    if (onboardingCompleted) "true" else "false"
                        }
                        Log.d(
                                "AuthViewModel",
                                "Updated DataStore onboarding status to: $onboardingCompleted"
                        )

                        isNewAccount.value = !onboardingCompleted
                        Log.d(
                                "AuthViewModel",
                                "Existing Google user found, onboardingCompleted: $onboardingCompleted"
                        )
                    }
                }

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
        // Reset new account flag on sign out
        isNewAccount.value = false

        // Completely clear DataStore preferences if available
        viewModelScope.launch {
            try {
                // Clear all preferences by setting to empty
                dataStore.edit { preferences -> preferences.clear() }
                Log.d("AuthViewModel", "Cleared all preferences in DataStore on sign out")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to clear DataStore preferences on sign out", e)
            }
        }

        Log.d("AuthViewModel", "User signed out, isAuthenticated: ${isAuthenticated.value}")
    }

    // Initialize Google Sign-In Client
    fun getGoogleSignInClient(activity: Activity, webClientId: String): GoogleSignInClient {
        val gso =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
        return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activity, gso)
    }
}
