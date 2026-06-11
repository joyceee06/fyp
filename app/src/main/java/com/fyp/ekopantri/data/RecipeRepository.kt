package com.fyp.ekopantri.data

import com.fyp.ekopantri.api.SpoonacularService
import com.fyp.ekopantri.model.RecipeDetails
import com.fyp.ekopantri.model.RecipeSearchResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Repository for handling all recipe-related data operations from the Spoonacular API.
 */
class RecipeRepository {

    private val spoonService: SpoonacularService = Retrofit.Builder()
        .baseUrl("https://api.spoonacular.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpoonacularService::class.java)

    /**
     * Searches for recipes based on the provided ingredients string.
     */
    suspend fun searchRecipes(ingredients: String, apiKey: String): RecipeSearchResponse {
        return spoonService.searchRecipes(ingredients = ingredients, apiKey = apiKey)
    }

    /**
     * Fetches detailed information for a specific recipe by its ID.
     */
    suspend fun getRecipeInformation(id: Int, apiKey: String): RecipeDetails {
        return spoonService.getRecipeInformation(id = id, apiKey = apiKey)
    }
}
