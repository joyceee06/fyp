package com.fyp.ekopantri.ui.education

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fyp.ekopantri.BuildConfig
import com.fyp.ekopantri.data.EducationRepository
import com.fyp.ekopantri.model.EducationItem
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

//UI State for the Education Detail Screen
sealed interface EducationUiState {
    data object Loading : EducationUiState
    data class Success(val data: EducationItem) : EducationUiState
    data class Error(val message: String) : EducationUiState
}

class EducationViewModel(private val repository: EducationRepository) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = EducationRepository(
                    firestore = FirebaseFirestore.getInstance(),
                    generativeModel = GenerativeModel(
                        modelName = "gemini-3-flash-preview",
                        apiKey = BuildConfig.GEMINI_EDUCATION_KEY
                    )
                )
                EducationViewModel(repository)
            }
        }
    }

    // =====================================================================================
    // MAIN SCREEN STATES (List of Articles & General AI)
    // =====================================================================================

    private val _articles = MutableStateFlow<List<EducationItem>>(emptyList())
    val articles: StateFlow<List<EducationItem>> = _articles.asStateFlow()

    private val _aiAnswer = MutableStateFlow<String?>(null)
    val aiAnswer: StateFlow<String?> = _aiAnswer.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()


    // =====================================================================================
    // DETAIL SCREEN STATES (Specific Article & AI Enhancement)
    // =====================================================================================

    private val _uiState = MutableStateFlow<EducationUiState>(EducationUiState.Loading)
    val uiState: StateFlow<EducationUiState> = _uiState.asStateFlow()

    private val _aiTips = MutableStateFlow<String?>(null)
    val aiTips: StateFlow<String?> = _aiTips.asStateFlow()


    // =====================================================================================
    // MAIN SCREEN LOGIC
    // =====================================================================================

    // Fetches all educational articles from Firebase Firestore
    fun fetchAllArticles() {
        viewModelScope.launch {
            repository.getAllEducationItems()
                .catch { e -> /* Handle error if necessary */ }
                .collect { list ->
                    _articles.value = list
                }
        }
    }

    // General chatbot logic for the search bar section
    fun askAiAboutFood(question: String) {
        if (question.isBlank()) return

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                _aiAnswer.value = repository.getGeneralAiResponse(question)
            } finally {
                _isAiLoading.value = false
            }
        }
    }


    // =====================================================================================
    // DETAIL SCREEN LOGIC
    // =====================================================================================


     // Loads a specific article by ID from Firestore
    fun loadContent(id: String) {
        viewModelScope.launch {
            _uiState.value = EducationUiState.Loading
            _aiTips.value = null

            repository.getEducationDetail(id)
                .catch { e ->
                    _uiState.value = EducationUiState.Error(e.message ?: "Unknown Error")
                }
                .collect { data ->
                    _uiState.value = EducationUiState.Success(data)
                }
        }
    }

    // Uses Gemini AI to enhance a specific Firebase article with Malaysian context
    fun enhanceWithAi(item: EducationItem) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val currentTips = repository.getAiEnhancement(item.content, item.title)
                _aiTips.value = currentTips
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}