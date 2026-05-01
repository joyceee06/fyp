@file:OptIn(ExperimentalMaterial3Api::class)

package com.fyp.ekopantri.ui.inventory

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

    // --- 1. UI STATE ---
    val isEditMode = foodId != null
    var isLoading by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    val categories by viewModel.availableCategories.collectAsState()
    var selectedCategory by remember { mutableStateOf("") }

    LaunchedEffect(categories) {
        if (!isEditMode && selectedCategory.isEmpty() && categories.isNotEmpty()) {
            selectedCategory = categories[0]
        }
    }

    val storageOptions = listOf("Fridge", "Freezer", "Pantry")
    var selectedStorage by remember { mutableStateOf(storageOptions[0]) }

    val datePickerState = rememberDatePickerState()
    val reminderOptions = listOf(1, 3, 7)
    var selectedReminder by remember { mutableIntStateOf(1) }

    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // --- 2. DATA FETCHING (Edit Mode) ---
    LaunchedEffect(foodId) {
        if (isEditMode) {
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

    // --- 3. SAVE LOGIC ---
    val handleSave: () -> Unit = {
        isLoading = true

        val saveToFirestore: (String) -> Unit = { finalUrl ->
            val foodData = FoodItem(
                id = foodId ?: "",
                imageUrl = finalUrl,
                name = name,
                category = selectedCategory,
                storageLocation = selectedStorage,
                quantity = quantity,
                expiryDate = datePickerState.selectedDateMillis ?: 0L,
                reminderDays = selectedReminder,
                ownerId = auth.currentUser?.uid ?: "test_user"
            )

            val docRef = if (isEditMode) db.collection("inventory").document(foodId)
            else db.collection("inventory").document()

            docRef.set(foodData.copy(id = docRef.id)).addOnCompleteListener {
                isLoading = false
                Toast.makeText(context, if (isEditMode) "Updated!" else "Added!", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
        }

        if (imageUri != null) {
            val ref = storage.reference.child("inventory/${UUID.randomUUID()}")
            ref.putFile(imageUri!!).continueWithTask { ref.downloadUrl }
                .addOnSuccessListener { uri -> saveToFirestore(uri.toString()) }
        } else {
            saveToFirestore(existingImageUrl)
        }
    }

    // --- 4. UI LAYOUT ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (isEditMode) "Edit Item" else "Add New Item", color=MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tint = MaterialTheme.colorScheme.onPrimary, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            FoodImagePicker(imageUri, existingImageUrl) { launcher.launch("image/*") }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = quantity,
                onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    DropdownSelector("Category", selectedCategory, categories) {
                        if (it == "NEW") showNewCategoryDialog = true else selectedCategory = it
                    }
                }
                Box(Modifier.weight(1f)) {
                    DropdownSelector("Location", selectedStorage, storageOptions) { selectedStorage = it }
                }
            }

            OutlinedTextField(
                value = if (datePickerState.selectedDateMillis != null)
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(datePickerState.selectedDateMillis!!))
                else "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Expiry Date") },
                trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarToday, null) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Column {
                Text("Reminder (Days before)", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    reminderOptions.forEachIndexed { index, days ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = reminderOptions.size),
                            onClick = { selectedReminder = days },
                            selected = selectedReminder == days
                        ) { Text("${days}d") }
                    }
                }
            }

            Button(
                onClick = handleSave,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && quantity.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
                else Text(if (isEditMode) "Save Changes" else "Add Item")
            }
        }

        // --- DIALOGS ---
        if (showNewCategoryDialog) {
            var newCatName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewCategoryDialog = false },
                title = { Text("New Category") },
                text = {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newCatName.isNotBlank()) {
                            selectedCategory = newCatName.trim()
                        }
                        showNewCategoryDialog = false
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showNewCategoryDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("Done") } }
            ) { DatePicker(state = datePickerState) }
        }
    }
}

@Composable
fun FoodImagePicker(uri: Uri?, existingUrl: String, onPick: () -> Unit) {
    Card(
        onClick = onPick,
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val model = uri ?: existingUrl.ifEmpty { null }
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(32.dp))
                    Text("Add Photo", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun DropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable) // Correct for M3
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }

            // Special case for Category to allow adding new ones
            if (label == "Category") {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("New Category", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        onSelect("NEW")
                        expanded = false
                    }
                )
            }
        }
    }
}