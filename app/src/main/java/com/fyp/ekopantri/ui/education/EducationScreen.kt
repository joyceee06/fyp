package com.fyp.ekopantri.ui.education

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fyp.ekopantri.model.EducationItem

// --- Constants & Colors ---
val ForestGreen = Color(0xFF2E7D5B)
val CreamBg = Color(0xFFFAF7F0)
val DarkForest = Color(0xFF1B2E22)
val MutedGray = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationScreen(
    navController: NavController,
    inventoryCategories: List<String> = emptyList(),
    viewModel: EducationViewModel
) {
    val articles by viewModel.articles.collectAsState()

    // UI States
    var showAiSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchAllArticles()
    }

    // Logic: Filter & Recommended (Wrapped in remember to avoid redundant calculation)
    val (recommendedArticles, filteredArticles) = remember(articles, inventoryCategories, selectedCategory, searchQuery) {
        val rec = articles.filter { article ->
            inventoryCategories.any { category ->
                article.title.contains(category, true) || article.content.contains(category, true)
            }
        }
        val filt = articles
            .filter { it !in rec }
            .filter { article ->
                val matchesCategory = (selectedCategory == "All" || article.category.equals(selectedCategory, true))
                val matchesSearch = article.title.contains(searchQuery, true) || article.content.contains(searchQuery, true)
                matchesCategory && matchesSearch
            }
        Pair(rec, filt)
    }

    Scaffold(
        containerColor = CreamBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAiSheet = true },
                containerColor = ForestGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AutoAwesome, "AI icon") },
                text = { Text("Ask AI assistant") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                HeaderSection()

                SearchSection(searchQuery) { searchQuery = it }

                CategoryFilters(selectedCategory) { selectedCategory = it }

                // 1. Recommended Section
                if (recommendedArticles.isNotEmpty() && searchQuery.isEmpty()) {
                    SectionTitle("Recommended For You")
                    recommendedArticles.forEach { FirebaseArticleCard(it, navController) }
                }

                // 2. All Articles Section
                SectionTitle(if (selectedCategory == "All") "All Articles" else "$selectedCategory Articles")

                if (filteredArticles.isEmpty()) {
                    EmptyText(if (articles.isEmpty()) "Loading articles..." else "No articles found")
                } else {
                    filteredArticles.forEach { FirebaseArticleCard(it, navController) }
                }

                SDGBanner()
                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }

            // --- Floating Assistant Bottom Sheet ---
            if (showAiSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAiSheet = false },
                    sheetState = sheetState,
                    containerColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = ForestGreen) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SmartAssistantSection(
                            viewModel = viewModel,
                            inventoryCategories = inventoryCategories,
                            onClose = { showAiSheet = false }
                        )
                    }
                }
            }
        }
    }
}

// --- UI Sub-Sections ---

@Composable
fun HeaderSection() {
    Column {
        Surface(color = ForestGreen.copy(alpha = 0.1f), shape = CircleShape) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Book, null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                Text(" Learn Center", color = ForestGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = "Food Education", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif), color = DarkForest, fontWeight = FontWeight.Bold)
        Text("Expert-curated knowledge to help you store smarter.", color = MutedGray, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SearchSection(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Search storage tips...") },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Search, null) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen, cursorColor = ForestGreen)
    )
}

@Composable
fun CategoryFilters(selected: String, onCategorySelected: (String) -> Unit) {
    val categories = listOf("All", "Storage", "Preservation", "Awareness")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories) { cat ->
            val isSelected = selected == cat
            Surface(
                modifier = Modifier.clickable { onCategorySelected(cat) },
                color = if (isSelected) ForestGreen else Color.White,
                shape = CircleShape,
                border = BorderStroke(1.dp, if (isSelected) ForestGreen else Color.LightGray)
            ) {
                Text(
                    text = cat,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isSelected) Color.White else MutedGray,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun SmartAssistantSection(viewModel: EducationViewModel, inventoryCategories: List<String>, onClose: () -> Unit) {
    var userQuestion by remember { mutableStateOf("") }
    val answer by viewModel.aiAnswer.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()

    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("EkoPantri AI Assistant", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ForestGreen)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
        }
        Text("Ask anything about food storage or waste reduction.", style = MaterialTheme.typography.bodyMedium, color = MutedGray)

        Spacer(Modifier.height(16.dp))

        if (inventoryCategories.isNotEmpty()) {
            Text("Suggested:", style = MaterialTheme.typography.labelMedium, color = ForestGreen)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                items(inventoryCategories) { category ->
                    SuggestionChip(onClick = { viewModel.askAiAboutFood("How to store $category?") }, label = { Text(category) })
                }
            }
        }

        OutlinedTextField(
            value = userQuestion,
            onValueChange = { userQuestion = it },
            placeholder = { Text("How do I keep chicken fresh?") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = { viewModel.askAiAboutFood(userQuestion) },
            enabled = userQuestion.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            else Text("Get AI Tip")
        }

        answer?.let { response ->
            Surface(
                Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth(),
                color = ForestGreen.copy(0.05f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ForestGreen.copy(0.1f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    response.split("\n").forEach { line ->
                        Text(
                            text = if (line.trim().startsWith("-")) "• ${line.substring(1).trim()}" else line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkForest,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Reusable UI Components ---

@Composable
fun FirebaseArticleCard(item: EducationItem, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("article_detail/${item.id}") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(70.dp)
                .background(ForestGreen.copy(0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(text = item.emoji.ifBlank { "📖" }, fontSize = 36.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Surface(color = ForestGreen.copy(0.1f), shape = CircleShape) {
                    Text(
                        text=item.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = ForestGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(item.content, color = MutedGray, style = MaterialTheme.typography.bodySmall, maxLines = 2, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun SDGBanner() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ForestGreen)) {
        Box(modifier = Modifier.background(Brush.linearGradient(listOf(ForestGreen, Color(0xFF1B5E20))))) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(60.dp), shape = RoundedCornerShape(12.dp), color = Color.White.copy(0.2f)) {
                    Text("🌍", fontSize = 32.sp, modifier = Modifier.wrapContentSize())
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("UNITED NATIONS · SDG 12", color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelSmall)
                    Text("Responsible Consumption", color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif))
                    Text("Ensure sustainable production and consumption patterns.", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DarkForest)
@Composable fun EmptyText(text: String) = Text(text, color = MutedGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)