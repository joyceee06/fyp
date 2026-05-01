package com.fyp.ekopantri.ui.recipe

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.ekopantri.BuildConfig
import com.fyp.ekopantri.api.SpoonacularService
import com.fyp.ekopantri.model.RecipeDetails
import com.fyp.ekopantri.model.RecipeSearchItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeViewModel : ViewModel() {

    private val spoonService: SpoonacularService = Retrofit.Builder()
        .baseUrl("https://api.spoonacular.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpoonacularService::class.java)

    private val _recipes = MutableStateFlow<List<RecipeSearchItem>>(emptyList())
    val recipes: StateFlow<List<RecipeSearchItem>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedRecipeDetail = MutableStateFlow<RecipeDetails?>(null)
    val selectedRecipeDetail: StateFlow<RecipeDetails?> = _selectedRecipeDetail.asStateFlow()

    fun generateRecipes(pantryItems: List<String>) {
        if (pantryItems.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cleanedItems = pantryItems.map { it.lowercase().trim().split(" ").last() }
                val query = cleanedItems.take(3).joinToString(",")

                var response = spoonService.searchRecipes(
                    ingredients = query,
                    apiKey = BuildConfig.SPOONACULAR_KEY
                )

                if (response.results.isEmpty() && cleanedItems.isNotEmpty()) {
                    response = spoonService.searchRecipes(
                        ingredients = cleanedItems[0],
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
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _selectedRecipeDetail.value = null

                val response = spoonService.getRecipeInformation(
                    id = recipeId,
                    apiKey = BuildConfig.SPOONACULAR_KEY
                )
                _selectedRecipeDetail.value = response
                onComplete()
            } catch (e: Exception) {
                Log.e("RecipeVM", "Detail error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}