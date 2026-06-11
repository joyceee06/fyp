@file:OptIn(ExperimentalMaterial3Api::class)

package com.fyp.ekopantri.ui.inventory

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fyp.ekopantri.model.FoodItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*
import com.fyp.ekopantri.ui.theme.*

@Composable
fun AddFoodScreen(
    foodId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val auth = FirebaseAuth.getInstance()

    // 1. Logic States
    val isEditMode = foodId != null
    var isLoading by remember { mutableStateOf(false) }

    // 2. Form States
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    val categories by viewModel.availableCategories.collectAsState()
    var selectedCategory by remember { mutableStateOf("") }
    var selectedStorage by remember { mutableStateOf("Fridge") }
    var selectedReminder by remember { mutableIntStateOf(1) }
    val datePickerState = rememberDatePickerState()

    // 3. UI Control States
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 4. Initialization & Fetching
    LaunchedEffect(categories) {
        if (!isEditMode && selectedCategory.isEmpty() && categories.isNotEmpty()) {
            selectedCategory = categories[0]
        }
    }

    LaunchedEffect(foodId) {
        if (isEditMode && foodId != null) {
            db.collection("inventory").document(foodId).get().addOnSuccessListener { doc ->
                doc.toObject(FoodItem::class.java)?.let { item ->
                    name = item.name
                    quantity = item.quantity
                    selectedCategory = item.category
                    selectedStorage = item.storageLocation
                    existingImageUrl = item.imageUrl
                    selectedReminder = item.reminderDays
                    datePickerState.selectedDateMillis = item.expiryDate
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }

    // 5. Save Logic
    val onSave: () -> Unit = {
        isLoading = true
        val finalSave: (String) -> Unit = { url ->
            val foodData = FoodItem(
                id = foodId ?: "",
                imageUrl = url,
                name = name,
                category = selectedCategory,
                storageLocation = selectedStorage,
                quantity = quantity,
                expiryDate = datePickerState.selectedDateMillis ?: 0L,
                reminderDays = selectedReminder,
                ownerId = auth.currentUser?.uid ?: "test_user"
            )
            val docRef =
                if (isEditMode && foodId != null)
                    db.collection("inventory").document(foodId)
                else
                    db.collection("inventory").document()
            docRef.set(foodData.copy(id = docRef.id)).addOnCompleteListener {
                isLoading = false
                Toast.makeText(context, if (isEditMode) "Updated!" else "Added!", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
        }

        if (imageUri != null) {
            val ref = storage.reference.child("inventory_images/${UUID.randomUUID()}.jpg")
            ref.putFile(imageUri!!).continueWithTask { ref.downloadUrl }
                .addOnSuccessListener { uri -> finalSave(uri.toString()) }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                }
        } else {
            finalSave(existingImageUrl)
        }
    }

    Scaffold(
        topBar = {
            AddFoodTopBar(
                isEditMode = isEditMode,
                onBack = onNavigateBack
            )
        },
        containerColor = CreamBg
    ) { padding ->
        AddFoodContent(
            modifier = Modifier.padding(padding),
            name = name,
            onNameChange = { name = it },
            quantity = quantity,
            onQuantityChange = { if (it.all { c -> c.isDigit() }) quantity = it },
            selectedCategory = selectedCategory,
            categories = categories,
            onCategorySelect = { if (it == "NEW") showNewCategoryDialog = true else selectedCategory = it },
            selectedStorage = selectedStorage,
            onStorageSelect = { selectedStorage = it },
            expiryDateMillis = datePickerState.selectedDateMillis,
            onDatePickerClick = { showDatePicker = true },
            selectedReminder = selectedReminder,
            onReminderChange = { selectedReminder = it },
            imageUri = imageUri,
            existingImageUrl = existingImageUrl,
            onImageClick = { launcher.launch("image/*") },
            isSaving = isLoading,
            onSaveClick = onSave,
            isEditMode = isEditMode
        )

        // Dialogs
        if (showNewCategoryDialog) {
            NewCategoryDialog(
                onDismiss = { showNewCategoryDialog = false },
                onConfirm = { 
                    selectedCategory = it
                    showNewCategoryDialog = false
                }
            )
        }

        if (showDatePicker) {
            FoodDatePickerDialog(
                state = datePickerState,
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

// --- PRIMARY CONTENT COMPONENT ---

@Composable
fun AddFoodContent(
    modifier: Modifier = Modifier,
    name: String,
    onNameChange: (String) -> Unit,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    selectedCategory: String,
    categories: List<String>,
    onCategorySelect: (String) -> Unit,
    selectedStorage: String,
    onStorageSelect: (String) -> Unit,
    expiryDateMillis: Long?,
    onDatePickerClick: () -> Unit,
    selectedReminder: Int,
    onReminderChange: (Int) -> Unit,
    imageUri: Uri?,
    existingImageUrl: String,
    onImageClick: () -> Unit,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    isEditMode: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        FoodImageCard(uri = imageUri, existingUrl = existingImageUrl, onClick = onImageClick)

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Food Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Quantity") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                FormDropdownSelector("Category", selectedCategory, categories, true, onCategorySelect)
            }
            Box(Modifier.weight(1f)) {
                val storageOptions = listOf("Fridge", "Freezer", "Pantry")
                FormDropdownSelector("Location", selectedStorage, storageOptions, false, onStorageSelect)
            }
        }

        OutlinedTextField(
            value = formatDate(expiryDateMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text("Expiry Date") },
            trailingIcon = { IconButton(onClick = onDatePickerClick) { Icon(Icons.Default.CalendarToday, null) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ReminderSelector(selectedReminder = selectedReminder, onReminderChange = onReminderChange)

        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = name.isNotBlank() && quantity.isNotEmpty() && !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(if (isEditMode) "Save Changes" else "Add Item", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// --- SUB-COMPOSABLES ---

@Composable
fun AddFoodTopBar(isEditMode: Boolean, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(if (isEditMode) "Edit Item" else "Add New Item", color = Color.White, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, tint = Color.White, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ForestGreen)
    )
}

@Composable
fun FoodImageCard(uri: Uri?, existingUrl: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(0.2f))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val model = uri ?: existingUrl.ifEmpty { null }
            if (model != null) {
                AsyncImage(model = model, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(40.dp), tint = ForestGreen)
                    Spacer(Modifier.height(8.dp))
                    Text("Add Food Photo", style = MaterialTheme.typography.labelLarge, color = ForestGreen)
                }
            }
        }
    }
}

@Composable
fun FormDropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    allowCustom: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
            if (allowCustom) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("New Category", color = ForestGreen)
                        }
                    },
                    onClick = { onSelect("NEW"); expanded = false }
                )
            }
        }
    }
}

@Composable
fun ReminderSelector(selectedReminder: Int, onReminderChange: (Int) -> Unit) {
    val options = listOf(1, 3, 7)
    Column {
        Text("Reminder (Days before expiry)", style = MaterialTheme.typography.labelMedium, color = DarkForest, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, days ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { onReminderChange(days) },
                    selected = selectedReminder == days,
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = ForestGreen, activeContentColor = Color.White)
                ) { Text("${days}d") }
            }
        }
    }
}

@Composable
fun NewCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newCatName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            OutlinedTextField(
                value = newCatName,
                onValueChange = { newCatName = it },
                label = { Text("Category Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (newCatName.isNotBlank()) onConfirm(newCatName.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}

@Composable
fun FoodDatePickerDialog(state: DatePickerState, onDismiss: () -> Unit) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = ForestGreen, fontWeight = FontWeight.Bold) } }
    ) { 
        DatePicker(
            state = state, 
            colors = DatePickerDefaults.colors(
                todayContentColor = ForestGreen,
                todayDateBorderColor = ForestGreen,
                selectedDayContainerColor = ForestGreen
            )
        ) 
    }
}

// --- LOGIC HELPERS ---

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return ""
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
