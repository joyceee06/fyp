package com.fyp.ekopantri.ui.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.ekopantri.data.InventoryRepository
import com.fyp.ekopantri.model.HistoryItem
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InsightViewModel : ViewModel() {

    private val repository = InventoryRepository()
    private var historyListener: ListenerRegistration? = null

    // 1. Raw Data State
    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())

    // 2. Filter States
    enum class TimeRange(val displayName: String) {
        MONTH("Month"),
        YEAR("Year"),
        ALL_TIME("All Time")
    }

    private val _filterType = MutableStateFlow(TimeRange.ALL_TIME)
    val filterType = _filterType.asStateFlow()

    private val _filterValue = MutableStateFlow("All Time")
    val filterValue = _filterValue.asStateFlow()

    init {
        startListening()
    }

    fun startListening() {
        historyListener?.remove()
        // IMPORTANT: Clear old data immediately to prevent seeing the previous user's data
        _historyList.value = emptyList()

        historyListener = repository.getProcessedHistory { items ->
            _historyList.value = items
        }
    }

    // 3. Dynamic Options for the UI (Chips/Dropdowns)
    // This generates the list of months or years based on the history data
    val filterOptions: StateFlow<List<String>> = combine(_historyList, _filterType) { list, type ->
        when (type) {
            TimeRange.ALL_TIME -> listOf("All Time")
            TimeRange.YEAR -> {
                list.filter { it.processedDate > 0 }
                    .map { SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(it.processedDate)) }
                    .distinct().sortedDescending()
            }
            TimeRange.MONTH -> {
                list.filter { it.processedDate > 0 }
                    .map { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(it.processedDate)) }
                    .distinct().sortedDescending()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All Time"))

    // 4. Setters for the UI
    fun onFilterTypeChange(type: TimeRange) {
        _filterType.value = type
        // Reset value to the first available option when type changes
        _filterValue.value = if (type == TimeRange.ALL_TIME) "All Time" else "Select"
    }

    fun onFilterValueChange(value: String) {
        _filterValue.value = value
    }

    // 5. THE SINGLE SOURCE OF TRUTH: Filtered History
    private val filteredHistory = combine(_historyList, _filterType, _filterValue) {
        list, type, value
        -> if (value == "Select" || type == TimeRange.ALL_TIME) return@combine list

        when (type) {
            TimeRange.YEAR -> {
                val formatter = SimpleDateFormat("yyyy", Locale.getDefault())
                list.filter { formatter.format(Date(it.processedDate)) == value }
            }
            TimeRange.MONTH -> {
                val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                list.filter { formatter.format(Date(it.processedDate)) == value }
            }
            else -> list
        }
    }

    // 6. Data for Charts
    val wasteVsConsumption: StateFlow<Map<String, Int>> = filteredHistory
        .map { list ->
            list.groupBy { it.status }
                .mapValues { entry -> entry.value.sumOf { it.quantity.toIntOrNull() ?: 0 } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val consumedByItem: StateFlow<Map<String, Int>> = filteredHistory
        .map { list ->
            list.filter { it.status == "consumed" }
                .groupBy { it.name }
                .mapValues { entry -> entry.value.sumOf { it.quantity.toIntOrNull() ?: 0 } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val discardedByItem: StateFlow<Map<String, Int>> = filteredHistory
        .map { list ->
            list.filter { it.status == "discarded" }
                .groupBy { it.name }
                .mapValues { entry -> entry.value.sumOf { it.quantity.toIntOrNull() ?: 0 } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun clearData() {
        historyListener?.remove()
        _historyList.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        historyListener?.remove()
    }
}