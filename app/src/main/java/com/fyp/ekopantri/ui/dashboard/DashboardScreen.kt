package com.fyp.ekopantri.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.ekopantri.R
import com.fyp.ekopantri.ui.inventory.InventoryItemCard
import com.fyp.ekopantri.ui.inventory.InventoryViewModel
import com.fyp.ekopantri.ui.theme.*

@Composable
fun DashboardScreen(
    inventoryViewModel: InventoryViewModel,
    onNavigateToInventory: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToEducation: () -> Unit,
    onEditFood: (String) -> Unit
) {
    val foodList by inventoryViewModel.foodList.collectAsState()
    
    val freshCount = foodList.count {
        inventoryViewModel.getInventoryExpiryStatus(it.expiryDate).label == "Fresh"
    }
    val expiringCount = foodList.count {
        inventoryViewModel.getInventoryExpiryStatus(it.expiryDate).label == "Expiring Soon"
    }
    val expiredCount = foodList.count {
        inventoryViewModel.getInventoryExpiryStatus(it.expiryDate).label == "Expired"
    }
    
    val urgentItems = remember(foodList) {
        foodList.filter { 
            val label = inventoryViewModel.getInventoryExpiryStatus(it.expiryDate).label
            label == "Expired" || label == "Expiring Soon"
        }.sortedBy { it.expiryDate }.take(4)
    }

    Scaffold(
        containerColor = CreamBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Hero Section
            item {
                DashboardHero(
                    onInventoryClick = onNavigateToInventory,
                    onRecipeClick = onNavigateToRecipes
                )
            }

            // 2. Stats Grid
            item {
                DashboardStatsGrid(
                    total = foodList.size,
                    fresh = freshCount,
                    expiring = expiringCount,
                    expired = expiredCount
                )
            }

            // 3. Urgent Items
            if (urgentItems.isNotEmpty()) {
                item {
                    DashboardSectionHeader(
                        title = "⚠️ Needs Attention",
                        actionLabel = "View all",
                        onActionClick = onNavigateToInventory
                    )
                }
                items(urgentItems, key = { it.id }) { food ->
                    InventoryItemCard(
                        food = food,
                        onActionClick = { /* Actions can be added if needed */ },
                        onEditClick = { onEditFood(food.id) },
                        onDeleteClick = { inventoryViewModel.deleteItem(food.id) },
                        viewModel = inventoryViewModel
                    )
                }
            }

            // 4. Quick Actions
            item {
                DashboardQuickActions(
                    onRecipeClick = onNavigateToRecipes,
                    onEducationClick = onNavigateToEducation
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DashboardHero(onInventoryClick: () -> Unit, onRecipeClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            Image(
                painter = painterResource(id = R.drawable.home),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(DarkForest.copy(alpha = 0.9f), Color.Transparent)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Smart Food\nManagement",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )
                Text(
                    text = "Track your pantry, reduce waste and discover recipes — all in one place. 🌱",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 12.dp, bottom = 20.dp).width(200.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onInventoryClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View Inventory")
                    }
                    Button(
                        onClick = onRecipeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DarkForest),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Get Recipes")
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStatsGrid(total: Int, fresh: Int, expiring: Int, expired: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardStatCard(
                title = "Total Items",
                value = total.toString(),
                subtitle = "in your pantry",
                icon = Icons.Default.Inventory,
                color = DarkForest,
                modifier = Modifier.weight(1f)
            )
            DashboardStatCard(
                title = "Fresh",
                value = fresh.toString(),
                subtitle = "items good to go",
                icon = Icons.Default.CheckCircle,
                color = ForestGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardStatCard(
                title = "Expiring Soon",
                value = expiring.toString(),
                subtitle = "use them quickly",
                icon = Icons.Default.Warning,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            DashboardStatCard(
                title = "Expired",
                value = expired.toString(),
                subtitle = "consider removing",
                icon = Icons.Default.Error,
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = color)
            }
            Spacer(Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DarkForest)
            Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DarkForest)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MutedGray)
        }
    }
}

@Composable
fun DashboardSectionHeader(title: String, actionLabel: String, onActionClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DarkForest)
        TextButton(onClick = onActionClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = actionLabel, color = MutedGray)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MutedGray)
            }
        }
    }
}

@Composable
fun DashboardQuickActions(onRecipeClick: () -> Unit, onEducationClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DashboardActionCard(
            title = "Recipe Suggestions",
            subtitle = "Cook with what you have",
            emoji = "🍳",
            onClick = onRecipeClick,
            modifier = Modifier.weight(1f)
        )
        DashboardActionCard(
            title = "Food Tips",
            subtitle = "Learn to store smarter",
            emoji = "📚",
            onClick = onEducationClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DashboardActionCard(title: String, subtitle: String, emoji: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(ForestGreen.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = DarkForest)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MutedGray)
        }
    }
}
