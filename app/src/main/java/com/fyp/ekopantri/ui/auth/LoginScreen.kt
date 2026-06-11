package com.fyp.ekopantri.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fyp.ekopantri.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    // 1. Logic States
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 2. Form States
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AccountHeader(
                title = "Login",
                subtitle = "Welcome back to EkoPantri"
            )

            Spacer(Modifier.height(32.dp))

            AccountTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                modifier = Modifier.width(320.dp),
                keyboardType = KeyboardType.Email
            )

            Spacer(Modifier.height(12.dp))

            AccountTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                modifier = Modifier.width(320.dp),
                isPassword = true
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            AccountButton(
                text = "Login",
                isLoading = isLoading,
                modifier = Modifier.width(320.dp),
                onClick = {
                    isLoading = true
                    viewModel.login(email, password) { success, message ->
                        isLoading = false
                        if (success) {
                            onLoginSuccess()
                        } else {
                            errorMessage = mapFirebaseError(message)
                        }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
            )

            Spacer(Modifier.height(16.dp))

            AccountFooter(
                mainText = "Don't have an account?",
                actionText = "Register here",
                onActionClick = onNavigateToRegister
            )
        }
    }
}

// --- LOGIC HELPERS ---

private fun mapFirebaseError(message: String?): String {
    if (message == null) return "An unknown error occurred"

    return when {
        message.contains("password", ignoreCase = true) -> "The password is invalid"
        message.contains("no user record", ignoreCase = true) -> "There is no user record"
        message.contains("identifier", ignoreCase = true) -> "There is no user record"
        message.contains("timeout", ignoreCase = true) ||
                message.contains("network", ignoreCase = true) -> "Network timeout"
        message.contains("email address is badly formatted", ignoreCase = true) -> "Invalid email format"
        message.contains("credential not available", ignoreCase = true) -> "Google Play Services error. Please check your account or try again."
        else -> message
    }
}
