package com.example.ui.viewmodel

import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.FirebaseSafeHelper
import com.example.data.cloud.GoogleAuthManager
import com.example.data.local.SessionPreferences
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.SamitiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: SamitiRepository,
    private val sessionPreferences: SessionPreferences? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(isCheckingSession = sessionPreferences != null)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        restoreSavedSession()
    }

    private fun restoreSavedSession() {
        val prefs = sessionPreferences ?: return
        viewModelScope.launch {
            try {
                val savedId = prefs.getLoggedInUserId()
                val savedEmail = prefs.getLoggedInUserEmail()
                val restoredLocal = when {
                    savedId != null -> repository.getUserById(savedId)
                        ?: savedEmail?.let { repository.getUserByEmail(it) }
                    !savedEmail.isNullOrBlank() -> repository.getUserByEmail(savedEmail)
                    else -> null
                }

                if (restoredLocal != null) {
                    _uiState.value = AuthUiState(
                        currentUser = restoredLocal,
                        isLoading = false,
                        isCheckingSession = false
                    )
                    return@launch
                }

                val firebaseUser = FirebaseSafeHelper.authOrNull()?.currentUser
                if (firebaseUser != null) {
                    val gUser = resolveOrCreateGoogleSamitiUser(
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName
                    )
                    prefs.saveLoggedInSession(gUser.id, gUser.email)
                    _uiState.value = AuthUiState(
                        currentUser = gUser,
                        isLoading = false,
                        isCheckingSession = false
                    )
                } else {
                    prefs.clearSession()
                    _uiState.value = AuthUiState(
                        currentUser = null,
                        isLoading = false,
                        isCheckingSession = false
                    )
                }
            } catch (_: Exception) {
                _uiState.value = AuthUiState(
                    currentUser = null,
                    isLoading = false,
                    isCheckingSession = false
                )
            }
        }
    }

    private suspend fun resolveOrCreateGoogleSamitiUser(
        email: String?,
        displayName: String?
    ): User {
        val normalizedEmail = email?.trim()?.ifBlank { null } ?: GoogleAuthManager.SAMITI_GOOGLE_EMAIL
        val existing = repository.getUserByEmail(normalizedEmail)
        if (existing != null) return existing
        val defaultName = if (normalizedEmail.equals(GoogleAuthManager.SAMITI_GOOGLE_EMAIL, ignoreCase = true)) {
            "যাদবপুর একতা সমিতি (অ্যাডমিন)"
        } else {
            "একতা সমিতি পরিচালক"
        }
        val newUser = User(
            email = normalizedEmail,
            passwordHash = "google_oauth",
            fullName = displayName?.trim()?.ifBlank { null } ?: defaultName,
            mobile = "",
            role = UserRole.ADMIN,
            isActive = true
        )
        val id = repository.registerUser(newUser)
        return newUser.copy(id = id)
    }

    fun onGoogleFirebaseSignedIn(
        email: String?,
        displayName: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = resolveOrCreateGoogleSamitiUser(email, displayName)
            sessionPreferences?.saveLoggedInSession(user.id, user.email)
            _uiState.value = AuthUiState(
                currentUser = user,
                isLoading = false,
                isCheckingSession = false,
                errorMessage = null
            )
            onSuccess()
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val user = repository.authenticate(email, pass)
            if (user != null) {
                sessionPreferences?.saveLoggedInSession(user.id, user.email)
                _uiState.value = AuthUiState(
                    currentUser = user,
                    isLoading = false,
                    isCheckingSession = false
                )
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isCheckingSession = false,
                    errorMessage = "ভুল ইমেইল অথবা পাসওয়ার্ড! আবার চেষ্টা করুন।"
                )
            }
        }
    }

    fun quickLoginAs(role: UserRole) {
        if (role == UserRole.ADMIN) {
            login("Nitai", "Nemai")
        } else {
            login("collector@ektasamiti.com", "123456")
        }
    }

    fun logout(credentialManager: CredentialManager? = null) {
        sessionPreferences?.clearSession()
        if (credentialManager != null) {
            GoogleAuthManager.signOut(credentialManager, viewModelScope)
        } else {
            try {
                FirebaseSafeHelper.authOrNull()?.signOut()
            } catch (_: Exception) {
            }
        }
        _uiState.value = AuthUiState(
            currentUser = null,
            isLoading = false,
            isCheckingSession = false
        )
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
