package com.fyp.ekopantri.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI State for the Profile screen.
 */
data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val message: String? = null
)

/**
 * ViewModel responsible for user authentication and profile management.
 * Handles login, registration, logout, and profile updates (name and password).
 */
class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // --- 1. UI STATE FLOWS ---

    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail = _userEmail.asStateFlow()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    // --- 2. LIFECYCLE & LISTENERS ---

    init {
        // Automatically sync profile data whenever the authentication state changes
        auth.addAuthStateListener {
            fetchUserProfile()
        }
    }

    /**
     * Fetches the current user's profile data from Firestore.
     * Resets states if no user is logged in.
     */
    fun fetchUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _userName.value = ""
            _userEmail.value = ""
            _uiState.update { ProfileUiState() }
            return
        }

        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val name = it.getString("name") ?: ""
                    val email = it.getString("email") ?: ""

                    _userName.value = name
                    _userEmail.value = email

                    _uiState.update { old ->
                        old.copy(name = name, email = email)
                    }
                }
            }
    }

    // --- 3. PROFILE ACTIONS ---

    /**
     * Updates individual fields in the [ProfileUiState].
     */
    fun updateField(update: (ProfileUiState) -> ProfileUiState) {
        _uiState.update(update)
    }

    /**
     * Updates the user's display name in Firestore.
     * Note: Email is treated as a permanent identifier and cannot be changed.
     */
    fun updateProfile(onResult: (Boolean, String) -> Unit) {
        val state = _uiState.value
        val user = auth.currentUser ?: return
        
        _uiState.update { it.copy(isLoading = true) }
        
        db.collection("users").document(user.uid)
            .update("name", state.name)
            .addOnSuccessListener {
                _uiState.update { it.copy(isLoading = false) }
                onResult(true, "Profile updated successfully")
            }
            .addOnFailureListener {
                _uiState.update { it.copy(isLoading = false) }
                onResult(false, it.message ?: "Failed to update profile")
            }
    }

    /**
     * Changes the user's password after re-authenticating with the current password.
     */
    fun changePassword(onResult: (Boolean, String) -> Unit) {
        val state = _uiState.value
        val user = auth.currentUser ?: return

        // Basic validation
        if (state.newPassword.length < 6) {
            onResult(false, "Password must be at least 6 characters")
            return
        }
        if (state.newPassword != state.confirmPassword) {
            onResult(false, "Passwords do not match")
            return
        }

        val email = user.email ?: return
        val credential = EmailAuthProvider.getCredential(email, state.currentPassword)

        _uiState.update { it.copy(isLoading = true) }

        // Security requirement: Re-authenticate before sensitive changes
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(state.newPassword)
                    .addOnSuccessListener {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentPassword = "",
                                newPassword = "",
                                confirmPassword = ""
                            )
                        }
                        onResult(true, "Password updated successfully")
                    }
                    .addOnFailureListener {
                        _uiState.update { it.copy(isLoading = false) }
                        onResult(false, it.message ?: "Failed to update password")
                    }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(isLoading = false) }
                onResult(false, "Verification failed: Wrong current password")
            }
    }

    // --- 4. AUTHENTICATION ACTIONS ---

    /**
     * Registers a new user and creates a corresponding profile document in Firestore.
     */
    fun register(name: String, email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            onResult(false, "Please fill in all fields")
            return
        }
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: ""
                val userDetails = mapOf("name" to name, "email" to email)
                db.collection("users").document(userId).set(userDetails)
                onResult(true, null)
            }
            .addOnFailureListener { onResult(false, it.message) }
    }

    /**
     * Logs in an existing user with email and password.
     */
    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isEmpty() || pass.isEmpty()) {
            onResult(false, "Please fill in all fields")
            return
        }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    /**
     * Signs the user out of the application.
     */
    fun logout(onLogoutSuccess: () -> Unit) {
        auth.signOut()
        onLogoutSuccess()
    }
}
