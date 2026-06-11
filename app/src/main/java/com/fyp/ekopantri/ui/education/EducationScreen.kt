package com.fyp.ekopantri.ui.education

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fyp.ekopantri.model.EducationItem
import com.fyp.ekopantri.model.FoodItem
import com.fyp.ekopantri.ui.theme.*

/**
 * Main screen for the Education module.
 * Displays AI-generated storage tips and provides an AI assistant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationScreen(
    navController: NavController,
    inventoryItems: List<FoodItem> = emptyList(),
    viewModel: EducationViewModel
) {
    // 1. STATE OBSERVATION
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val articles by viewModel.filteredContent.collectAsState()
    val isArticlesLoading by viewModel.isArticlesLoading.collectAsState()

    // 2. LIFECYCLE
    LaunchedEffect(inventoryItems) {
        viewModel.updateInventoryItems(inventoryItems)
    }

    // 3. UI STATE (Bottom Sheet)
    var showAiSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        containerColor = CreamBg,
        floatingActionButton = {
            EducationAiFab(onClick = { showAiSheet = true })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Main List
            EducationListContent(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.searchQuery.value = it },
                selectedCategory = selectedCategory,
                onCategoryChange = { viewModel.selectedCategory.value = it },
                articles = articles,
                isArticlesLoading = isArticlesLoading,
                navController = navController
            )

            // AI Assistant Sheet
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
                        EducationSmartAssistant(
                            viewModel = viewModel,
                            inventoryItemNames = inventoryItems.map { it.name }.distinct(),
                            onClose = { showAiSheet = false }
                        )
                    }
                }
            }
        }
    }
}

// --- 1. CORE LIST COMPONENTS ---

@Composable
fun EducationListContent(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    articles: List<EducationItem>,
    isArticlesLoading: Boolean,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { EducationHeader() }

        item { EducationSearchSection(query = searchQuery, onSearchChange = onSearchChange) }

        item { EducationCategoryFilters(selected = selectedCategory, onCategorySelected = onCategoryChange) }

        // Articles Section
        item { 
            SectionHeader(
                text = if (selectedCategory == "All") "All Articles" else "$selectedCategory Articles"
            ) 
        }

        if (isArticlesLoading && articles.isEmpty()) {
            item { EducationLoadingSpinner() }
        } else if (articles.isEmpty()) {
            item { EducationEmptyText("No articles found") }
        } else {
            items(articles) { article ->
                EducationArticleCard(article, navController)
            }
        }

        item {
            EducationSDGBanner()
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// --- 2. SUB-COMPOSABLES (CARDS & HEADERS) ---

@Composable
fun EducationHeader() {
    Column {
        Surface(color = ForestGreen.copy(alpha = 0.1f), shape = CircleShape) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Book, null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                Text(" Learn Center", color = ForestGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = "Pantry Guide", style = MaterialTheme.typography.headlineSmall, color = DarkForest, fontWeight = FontWeight.Bold)
        Text(text = "Learn simple ways to organize and manage your pantry.", color = MutedGray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun EducationArticleCard(item: EducationItem, navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("article_detail/${item.id}") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(70.dp).background(ForestGreen.copy(0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(text = item.emoji.ifBlank { "📖" }, fontSize = 36.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Surface(color = ForestGreen.copy(0.1f), shape = CircleShape) {
                    Text(text = item.category.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = ForestGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(item.content, color = MutedGray, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

// --- 3. UI UTILITIES (SEARCH, FILTERS, FAB) ---

@Composable
fun EducationSearchSection(query: String, onSearchChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onSearchChange,
        placeholder = { Text("Search storage tips...", style = MaterialTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        leadingIcon = { Icon(Icons.Default.Search, null) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ForestGreen,
            unfocusedBorderColor = Color.LightGray.copy(0.5f),
            cursorColor = ForestGreen
        ),
        singleLine = true
    )
}

@Composable
fun EducationCategoryFilters(selected: String, onCategorySelected: (String) -> Unit) {
    val categories = listOf("All", "Storage", "Preservation", "Awareness")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories) { cat ->
            val isSelected = selected == cat
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(cat) },
                label = { Text(text = cat) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (isSelected) ForestGreen else Color.White,
                    labelColor = if (isSelected) Color.White else MutedGray
                )
            )
        }
    }
}

@Composable
fun EducationAiFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Default.AutoAwesome, "AI icon") },
        text = { Text("Ask AI assistant") }
    )
}

// --- 4. AI ASSISTANT COMPONENTS ---

@Composable
fun EducationSmartAssistant(viewModel: EducationViewModel, inventoryItemNames: List<String>, onClose: () -> Unit) {
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

        if (inventoryItemNames.isNotEmpty()) {
            Text("Suggested:", style = MaterialTheme.typography.labelMedium, color = ForestGreen)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                items(inventoryItemNames) { itemName ->
                    SuggestionChip(onClick = { viewModel.askAiAboutFood("How to store $itemName?") }, label = { Text(itemName, style = MaterialTheme.typography.labelMedium) })
                }
            }
        }

        OutlinedTextField(
            value = userQuestion,
            onValueChange = { userQuestion = it },
            placeholder = { Text("How do I keep chicken fresh?", style = MaterialTheme.typography.labelLarge) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = { viewModel.askAiAboutFood(userQuestion) },
            enabled = userQuestion.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            else Text("Get AI Tip")
        }

        answer?.let { EducationAiResponseBox(it) }
    }
}

@Composable
fun EducationAiResponseBox(response: String) {
    Surface(
        Modifier.padding(top = 20.dp).fillMaxWidth(),
        color = ForestGreen.copy(0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ForestGreen.copy(0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("EkoPantri Advice", style = MaterialTheme.typography.labelLarge, color = ForestGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            response.split("\n").filter { it.isNotBlank() }.forEach { line ->
                EducationAiResponseLine(line)
            }
        }
    }
}

@Composable
fun EducationAiResponseLine(line: String) {
    val trimmed = line.trim()
    val isBullet = trimmed.startsWith("-") || trimmed.startsWith("*") || (trimmed.firstOrNull()?.isDigit() == true && trimmed.contains("."))
    
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        if (isBullet) {
            Text("• ", style = MaterialTheme.typography.bodyLarge, color = ForestGreen, fontWeight = FontWeight.Bold)
            
            val rawContent = when {
                trimmed.startsWith("-") || trimmed.startsWith("*")
                    -> trimmed.substring(1).trim()
                trimmed.firstOrNull()?.isDigit() == true && trimmed.contains(".")
                    -> trimmed.substringAfter(".").trim()
                else -> trimmed
            }

            val annotatedContent = buildAnnotatedString {
                if (rawContent.contains(":")) {
                    val title = rawContent.substringBefore(":").trim()
                    val description = rawContent.substringAfter(":").trim()
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = DarkForest)) {
                        append("$title: ")
                    }
                    append(description)
                } else {
                    append(rawContent)
                }
            }
            Text(text = annotatedContent, style = MaterialTheme.typography.bodyMedium, color = DarkForest)
        } else {
            Text(
                text = trimmed,
                style = if (trimmed.endsWith(":") || trimmed.length < 50) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
                color = DarkForest,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- 5. GLOBAL UTILITIES & BANNERS ---

@Composable
fun EducationSDGBanner() {
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

@Composable
fun SectionHeader(text: String, color: Color = DarkForest) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
}

@Composable 
fun EducationEmptyText(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = MutedGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

@Composable
fun EducationLoadingSpinner() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ForestGreen)
    }
}
