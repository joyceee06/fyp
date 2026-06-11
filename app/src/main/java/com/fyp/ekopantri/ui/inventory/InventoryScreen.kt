@file:OptIn(ExperimentalMaterial3Api::class)

package com.fyp.ekopantri.ui.inventory

import android.net.Uri
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fyp.ekopantri.model.FoodItem
import kotlinx.coroutines.launch
import java.io.File
import com.fyp.ekopantri.ui.theme.*

@Composable
fun InventoryScreen(
    onNavigateToAddFood: () -> Unit,
    onNavigateToEditFood: (String) -> Unit,
    onNavigateToReview: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val scannerViewModel: ScannerViewModel = viewModel()
    val foodList by viewModel.foodList.collectAsState()
    val filteredSortedList by viewModel.filteredSortedList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Filter States from ViewModel
    val search by viewModel.searchQuery.collectAsState()
    val locationFilter by viewModel.locationFilter.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()

    var actionTarget by remember { mutableStateOf<Pair<FoodItem, String>?>(null) }
    var isScanningAI by remember { mutableStateOf(false) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    // Observe categories from ViewModel
    val categories by viewModel.availableCategories.collectAsState()

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempUri?.let { uri ->
                isScanningAI = true
                scope.launch {
                    val items = scannerViewModel.scanReceipt(context, uri)
                    isScanningAI = false
                    viewModel.setScannedItems(items)
                    onNavigateToReview()
                }
            }
        }
    }

    Scaffold(
        containerColor = CreamBg,
        floatingActionButton = {
            InventoryFABs(
                onScanClick = {
                    try {
                        val tempFile = File.createTempFile("receipt_", ".jpg", context.cacheDir).apply { deleteOnExit() }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                        tempUri = uri
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        makeText(context, "Camera initialization failed", LENGTH_SHORT).show()
                    }
                },
                onAddClick = onNavigateToAddFood
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading || isScanningAI) {
                InventoryLoadingView(isScanningAI)
            } else {
                InventoryListContent(
                    items = filteredSortedList,
                    totalCount = foodList.size,
                    search = search,
                    onSearchChange = { viewModel.searchQuery.value = it },
                    locationFilter = locationFilter,
                    onLocationChange = { viewModel.locationFilter.value = it },
                    statusFilter = statusFilter,
                    onStatusChange = { viewModel.statusFilter.value = it },
                    categoryFilter = categoryFilter,
                    onCategoryChange = { viewModel.categoryFilter.value = it },
                    categories = categories,
                    onActionClick = { food, action -> actionTarget = food to action },
                    onEditClick = { onNavigateToEditFood(it) },
                    viewModel = viewModel
                )
            }

            actionTarget?.let { (item, status) ->
                InventoryActionDialog(
                    item = item,
                    status = status,
                    onDismiss = { actionTarget = null },
                    onConfirm = { qty ->
                        if (status == "delete") viewModel.deleteItem(item.id)
                        else viewModel.adjustItemQuantity(item, qty, status)
                        actionTarget = null
                    }
                )
            }
        }
    }
}

// --- PRIMARY CONTENT COMPONENT ---

@Composable
fun InventoryListContent(
    items: List<FoodItem>,
    totalCount: Int,
    search: String,
    onSearchChange: (String) -> Unit,
    locationFilter: String,
    onLocationChange: (String) -> Unit,
    statusFilter: String,
    onStatusChange: (String) -> Unit,
    categoryFilter: String,
    onCategoryChange: (String) -> Unit,
    categories: List<String>,
    onActionClick: (FoodItem, String) -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: InventoryViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { InventoryScreenHeader(count = totalCount) }

        item {
            InventoryFilterSection(
                search = search,
                onSearchChange = onSearchChange,
                locationFilter = locationFilter,
                onLocationChange = onLocationChange,
                statusFilter = statusFilter,
                onStatusChange = onStatusChange,
                categoryFilter = categoryFilter,
                onCategoryChange = onCategoryChange,
                categories = categories
            )
        }

        if (items.isEmpty()) {
            item {
                val isFiltered = search.isNotEmpty() || locationFilter != "All" || statusFilter != "All" || categoryFilter != "All"
                EmptyInventoryView(isFiltered)
            }
        } else {
            items(items, key = { it.id }) { food ->
                InventoryItemCard(
                    food = food,
                    onActionClick = { action -> onActionClick(food, action) },
                    onEditClick = { onEditClick(food.id) },
                    onDeleteClick = { onActionClick(food, "delete") },
                    viewModel = viewModel
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// --- REUSABLE SUB-COMPONENTS ---

@Composable
fun InventoryScreenHeader(count: Int) {
    Column {
        Text(text = "Food Inventory", style = MaterialTheme.typography.headlineSmall, color = DarkForest, fontWeight = FontWeight.Bold)
        Text(text = "$count items tracked", style = MaterialTheme.typography.bodySmall, color = MutedGray)
    }
}

@Composable
fun InventoryFilterSection(
    search: String,
    onSearchChange: (String) -> Unit,
    locationFilter: String,
    onLocationChange: (String) -> Unit,
    statusFilter: String,
    onStatusChange: (String) -> Unit,
    categoryFilter: String,
    onCategoryChange: (String) -> Unit,
    categories: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            placeholder = { Text("Search food...", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ForestGreen,
                unfocusedBorderColor = Color.LightGray.copy(0.5f),
                cursorColor = ForestGreen
            ),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InventoryDropdown(title = "Location", selected = locationFilter, options = listOf("All", "Fridge", "Freezer", "Pantry"), onSelected = onLocationChange, modifier = Modifier.weight(1f))
            InventoryDropdown(title = "Status", selected = statusFilter, options = listOf("All", "Fresh", "Expiring", "Expired"), onSelected = onStatusChange, modifier = Modifier.weight(1f))
            InventoryDropdown(title = "Category", selected = categoryFilter, options = listOf("All") + categories, onSelected = onCategoryChange, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun InventoryDropdown(
    title: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MutedGray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(0.5f)),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (icon != null) {
                            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MutedGray)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(text = selected, style = MaterialTheme.typography.bodyMedium, color = DarkForest, maxLines = 1)
                    }
                    Icon(Icons.Default.ArrowDropDown, null, tint = MutedGray, modifier = Modifier.size(20.dp))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(option); expanded = false })
                }
            }
        }
    }
}

