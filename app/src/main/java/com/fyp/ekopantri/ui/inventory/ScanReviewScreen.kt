@file:OptIn(ExperimentalMaterial3Api::class)

package com.fyp.ekopantri.ui.inventory

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fyp.ekopantri.model.FoodItem
import java.text.SimpleDateFormat
import java.util.*
import com.fyp.ekopantri.ui.theme.*

@Composable
fun ScanReviewScreen(
    scannedItems: List<FoodItem>,
    onSaveAll: (List<FoodItem>) -> Unit,
    onCancel: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    var editableItems by remember { mutableStateOf(scannedItems) }
    val dynamicCategories by viewModel.availableCategories.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Review Scanned Items", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ForestGreen)
            )
        },
        bottomBar = {
            ReviewBottomBar(
                itemCount = editableItems.size,
                onCancel = onCancel,
                onConfirm = { onSaveAll(editableItems) }
            )
        },
        containerColor = CreamBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(editableItems) { index, item ->
                ReviewItemCard(
                    item = item,
                    categories = dynamicCategories,
                    onUpdate = { updated ->
                        editableItems = editableItems.toMutableList().apply { this[index] = updated }
                    },
                    onDelete = {
                        editableItems = editableItems.toMutableList().apply { removeAt(index) }
                    }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// --- SUB-COMPOSABLES ---

@Composable
fun ReviewBottomBar(itemCount: Int, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Surface(tonalElevation = 8.dp, color = Color.White) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(0.5f))
            ) {
                Text("Discard All", color = Color.Gray)
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Save All ($itemCount)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReviewItemCard(
    item: FoodItem,
    categories: List<String>,
    onUpdate: (FoodItem) -> Unit,
    onDelete: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var storageExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    val locations = listOf("Fridge", "Freezer", "Pantry")

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUpdate(item.copy(imageUrl = it.toString())) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(0.2f))
    ) {
        Column(Modifier.padding(16.dp)) {
            // 1. Header: Image + Name + Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReviewItemThumbnail(imageUrl = item.imageUrl) { imageLauncher.launch("image/*") }

                Spacer(Modifier.width(12.dp))

                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    label = { Text("Food Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, "Remove", tint = Color.Red)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2. Category & Quantity
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1.2f)) {
                    ReviewDropdownSelector("Category", item.category, categories, categoryExpanded, { categoryExpanded = it }) {
                        if (it == "NEW") showNewCategoryDialog = true
                        else onUpdate(item.copy(category = it))
                    }
                }

                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onUpdate(item.copy(quantity = it)) },
                    label = { Text("Qty") },
                    modifier = Modifier.weight(0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // 3. Storage & Expiry
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    ReviewDropdownSelector("Storage", item.storageLocation, locations, storageExpanded, { storageExpanded = it }) {
                        onUpdate(item.copy(storageLocation = it))
                    }
                }

                OutlinedTextField(
                    value = formatDate(item.expiryDate),
                    onValueChange = {},
                    label = { Text("Expiry") },
                    readOnly = true,
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // 4. Reminder
            ReviewReminderSelector(selected = item.reminderDays) { onUpdate(item.copy(reminderDays = it)) }
        }
    }

    // Dialogs
    if (showNewCategoryDialog) {
        NewCategoryDialog(onDismiss = { showNewCategoryDialog = false }) {
            onUpdate(item.copy(category = it))
            showNewCategoryDialog = false
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = if (item.expiryDate > 0) item.expiryDate else null)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onUpdate(item.copy(expiryDate = it)) }
                    showDatePicker = false
                }) { Text("OK", color = ForestGreen, fontWeight = FontWeight.Bold) }
            }
        ) { 
            DatePicker(
                state = datePickerState, 
                colors = DatePickerDefaults.colors(
                    todayContentColor = ForestGreen,
                    todayDateBorderColor = ForestGreen,
                    selectedDayContainerColor = ForestGreen
                )
            ) 
        }
    }
}

@Composable
fun ReviewItemThumbnail(imageUrl: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F4F6))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.PhotoCamera, null, tint = ForestGreen)
        }
    }
}

@Composable
fun ReviewDropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }, modifier = Modifier.background(Color.White)) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); onExpandedChange(false) })
            }
            if (label == "Category") {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("New Category", color = ForestGreen)
                        }
                    },
                    onClick = { onSelect("NEW"); onExpandedChange(false) }
                )
            }
        }
    }
}

@Composable
fun ReviewReminderSelector(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf(1, 3, 7)
    Column {
        Text("Notify Before Expiry", style = MaterialTheme.typography.labelSmall, color = DarkForest, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { days ->
                val isSelected = selected == days
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(days) },
                    label = { Text("$days Days") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

// --- LOGIC HELPERS ---

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
