package com.fyp.ekopantri.ui.inventory

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.ekopantri.data.InventoryRepository
import com.fyp.ekopantri.model.FoodItem
import com.fyp.ekopantri.model.HistoryItem
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


/**
 * ViewModel responsible for managing the user's food inventory.
 * It handles real-time updates from Firestore, inventory adjustments, and category management.
 */
class InventoryViewModel : ViewModel() {

    companion object {
        private const val TAG = "InventoryViewModel"
    }

    private val repository = InventoryRepository()
    
    // --- 1. LISTENERS ---
    private var inventoryListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    // --- 2. UI STATE FLOWS (RAW DATA) ---
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _foodList = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodList: StateFlow<List<FoodItem>> = _foodList.asStateFlow()

    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyList: StateFlow<List<HistoryItem>> = _historyList.asStateFlow()

    private val _scannedItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val scannedItems: StateFlow<List<FoodItem>> = _scannedItems.asStateFlow()

    // --- 3. FILTER & SEARCH STATES ---
    val searchQuery = MutableStateFlow("")
    val locationFilter = MutableStateFlow("All")
    val statusFilter = MutableStateFlow("All")
    val categoryFilter = MutableStateFlow("All")

    // --- 4. DERIVED DATA (SINGLE SOURCE OF TRUTH) ---

    /**
     * The main filtered and sorted list that the UI observes.
     * Updates automatically whenever raw data or any filter state changes.
     */
    val filteredSortedList: StateFlow<List<FoodItem>> = combine(
        _foodList, searchQuery, locationFilter, statusFilter, categoryFilter
    ) { list, query, location, status, category ->
        filterAndSortInventory(list, query, location, status, category)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Dynamically derived list of categories for filter dropdowns.
     */
    val availableCategories: StateFlow<List<String>> = _foodList
        .map { list ->
            val defaults = listOf("Vegetables", "Fruits", "Meat", "Dairy", "Grains")
            val fromDb = list.map { it.category }.filter { it.isNotBlank() }.distinct()
            (defaults + fromDb).distinct().sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("Vegetables", "Fruits", "Meat", "Dairy", "Grains")
        )

    // --- 5. LIFECYCLE & INITIALIZATION ---

    init {
        startListening()
    }

    /**
     * Sets up real-time Firestore listeners.
     */
    fun startListening() {
        stopListeners()
        resetState()

        _isLoading.value = true

        inventoryListener = repository.getInventory { items ->
            _foodList.value = items
            _isLoading.value = false
        }

        historyListener = repository.getProcessedHistory { items ->
            _historyList.value = items
        }
    }

    /**
     * Stops all listeners and clears all local data.
     */
    fun clearData() {
        stopListeners()
        resetState()
    }

    private fun stopListeners() {
        inventoryListener?.remove()
        historyListener?.remove()
    }

    private fun resetState() {
        _foodList.value = emptyList()
        _historyList.value = emptyList()
        _scannedItems.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        stopListeners()
    }

    // --- 6. USER ACTIONS (CRUD & PROCESSING) ---

    fun adjustItemQuantity(item: FoodItem, amount: Int, status: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.splitAndProcessItem(item, amount, status)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to adjust quantity for: ${item.name}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteItem(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete item: $id", e)
            }
        }
    }

    fun batchAddItems(items: List<FoodItem>) {
        viewModelScope.launch {
            try {
                items.forEach { repository.saveItem(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Batch add failed", e)
            }
        }
    }

    fun setScannedItems(items: List<FoodItem>) {
        _scannedItems.value = items
    }

    // --- 7. LOGIC HELPERS ---

    // Data class to hold expiry information for the UI.
    data class ExpiryInfo(val label: String, val color: Color)

    /**
     * Determines the visual status (Label + Color) based on expiry timestamp.
     */
    fun getInventoryExpiryStatus(timestamp: Long): ExpiryInfo {
        if (timestamp <= 0) return ExpiryInfo("Fresh", Color(0xFF10B981))
        val now = System.currentTimeMillis()
        val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
        
        return when {
            timestamp < now -> ExpiryInfo("Expired", Color(0xFFEF4444))
            timestamp - now <= threeDaysInMillis -> ExpiryInfo("Expiring Soon", Color(0xFFF59E0B))
            else -> ExpiryInfo("Fresh", Color(0xFF10B981))
        }
    }

    /**
     * Pure function to handle filtering and sorting logic.
     */
    private fun filterAndSortInventory(
        list: List<FoodItem>,
        query: String,
        location: String,
        status: String,
        category: String
    ): List<FoodItem> {
        return list.filter { item ->
            val matchesSearch = item.name.contains(query, ignoreCase = true)
            val matchesLocation = location == "All" || item.storageLocation.equals(location, ignoreCase = true)
            val matchesStatus = when (status) {
                "Fresh" -> getInventoryExpiryStatus(item.expiryDate).label == "Fresh"
                "Expiring" -> getInventoryExpiryStatus(item.expiryDate).label == "Expiring Soon"
                "Expired" -> getInventoryExpiryStatus(item.expiryDate).label == "Expired"
                else -> true
            }
            val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)

            matchesSearch && matchesLocation && matchesStatus && matchesCategory
        }.sortedWith(
            compareBy<FoodItem> {
                when (getInventoryExpiryStatus(it.expiryDate).label) {
                    "Expired" -> 0
                    "Expiring Soon" -> 1
                    else -> 2
                }
            }.thenBy { it.expiryDate }
        )
    }
}
