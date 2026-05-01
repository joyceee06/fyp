package com.fyp.ekopantri.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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

    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isEmpty() || pass.isEmpty()) {
            onResult(false, "Please fill in all fields")
            return
        }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        onLogoutSuccess()
    }
}