@Composable
fun InventoryItemCard(
    food: FoodItem,
    onActionClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    viewModel: InventoryViewModel
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val status = viewModel.getInventoryExpiryStatus(food.expiryDate)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemThumbnail(category = food.category, imageUrl = food.imageUrl)

                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(text = food.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkForest)
                    Text(text = "${food.storageLocation} • Qty: ${food.quantity}", style = MaterialTheme.typography.bodySmall, color = MutedGray)
                    Spacer(Modifier.height(4.dp))
                    InventoryStatusBadge(status)
                }

                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = MutedGray) }

                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEditClick() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = { showMenu = false; onDeleteClick() }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = Color.Red) })
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventoryActionButton(text = "Consume", icon = Icons.Default.CheckCircle, color = ForestGreen, modifier = Modifier.weight(1f), onClick = { onActionClick("consumed") })
                    InventoryActionButton(text = "Discard", icon = null, color = Color.Red, isOutlined = true, modifier = Modifier.weight(1f), onClick = { onActionClick("discarded") })
                }
            }
        }
    }
}

@Composable
fun ItemThumbnail(category: String, imageUrl: String) {
    Box(modifier = Modifier.size(60.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            val emoji = when {
                category.contains("Fruit", true) -> "🍎"
                category.contains("Veg", true) -> "🥦"
                category.contains("Meat", true) -> "🍗"
                category.contains("Dairy", true) -> "🥛"
                else -> "📦"
            }
            Text(text = emoji, fontSize = 28.sp)
        }
    }
}

@Composable
fun InventoryStatusBadge(status: InventoryViewModel.ExpiryInfo) {
    Surface(color = status.color.copy(alpha = 0.1f), shape = RoundedCornerShape(50)) {
        Text(text = status.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = status.color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InventoryActionButton(
    text: String,
    icon: ImageVector?,
    color: Color,
    modifier: Modifier = Modifier,
    isOutlined: Boolean = false,
    onClick: () -> Unit
) {
    if (isOutlined) {
        OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, color.copy(0.5f))) {
            Text(text, color = color, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = color)) {
            if (icon != null) Icon(icon, null, modifier = Modifier.size(18.dp))
            if (icon != null) Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun InventoryFABs(onScanClick: () -> Unit, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(bottom = 16.dp, end = 8.dp), horizontalAlignment = Alignment.End) {
        FloatingActionButton(onClick = onScanClick, containerColor = ForestGreen, contentColor = Color.White, shape = CircleShape) {
            Icon(Icons.AutoMirrored.Filled.ReceiptLong, null)
        }
        Spacer(Modifier.height(12.dp))
        FloatingActionButton(onClick = onAddClick, containerColor = ForestGreen, contentColor = Color.White, shape = CircleShape){
            Icon(Icons.Default.Add, null) }
    }
}

@Composable
fun InventoryActionDialog(item: FoodItem, status: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val maxQty = item.quantity.toIntOrNull() ?: 1
    var selectedQty by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (status == "delete") "Delete Item" else "Update Inventory") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                val message = when (status) {
                    "delete" -> "Are you sure you want to delete ${item.name}?"
                    "discarded" -> "How many units of ${item.name} were discarded?"
                    "consumed" -> "How many units of ${item.name} were consumed?"
                    else -> "How many units of ${item.name} were $status?"
                }
                Text(text = message, textAlign = TextAlign.Center)
                if (status != "delete") {
                    Spacer(Modifier.height(20.dp))
                    InventoryQuantitySelector(selectedQty, maxQty, onQtyChange = { selectedQty = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedQty) },
                colors = ButtonDefaults.buttonColors(containerColor = if (status == "consumed" || status == "discarded") ForestGreen else MaterialTheme.colorScheme.error)
            ) { Text(if (status == "delete") "Delete All" else "Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MutedGray) }
        }
    )
}

@Composable
fun InventoryQuantitySelector(selectedQty: Int, maxQty: Int, onQtyChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.width(180.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(8.dp)) {
                FilledIconButton(onClick = { if (selectedQty > 1) onQtyChange(selectedQty - 1) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)) { Icon(Icons.Default.Remove, null) }
                Text(text = selectedQty.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                FilledIconButton(onClick = { if (selectedQty < maxQty) onQtyChange(selectedQty + 1) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)) { Icon(Icons.Default.Add, null) }
            }
        }
        Text(text = "Available: $maxQty", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp), color = MutedGray)
    }
}

@Composable
fun EmptyInventoryView(isFiltered: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = if (isFiltered) "🔍" else "🍽️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(text = if (isFiltered) "No items found" else "Your inventory is empty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkForest)
        Text(text = if (isFiltered) "Try adjusting your filters" else "Add some food to start tracking", style = MaterialTheme.typography.bodySmall, color = MutedGray, textAlign = TextAlign.Center)
    }
}

@Composable
fun InventoryLoadingView(isUploading: Boolean) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = ForestGreen)
        if (isUploading) {
            Spacer(Modifier.height(16.dp))
            Text("AI is reading your receipt...", color = DarkForest)
        }
    }
}
