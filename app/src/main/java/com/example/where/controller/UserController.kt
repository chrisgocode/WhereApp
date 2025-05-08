package com.example.where.controller

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class UserController @Inject constructor(private val firestore: FirebaseFirestore) {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FIELD_EMAIL = "email"
        private const val TAG = "UserController"
    }

    // fetches the Firebase UID for a given email by querying the users collection.
    suspend fun getUidForEmail(email: String): String? {
        Log.d(TAG, "Attempting to find UID for email: $email")
        return try {
            val querySnapshot =
                    firestore
                            .collection(USERS_COLLECTION)
                            .whereEqualTo(FIELD_EMAIL, email)
                            .limit(1)
                            .get()
                            .await()

            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                Log.d(TAG, "Found UID: ${document.id} for email: $email")
                document.id
            } else {
                Log.w(TAG, "No user found with email: $email")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching UID for email $email", e)
            null
        }
    }
}
