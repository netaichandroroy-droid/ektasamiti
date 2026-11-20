package com.example.data.cloud

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class GoogleSignInConfigStatus(
    val isReady: Boolean,
    val hasWebClientId: Boolean,
    val hasFirebaseConfig: Boolean
)

object GoogleAuthManager {
    private const val TAG = "GoogleAuthManager"
    const val SAMITI_GOOGLE_EMAIL = "jadobpursamiti@gmail.com"
    const val MISSING_CONFIG_ERROR_MSG =
        "এই APK-তে google-services.json কনফিগারেশন যুক্ত নেই (GitHub Secret GOOGLE_SERVICES_JSON সেট করা না থাকায়)"

    fun checkConfiguration(context: Context): GoogleSignInConfigStatus {
        val hasWebClientId = !resolveWebClientId(context).isNullOrBlank()
        val hasFirebaseConfig = !resolveStringResByName(context, "google_app_id").isNullOrBlank()
        return GoogleSignInConfigStatus(
            isReady = hasWebClientId && hasFirebaseConfig,
            hasWebClientId = hasWebClientId,
            hasFirebaseConfig = hasFirebaseConfig
        )
    }

    fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        addAuthStateListener(listener)
        awaitClose { removeAuthStateListener(listener) }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: (FirebaseUser) -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        val fbAuth = FirebaseSafeHelper.authOrNull(context)
        if (fbAuth == null) {
            onUnauthenticated()
            return
        }
        val existing = try {
            fbAuth.currentUser
        } catch (_: Exception) {
            null
        }
        if (existing != null) {
            onAuthSuccess(existing)
            return
        }
        val clientId = resolveWebClientId(context)
        if (clientId.isNullOrBlank()) {
            Log.w(TAG, "default_web_client_id not configured")
            onUnauthenticated()
            return
        }

        val request = try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()

            GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
        } catch (e: Exception) {
            Log.d(TAG, "Auto sign-in request build skipped: ${e.message}")
            onUnauthenticated()
            return
        }

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = fbAuth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        onAuthSuccess(user)
                    } else {
                        onUnauthenticated()
                    }
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Silent auto sign-in unavailable: ${e.message}")
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: (FirebaseUser) -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit = {}
    ) {
        val configStatus = checkConfiguration(context)
        val clientId = resolveWebClientId(context)
        if (!configStatus.hasWebClientId || clientId.isNullOrBlank()) {
            Log.w(TAG, "Google Sign-In unavailable: default_web_client_id missing in this build")
            onAuthError(MISSING_CONFIG_ERROR_MSG)
            return
        }

        val activityContext = context.findActivity()
        if (activityContext == null) {
            onAuthError("Google Sign-In চালু করতে অ্যাক্টিভিটি পাওয়া যায়নি")
            return
        }

        val fbAuth = FirebaseSafeHelper.authOrNull(context)
        if (fbAuth == null) {
            Log.w(TAG, "FirebaseAuth unavailable: google_app_id present=${configStatus.hasFirebaseConfig}")
            onAuthError(MISSING_CONFIG_ERROR_MSG)
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activityContext, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = fbAuth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        onAuthSuccess(user)
                    } else {
                        onAuthError("Google লগইন সম্পন্ন হয়নি")
                    }
                } else {
                    onAuthError("অপ্রত্যাশিত ক্রেডেনশিয়াল ধরন")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google credential found on device: ${e.message}", e)
                onAuthError("এই ফোনে কোনো Google অ্যাকাউন্ট পাওয়া যায়নি। ফোনের Settings থেকে $SAMITI_GOOGLE_EMAIL লগইন করুন।")
            } catch (e: GetCredentialProviderConfigurationException) {
                Log.w(TAG, "Credential Manager provider missing: ${e.message}", e)
                onAuthError("ডিভাইসে Google Play Services / Credential সেবা আপডেট করুন।")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential Manager error: ${e.type} - ${e.message}", e)
                onAuthError(e.localizedMessage ?: "Google সাইন-ইন ত্রুটি (${e.type})")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Google সাইন-ইন ব্যর্থ হয়েছে")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    try {
                        FirebaseSafeHelper.authOrNull()?.signOut()
                    } catch (_: Exception) {
                    }
                    onSignOutComplete()
                }
            }
        }
    }

    internal fun resolveWebClientId(context: Context): String? {
        return resolveStringResByName(context, "default_web_client_id")
    }

    internal fun resolveStringResByName(
        context: Context,
        resName: String,
        rStringClass: Class<*> = R.string::class.java
    ): String? {
        return try {
            val field = rStringClass.getField(resName)
            val resId = field.getInt(null)
            if (resId != 0) {
                context.getString(resId).takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
