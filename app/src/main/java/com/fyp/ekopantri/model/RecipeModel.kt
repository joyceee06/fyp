package com.fyp.ekopantri.model

data class RecipeSearchResponse(
    val results: List<RecipeSearchItem>
)

data class RecipeSearchItem(
    val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val dishTypes: List<String>?
)

data class RecipeDetails(
    val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val summary: String,
    val dishTypes: List<String>?,
    val extendedIngredients: List<SpoonIngredient>,
    val instructions: String? = ""
)

data class SpoonIngredient(
    val id: Int,
    val name: String,
    val original: String,
    val amount: Double,
    val unit: String
)
