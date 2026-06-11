package com.fyp.ekopantri.ui.education

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.ekopantri.model.EducationItem
import com.fyp.ekopantri.ui.theme.DarkForest
import com.fyp.ekopantri.ui.theme.ForestGreen
import com.fyp.ekopantri.ui.theme.AiPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationDetailScreen(
    viewModel: EducationViewModel,
    onBack: () -> Unit,
    documentId: String
) {
    LaunchedEffect(documentId) {
        viewModel.loadContent(documentId)
    }

    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            EducationDetailTopBar(
                onBack = onBack
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            when (val result = state) {
                is EducationUiState.Loading -> {
                    EducationDetailLoading()
                }
                is EducationUiState.Error -> {
                    EducationDetailError(result.message)
                }
                is EducationUiState.Success -> {
                    EducationDetailContent(item = result.data)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationDetailTopBar(
    onBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text("Education Detail", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = ForestGreen,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

// --- 1. CORE CONTENT COMPONENT ---
@Composable
fun EducationDetailContent(item: EducationItem) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // Header
        EducationDetailHeader(emoji = item.emoji, title = item.title, category = item.category)

        Spacer(modifier = Modifier.height(16.dp))

        // Status Badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoBadge(
                icon = Icons.Default.AutoAwesome,
                text = "AI Generated Guide",
                containerColor = AiPurple.copy(alpha = 0.1f),
                contentColor = AiPurple
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Content Box
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(20.dp),
                lineHeight = 28.sp,
                color = DarkForest
            )
        }

        // Checklist Section
        if (item.baseTips.isNotEmpty()) {
            EducationStorageChecklist(tips = item.baseTips)
        }
    }
}

// --- 2. SUB-COMPOSABLES ---
@Composable
fun EducationDetailHeader(emoji: String, title: String, category: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 40.sp, lineHeight = 40.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                color = DarkForest
            )
        }
        Text(
            text = category,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun InfoBadge(
    icon: ImageVector,
    text: String,
    containerColor: Color = ForestGreen.copy(alpha = 0.1f),
    contentColor: Color = ForestGreen,
    style: TextStyle = MaterialTheme.typography.labelSmall
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = contentColor)
            Spacer(Modifier.width(6.dp))
            Text(text = text, color = contentColor, style = style, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EducationStorageChecklist(tips: List<String>) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Storage Checklist",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = DarkForest
    )
    Spacer(modifier = Modifier.height(12.dp))

    tips.forEach { tip ->
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text("✅ ", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = DarkForest
            )
        }
    }
}

@Composable
fun EducationDetailLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ForestGreen)
    }
}

@Composable
fun EducationDetailError(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
    }
}
