package com.fyp.ekopantri.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fyp.ekopantri.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = { ProfileTopBar(onBack) },
        containerColor = CreamBg
    ) { padding ->
        ProfileContent(
            modifier = Modifier.padding(padding),
            state = state,
            onFieldUpdate = { viewModel.updateField(it) },
            onUpdateProfile = {
                viewModel.updateProfile { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onChangePassword = {
                viewModel.changePassword { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun ProfileContent(
    modifier: Modifier = Modifier,
    state: ProfileUiState,
    onFieldUpdate: ((ProfileUiState) -> ProfileUiState) -> Unit,
    onUpdateProfile: () -> Unit,
    onChangePassword: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        SectionHeader(text = "Edit Profile")

        // NAME
        AccountTextField(
            value = state.name,
            onValueChange = { newValue ->
                onFieldUpdate { it.copy(name = newValue) }
            },
            label = "Full Name",
            modifier = Modifier.fillMaxWidth()
        )

        // EMAIL (Read-Only)
        AccountTextField(
            value = state.email,
            onValueChange = {}, // Disabled
            label = "Email Address (Permanent)",
            modifier = Modifier.fillMaxWidth(),
            enabled = false // This makes it greyed out and un-editable
        )

        AccountButton(
            text = "Update Profile",
            isLoading = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = onUpdateProfile,
            enabled = state.name.isNotBlank() && state.email.isNotBlank()
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color.LightGray.copy(alpha = 0.5f)
        )

        SectionHeader(text = "Change Password")

        // CURRENT PASSWORD
        AccountTextField(
            value = state.currentPassword,
            onValueChange = { newValue ->
                onFieldUpdate { it.copy(currentPassword = newValue) }
            },
            label = "Current Password",
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        // NEW PASSWORD
        AccountTextField(
            value = state.newPassword,
            onValueChange = { newValue ->
                onFieldUpdate { it.copy(newPassword = newValue) }
            },
            label = "New Password",
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        // CONFIRM PASSWORD
        AccountTextField(
            value = state.confirmPassword,
            onValueChange = { newValue ->
                onFieldUpdate { it.copy(confirmPassword = newValue) }
            },
            label = "Confirm Password",
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        AccountButton(
            text = "Change Password",
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            containerColor = DarkForest,
            onClick = onChangePassword,
            enabled = !state.isLoading && state.newPassword.length >= 6
        )
        
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Top bar for the Profile screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(text = "Profile", color = Color.White, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = Color.White,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ForestGreen)
    )
}

/**
 * A standard title for sections.
 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = DarkForest,
        modifier = Modifier.fillMaxWidth()
    )
}
