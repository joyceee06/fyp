package com.fyp.ekopantri.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    val viewModel: AuthViewModel = viewModel()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Login",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight =FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        val fieldModifier = Modifier.width(300.dp)

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = fieldModifier,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = fieldModifier,
            shape = MaterialTheme.shapes.medium
        )

        if (errorMessage != null) {
            Text(
                errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.login(email, password) { success, message ->
                    if (success) {
                        onLoginSuccess()
                    } else {
                        errorMessage = mapFirebaseError(message)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = fieldModifier.height(50.dp),
            shape = MaterialTheme.shapes.large
        ) { Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onNavigateToRegister) {
                Text(
                    "Register here",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun mapFirebaseError(message: String?): String {
    if (message == null) return "An unknown error occurred"

    return when {
        message.contains("password", ignoreCase = true) -> "The password is invalid"
        message.contains("no user record", ignoreCase = true) -> "There is no user record"
        message.contains("identifier", ignoreCase = true) -> "There is no user record"
        message.contains("timeout", ignoreCase = true) || message.contains("network", ignoreCase = true) -> "Network timeout"
        message.contains("email address is badly formatted", ignoreCase = true) -> "Invalid email format"
        else -> message
    }
}

