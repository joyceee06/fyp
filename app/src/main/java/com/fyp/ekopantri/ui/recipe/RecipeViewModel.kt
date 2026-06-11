package com.fyp.ekopantri.ui.recipe

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.ekopantri.BuildConfig
import com.fyp.ekopantri.data.RecipeRepository
import com.fyp.ekopantri.model.FoodItem
import com.fyp.ekopantri.model.RecipeDetails
import com.fyp.ekopantri.model.RecipeSearchItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RecipeUiState {
    data object Loading : RecipeUiState
    data class Success(val data: RecipeDetails) : RecipeUiState
    data class Error(val message: String) : RecipeUiState
}

class RecipeViewModel : ViewModel() {

    private val repository = RecipeRepository()

    private val _recipes = MutableStateFlow<List<RecipeSearchItem>>(emptyList())
    val recipes: StateFlow<List<RecipeSearchItem>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Loading)
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    private fun isRecipeConfigured(): Boolean = BuildConfig.SPOONACULAR_KEY.isNotBlank()

    fun generateRecipes(pantryItems: List<FoodItem>) {
        if (pantryItems.isEmpty()) return
        if (!isRecipeConfigured()) {
            Log.e("RecipeVM", "SPOONACULAR_KEY missing in local.properties")
            _recipes.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Process items: Remove expired, sort by urgency, take unique names
                val now = System.currentTimeMillis()
                val ingredientNames = pantryItems
                    .filter { it.expiryDate > now }
                    .sortedBy { it.expiryDate }
                    .map { it.name.lowercase().trim() }
                    .distinct()

                if (ingredientNames.isEmpty()) {
                    _recipes.value = emptyList()
                    return@launch
                }

                // 2. Prepare API Query (Top 3 ingredients)
                val query = ingredientNames.take(3).joinToString(",")

                // 3. Fetch from Repository
                var response = repository.searchRecipes(
                    ingredients = query,
                    apiKey = BuildConfig.SPOONACULAR_KEY
                )

                // Fallback: If 3-item combo fails, try the single most urgent item
                if (response.results.isEmpty() && ingredientNames.isNotEmpty()) {
                    response = repository.searchRecipes(
                        ingredients = ingredientNames[0],
                        apiKey = BuildConfig.SPOONACULAR_KEY
                    )
                }

                _recipes.value = response.results

            } catch (e: Exception) {
                Log.e("RecipeVM", "Error: ${e.message}")
                _recipes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchRecipeDetails(recipeId: Int, onComplete: () -> Unit) {
        if (!isRecipeConfigured()) {
            _uiState.value = RecipeUiState.Error("SPOONACULAR_KEY missing in local.properties")
            return
        }
        viewModelScope.launch {
            _uiState.value = RecipeUiState.Loading
            try {
                val response = repository.getRecipeInformation(
                    id = recipeId,
                    apiKey = BuildConfig.SPOONACULAR_KEY
                )
                _uiState.value = RecipeUiState.Success(response)
                onComplete()
            } catch (e: Exception) {
                Log.e("RecipeVM", "Detail error: ${e.message}")
                _uiState.value = RecipeUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}
