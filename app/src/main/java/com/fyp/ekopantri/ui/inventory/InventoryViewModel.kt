package com.fyp.ekopantri.ui.inventory

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.add
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.ekopantri.data.InventoryRepository
import com.fyp.ekopantri.model.FoodItem
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventoryViewModel : ViewModel() {

    private val repository = InventoryRepository()
    private var inventoryListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    // --- 1. UI STATE FLOWS ---

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _foodList = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodList: StateFlow<List<FoodItem>> = _foodList.asStateFlow()

    private val _historyList = MutableStateFlow<List<FoodItem>>(emptyList())

    private val _scannedItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val scannedItems = _scannedItems.asStateFlow()

    // --- 2. EXPIRY NOTIFICATIONS LOGIC ---

    val expiringSoonItems: StateFlow<List<FoodItem>> = _foodList
        .map { list ->
            val currentTime = System.currentTimeMillis()
            val millisInDay = 1000 * 60 * 60 * 24

            list.filter { item ->
                val daysRemaining = (item.expiryDate - currentTime) / millisInDay
                // Only show if it matches the user's set reminder threshold
                daysRemaining >= 0 && daysRemaining <= item.reminderDays
            }.sortedBy { it.expiryDate }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 3. CATEGORY LOGIC ---

    val availableCategories: StateFlow<List<String>> = _foodList
        .map { list ->
            val defaults = listOf("Vegetables", "Fruits", "Meat", "Dairy", "Grains")
            val fromDb = list.map { it.category }.filter { it.isNotBlank() }.distinct()
            (defaults + fromDb).distinct().sorted()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            listOf("Vegetables", "Fruits", "Meat", "Dairy", "Grains")
        )

    // --- 4. LIFECYCLE & LISTENERS ---

    init {
        startListening()
    }

    fun startListening() {
        inventoryListener?.remove()
        historyListener?.remove()
        _isLoading.value = true

        // Active Inventory Listener
        inventoryListener = repository.getInventory { items ->
            _foodList.value = items
            _isLoading.value = false
        }

        // Consumed/Discarded History Listener
        historyListener = repository.getProcessedHistory { items ->
            _historyList.value = items
        }
    }

    override fun onCleared() {
        super.onCleared()
        inventoryListener?.remove()
        historyListener?.remove()
    }

    // --- 5. INVENTORY ACTIONS (CRUD) ---

    fun adjustItemQuantity(item: FoodItem, amount: Int, status: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.splitAndProcessItem(item, amount, status)
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Failed to process item", e)
            } finally {
                _isLoading.value = false
            }
        }
    }


    // --- 6. SCANNER ACTIONS (Using Util) ---

    fun scanReceipt(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val scanner = ScannerViewModel()
            val items = scanner.scanReceipt(context, uri)
            _scannedItems.value = items // This fills your ScanReviewScreen
            _isLoading.value = false
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { repository.deleteItem(id) }
    }

    fun batchAddItems(items: List<FoodItem>) {
        viewModelScope.launch {
            items.forEach { repository.saveItem(it) }
        }
    }

    fun setScannedItems(items: List<FoodItem>) {
        _scannedItems.value = items
    }

    // --- 7. INSIGHTS & ANALYTICS ---

    private val _selectedMonth = MutableStateFlow("All Time")
    val selectedMonth = _selectedMonth.asStateFlow()

    // Generates a list of months available in the history
    val availableMonths: StateFlow<List<String>> = _historyList
        .map { list ->
            val months = list
                .filter { it.processedDate > 0 }
                .map { item ->
                    val date = Date(item.processedDate)
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
                }.distinct().sortedDescending()

            listOf("All Time") + months
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All Time"))

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    // Handles the filtering logic whenever the list or month changes
    private val filteredHistory = combine(_historyList, _selectedMonth) { list, month ->
        if (month == "All Time") {
            list
        } else {
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            list.filter { item ->
                val itemMonth = formatter.format(Date(item.processedDate))
                itemMonth == month
            }
        }
    }
    // Consumed vs Discarded
    val wasteVsConsumption: StateFlow<Map<String, Int>> = filteredHistory
        .map { list ->
            list.groupBy { it.status }
                .mapValues { entry -> entry.value.sumOf { it.quantity.toIntOrNull() ?: 0 } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Consumed by Category
    val consumedByCategory: StateFlow<Map<String, Int>> = filteredHistory
        .map { list ->
            list.filter { it.status == "consumed" }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.quantity.toIntOrNull() ?: 0 } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Discarded by Category
    val discardedByCategory: StateFlow<Map<String, Int>> = filteredHistory
        .map { list ->
            list.filter { it.status == "discarded" }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.quantity.toIntOrNull() ?: 0 } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

}
