package com.fyp.ekopantri.ui.recipe

import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fyp.ekopantri.model.RecipeDetails
import com.fyp.ekopantri.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    viewModel: RecipeViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recipe Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = CreamBg
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is RecipeUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is RecipeUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is RecipeUiState.Success -> {
                    RecipeDetailContent(details = s.data)
                }
            }
        }
    }
}

// --- CONTENT COMPONENTS ---

@Composable
fun RecipeDetailContent(details: RecipeDetails) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxSize()
    ) {
        // 1. Hero Header
        RecipeDetailHero(details = details)

        Column(modifier = Modifier.padding(24.dp)) {
            // 2. About Section
            RecipeAboutSection(summary = details.summary)

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // 3. Ingredients Section
            SectionHeader("Ingredients")
            details.extendedIngredients.forEach { ingredient ->
                RecipeIngredientRow(text = ingredient.original)
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // 4. Instructions Section
            SectionHeader("Instructions")
            RecipeInstructionsList(instructions = details.instructions)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- SUB-COMPOSABLES ---

@Composable
fun RecipeDetailHero(details: RecipeDetails) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        AsyncImage(
            model = details.image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = details.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoBadge(
                    icon = Icons.Default.Timer,
                    text = "${details.readyInMinutes} mins",
                    containerColor = DarkForest.copy(alpha = 0.7f)
                )
                InfoBadge(
                    icon = Icons.Default.RestaurantMenu,
                    text = details.dishTypes?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                    containerColor = DarkForest.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun RecipeAboutSection(summary: String) {
    val summaryText = remember(summary) { cleanSummary(summary) }
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "About this recipe",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 24.sp
            )
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Show less" else "Read more", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun RecipeIngredientRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RecipeInstructionsList(instructions: String?) {
    val steps = remember(instructions) { formatInstructions(instructions) }

    steps.forEachIndexed { index, step ->
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${index + 1}.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = step,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// --- SHARED COMPONENTS ---

@Composable
fun InfoBadge(
    icon: ImageVector,
    text: String,
    containerColor: Color = DarkForest,
    contentColor: Color = Color.White,
    style: TextStyle = MaterialTheme.typography.labelSmall
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = contentColor)
            Spacer(Modifier.width(4.dp))
            Text(text = text, color = contentColor, style = style)
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary
    )
}

// --- LOGIC HELPERS ---

private fun cleanSummary(html: String): String {
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun formatInstructions(instruction: String?): List<String> {
    if (instruction.isNullOrEmpty()) return listOf("No instructions provided.")
    return instruction
        .replace(Regex("<.*?>"), "")
        .split(Regex("\\.\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}
