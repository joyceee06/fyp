package com.fyp.ekopantri.ui.education

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fyp.ekopantri.BuildConfig
import com.fyp.ekopantri.data.EducationRepository
import com.fyp.ekopantri.model.EducationItem
import com.fyp.ekopantri.model.FoodItem
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Sealed interface representing the UI state for education content loading.
 */
sealed interface EducationUiState {
    data object Loading : EducationUiState
    data class Success(val data: EducationItem) : EducationUiState
    data class Error(val message: String) : EducationUiState
}

/**
 * ViewModel responsible for managing AI-generated educational content.
 * It handles article generation based on inventory, general AI assistance, and filtering.
 */
class EducationViewModel(private val repository: EducationRepository) : ViewModel() {

    // --- 1. RAW DATA STREAMS (INTERNAL) ---
    private val _aiGeneratedArticles = MutableStateFlow<List<EducationItem>>(emptyList())
    private val pantryItems = MutableStateFlow<List<FoodItem>>(emptyList())
    
    private val _aiAnswer = MutableStateFlow<String?>(null)
    private val _isAiLoading = MutableStateFlow(false)
    private val _isArticlesLoading = MutableStateFlow(false)
    private val _uiState = MutableStateFlow<EducationUiState>(EducationUiState.Loading)

    // --- 2. PUBLIC UI STATE ---
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    val aiAnswer = _aiAnswer.asStateFlow()
    val isAiLoading = _isAiLoading.asStateFlow()
    val isArticlesLoading = _isArticlesLoading.asStateFlow()
    val uiState = _uiState.asStateFlow()

    /**
     * The main filtered and sorted content for the Education Screen.
     * Prioritizes articles based on the URGENCY (Expiry Date) of matching items in the pantry.
     */
    val filteredContent: StateFlow<List<EducationItem>> = combine(
        _aiGeneratedArticles, searchQuery, selectedCategory, pantryItems
    ) { articles, query, category, inventory ->
        
        articles
            .filter { article ->
                val matchesCategory = category == "All" ||
                        article.category.equals(category, ignoreCase = true)
                val matchesSearch = article.title.contains(query, ignoreCase = true) || 
                                   article.content.contains(query, ignoreCase = true)
                matchesCategory && matchesSearch
            }
            .sortedBy { article ->
                val matchingItem = inventory.find { item ->
                    article.title.contains(item.name, ignoreCase = true) || 
                    article.content.contains(item.name, ignoreCase = true)
                }
                matchingItem?.expiryDate ?: Long.MAX_VALUE
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- 3. BUSINESS LOGIC & ACTIONS ---

    /**
     * Updates the inventory context and triggers article generation if needed.
     */
    fun updateInventoryItems(items: List<FoodItem>) {
        val hasArticles = _aiGeneratedArticles.value.isNotEmpty()
        val itemsChanged = pantryItems.value != items

        pantryItems.value = items
        
        // Trigger AI generation if articles are missing OR inventory has changed
        if (!hasArticles || itemsChanged) {
            val itemNames = items.map { it.name }.distinct()
            Log.d(TAG, "Triggering AI generation. Items: $itemNames")
            generateDynamicContent(itemNames)
        }
    }

    /**
     * Asks the AI assistant a specific question about food sustainability or storage.
     */
    fun askAiAboutFood(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiAnswer.value = repository.getGeneralAiResponse(question)
            _isAiLoading.value = false
        }
    }

    /**
     * Loads a specific article's content for the detail screen.
     */
    fun loadContent(id: String) {
        viewModelScope.launch {
            _uiState.value = EducationUiState.Loading
            val cachedAiItem = _aiGeneratedArticles.value.find { it.id == id }
            
            if (cachedAiItem != null) {
                _uiState.value = EducationUiState.Success(cachedAiItem)
            } else {
                _uiState.value = EducationUiState.Error("Article not found or expired.")
            }
        }
    }

    // --- 4. PRIVATE HELPERS ---

    private fun generateDynamicContent(items: List<String>) {
        if (_isArticlesLoading.value) return
        
        viewModelScope.launch {
            _isArticlesLoading.value = true
            val generated = repository.generateAiArticles(items)
            if (generated.isNotEmpty()) {
                _aiGeneratedArticles.value = generated
            } else {
                Log.e(TAG, "Failed to generate AI articles")
            }
            _isArticlesLoading.value = false
        }
    }

    // --- 5. FACTORY & COMPANION ---

    companion object {
        private const val TAG = "EducationViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = EducationRepository(
                    generativeModel = GenerativeModel(
                        modelName = "gemini-1.5-flash",
                        apiKey = BuildConfig.GEMINI_EDUCATION_KEY
                    )
                )
                EducationViewModel(repository)
            }
        }
    }
}
