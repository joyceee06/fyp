@file:OptIn(ExperimentalMaterial3Api::class)

package com.fyp.ekopantri.ui.inventory

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fyp.ekopantri.model.FoodItem
import com.fyp.ekopantri.ui.inventory.ScannerViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InventoryScreen(
    onNavigateToAddFood: () -> Unit,
    onNavigateToEditFood: (String) -> Unit,
    onNavigateToReview: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val scannerViewModel: ScannerViewModel = viewModel()
    val foodList by viewModel.foodList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    //  Sorting Logic
    val sortedFoodList by remember(foodList) {
        derivedStateOf {
            foodList.sortedWith(compareBy<FoodItem> { item ->
                when {
                    isExpired(item.expiryDate) -> 0
                    isNearExpiry(item.expiryDate) -> 1
                    else -> 2
                }
            }.thenBy { it.expiryDate })
        }
    }

    var actionTarget by remember { mutableStateOf<Pair<FoodItem, String>?>(null) }
    var isScanningAI by remember { mutableStateOf(false) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }

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

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        if (isLoading || isScanningAI) {
            LoadingView(isScanningAI)
        } else if (foodList.isEmpty()) {
            EmptyInventoryView()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedFoodList, key = { it.id }) { food ->
                    InventoryItemCard(
                        food = food,
                        onActionClick = { status -> actionTarget = food to status },
                        onDeleteClick = { actionTarget = food to "delete" },
                        onEditClick = { onNavigateToEditFood(food.id) }
                    )
                }
            }
        }

        InventoryActionButtons(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            onScanClick = {
                val tempFile = File.createTempFile("receipt_", ".jpg", context.cacheDir).apply { deleteOnExit() }
                tempUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                cameraLauncher.launch(tempUri!!)
            },
            onAddClick = onNavigateToAddFood
        )

        actionTarget?.let { (item, status) ->
            InventoryActionDialog(
                item = item,
                status = status,
                onDismiss = { actionTarget = null },
                onConfirm = { qty ->
                    if (status == "delete")
                        viewModel.deleteItem(item.id)
                    else
                        viewModel.adjustItemQuantity(item, qty, status)
                    actionTarget = null
                }
            )
        }
    }
}

@Composable
fun InventoryItemCard(
    food: FoodItem,
    onActionClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val alrExpired = isExpired(food.expiryDate)
    val isSoon = isNearExpiry(food.expiryDate)

    val statusColor = if (alrExpired)
        MaterialTheme.colorScheme.error
    else if (isSoon)
        Color(0xFFFF9800)
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ){
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemImage(food.imageUrl, alrExpired, isSoon)

                Column(modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)) {
                    Text(
                        food.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${food.category} • Qty: ${food.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (alrExpired) "Expired"
                            else "Exp: ${formatDate(food.expiryDate)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                alrExpired -> MaterialTheme.colorScheme.error
                                isSoon -> Color(0xFFFF9800) // orange
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { showMenu = false; onEditClick() },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDeleteClick() },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(thickness = 0.8.dp)
                    Text(
                        "Storage: ${food.storageLocation}",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onActionClick("consumed") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Consume")
                        }

                        OutlinedButton(
                            onClick = { onActionClick("discarded") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(6.dp))
                            Text("Discard", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryActionDialog(
    item: FoodItem,
    status: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val maxQty = item.quantity.toIntOrNull() ?: 1
    var selectedQty by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (status == "delete") "Delete Item" else "Update Inventory")
        },
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
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            FilledIconButton(
                                onClick = { if (selectedQty > 1) selectedQty-- },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) { Icon(Icons.Default.Remove, null) }

                            Text(
                                text = selectedQty.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            FilledIconButton(
                                onClick = { if (selectedQty < maxQty) selectedQty++ },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) { Icon(Icons.Default.Add, null) }
                        }
                    }
                    Text(
                        text = "Available: $maxQty",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedQty) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (status == "consumed"||status == "discarded") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (status == "delete") "Delete All" else "Confirm")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) { Text("Cancel") }
        }
    )
}

@Composable
fun InventoryActionButtons(
    modifier: Modifier,
    onScanClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        FloatingActionButton(
            onClick = onScanClick,
            containerColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null) }

        Spacer(Modifier.height(16.dp))

        FloatingActionButton(
            onClick = onAddClick,
            containerColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(10.dp)
        ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(32.dp)) }
    }
}

@Composable
fun ItemImage(
    imageUrl: String,
    alrExpired: Boolean,
    isSoon: Boolean
) {
    Box(modifier = Modifier.size(70.dp)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Fastfood, null, modifier = Modifier.padding(20.dp))
            }
        }
        if (alrExpired || isSoon) {
            Surface(
                color = if (alrExpired) Color.Red else Color(0xFFFF9800),
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-4).dp, y = (-4).dp),
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.White)
            ) {
                Icon(if (alrExpired) Icons.Default.PriorityHigh else Icons.Default.AccessTime, null, tint = Color.White, modifier = Modifier.padding(2.dp))
            }
        }
    }
}

@Composable
fun EmptyInventoryView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Icon(
                Icons.Default.Kitchen,
                contentDescription = null,
                modifier = Modifier.padding(30.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Your pantry is empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Let’s scan a receipt or add some goodies",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun LoadingView(isUploading: Boolean) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        if (isUploading) {
            Spacer(Modifier.height(16.dp))
            Text("AI is reading your receipt...")
        }
    }
}

fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "No Date"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}

fun isNearExpiry(timestamp: Long): Boolean {
    if (timestamp <= 0) return false
    val diff = timestamp - System.currentTimeMillis()
    return diff in 0..(3 * 24 * 60 * 60 * 1000) // Within 3 days
}
fun isExpired(timestamp: Long): Boolean = if (timestamp <= 0) false else timestamp < System.currentTimeMillis()