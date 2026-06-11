@file:OptIn(ExperimentalMaterial3Api::class)

package com.fyp.ekopantri.ui.recipe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fyp.ekopantri.model.RecipeSearchItem
import com.fyp.ekopantri.ui.inventory.InventoryViewModel
import com.fyp.ekopantri.ui.theme.*

@Composable
fun RecipeScreen(
    navController: NavController,
    inventoryViewModel: InventoryViewModel,
    recipeViewModel: RecipeViewModel
) {
    val pantryItems by inventoryViewModel.foodList.collectAsState()
    val recipes by recipeViewModel.recipes.collectAsState()
    val isLoading by recipeViewModel.isLoading.collectAsState()

    // 1. Initial Generation Logic
    LaunchedEffect(pantryItems) {
        if (pantryItems.isNotEmpty() && recipes.isEmpty()) {
            recipeViewModel.generateRecipes(pantryItems)
        }
    }

    Scaffold(
        containerColor = CreamBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = {
                    recipeViewModel.generateRecipes(pantryItems)
                },
                modifier = Modifier.fillMaxSize()
            ) {
                RecipeListContent(
                    pantryEmpty = pantryItems.isEmpty(),
                    recipes = recipes,
                    isLoading = isLoading,
                    onRecipeClick = { recipe ->
                        recipeViewModel.fetchRecipeDetails(recipe.id) {
                            navController.navigate("recipe_detail")
                        }
                    },
                    onRetryClick = {
                        recipeViewModel.generateRecipes(pantryItems)
                    }
                )
            }
        }
    }
}

// --- PRIMARY CONTENT COMPONENT ---

@Composable
fun RecipeListContent(
    pantryEmpty: Boolean,
    recipes: List<RecipeSearchItem>,
    isLoading: Boolean,
    onRecipeClick: (RecipeSearchItem) -> Unit,
    onRetryClick: () -> Unit
) {
    when {
        pantryEmpty -> {
            EmptyRecipeView(
                message = "Your pantry is empty",
                subMessage = "Add food to your inventory to see recipe suggestions!",
                showButton = false
            )
        }

        recipes.isEmpty() && !isLoading -> {
            EmptyRecipeView(
                message = "No recipes found",
                subMessage = "Try adding more common ingredients or pull down to refresh.",
                showButton = true,
                onButtonClick = onRetryClick
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column {
                        Text(text = "Smart Recipes", style = MaterialTheme.typography.headlineSmall, color = DarkForest, fontWeight = FontWeight.Bold)
                        Text(text = "Based on what you have in stock", style = MaterialTheme.typography.bodySmall, color = MutedGray)
                    }
                }

                items(recipes, key = { it.id }) { recipe ->
                    RecipeItemCard(recipe = recipe, onClick = { onRecipeClick(recipe) })
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// --- SUB-COMPOSABLES ---

@Composable
fun RecipeItemCard(recipe: RecipeSearchItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            AsyncImage(
                model = recipe.image,
                contentDescription = recipe.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = recipe.title,
                    color = DarkForest,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (recipe.readyInMinutes > 0) {
                        RecipeInfoChip(
                            text = "⏱ ${recipe.readyInMinutes} min",
                            containerColor = ForestGreen.copy(alpha = 0.1f),
                            contentColor = ForestGreen
                        )
                    }

                    recipe.dishTypes?.firstOrNull()?.let { type ->
                        RecipeInfoChip(
                            text = type.replaceFirstChar { it.uppercase() },
                            containerColor = ForestGreen.copy(alpha = 0.1f),
                            contentColor = ForestGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyRecipeView(
    message: String,
    subMessage: String,
    showButton: Boolean = false,
    onButtonClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🍳", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DarkForest,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subMessage,
            textAlign = TextAlign.Center,
            color = MutedGray,
            style = MaterialTheme.typography.bodyMedium
        )
        if (showButton) {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Try Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RecipeInfoChip(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
