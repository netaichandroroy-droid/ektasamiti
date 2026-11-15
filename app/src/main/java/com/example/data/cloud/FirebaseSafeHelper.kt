package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

object FirebaseSafeHelper {
    private const val TAG = "FirebaseSafeHelper"

    fun ensureInitialized(context: Context): Boolean {
        return try {
            val appContext = context.applicationContext ?: context
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                true
            } else {
                FirebaseApp.initializeApp(appContext) != null
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Firebase initialization unavailable: ${t.message}")
            false
        }
    }

    fun authOrNull(context: Context? = null): FirebaseAuth? {
        return try {
            if (context != null) {
                if (!ensureInitialized(context)) return null
            } else {
                FirebaseApp.getInstance()
            }
            Firebase.auth
        } catch (t: Throwable) {
            Log.w(TAG, "FirebaseAuth unavailable: ${t.message}")
            null
        }
    }

    fun firestoreOrNull(context: Context): FirebaseFirestore? {
        return try {
            val appContext = context.applicationContext ?: context
            if (!ensureInitialized(appContext)) return null
            val dbId = appContext.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank()) {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "FirebaseFirestore unavailable: ${t.message}")
            null
        }
    }

    fun authStateFlow(context: Context): Flow<FirebaseUser?> {
        val auth = authOrNull(context) ?: return flowOf(null)
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                trySend(firebaseAuth.currentUser)
            }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }
}
