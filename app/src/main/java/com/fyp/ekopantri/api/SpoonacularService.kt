package com.fyp.ekopantri.api

import com.fyp.ekopantri.model.RecipeDetails
import com.fyp.ekopantri.model.RecipeSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpoonacularService {
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("includeIngredients") ingredients: String,
        @Query("addRecipeInformation") addInfo: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("ranking") ranking: Int = 2,
        @Query("number") number: Int = 10,
        @Query("apiKey") apiKey: String
    ): RecipeSearchResponse

    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String
    ): RecipeDetails
}
