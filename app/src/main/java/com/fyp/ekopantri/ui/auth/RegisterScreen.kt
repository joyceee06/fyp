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
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // 1. Logic States
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // 2. Form States
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

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
                title = "Create Account",
                subtitle = "Start Reducing Food Waste Today"
            )

            Spacer(Modifier.height(32.dp))

            AccountTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                modifier = Modifier.width(320.dp)
            )

            Spacer(Modifier.height(12.dp))

            AccountTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
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

            Spacer(Modifier.height(12.dp))

            AccountTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                modifier = Modifier.width(320.dp),
                isPassword = true
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            AccountButton(
                text = "Register",
                isLoading = isLoading,
                modifier = Modifier.width(320.dp),
                onClick = {
                    if (password != confirmPassword) {
                        error = "Passwords do not match"
                    } else {
                        isLoading = true
                        viewModel.register(name, email, password) { success, errorMessage ->
                            isLoading = false
                            if (success) {
                                onRegisterSuccess()
                            } else {
                                error = if (errorMessage?.contains("credential not available", true) == true) {
                                    "Google Play Services error. Please check your account or try again."
                                } else {
                                    errorMessage ?: "Registration failed"
                                }
                            }
                        }
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank() && 
                         password.isNotBlank() && confirmPassword.isNotBlank() && !isLoading
            )

            Spacer(Modifier.height(16.dp))

            AccountFooter(
                mainText = "Already have an account?",
                actionText = "Login",
                onActionClick = onNavigateToLogin
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
