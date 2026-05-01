package com.fyp.ekopantri.ui.inventory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.fyp.ekopantri.BuildConfig
import com.fyp.ekopantri.model.FoodItem
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStream

class ScannerViewModel : ViewModel() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = BuildConfig.GEMINI_SCANNER_KEY
    )


    suspend fun scanReceipt(context: Context, imageUri: Uri): List<FoodItem> =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = uriToBitmap(context, imageUri) ?: return@withContext emptyList()

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(PROMPT)
                    }
                )

                val resultText = response.text ?: ""
                parseJsonResult(resultText)
            } catch (e: Exception) {
                // This will now log the real error (like 429 if you are out of free credits)
                Log.e("ScannerViewModel", "Scan failed: ${e.message}", e)
                emptyList()
            }
        }

    private fun parseJsonResult(jsonString: String): List<FoodItem> {
        val foodList = mutableListOf<FoodItem>()
        try {
            // Find the start and end of the JSON array to handle extra text from AI
            val startIndex = jsonString.indexOf("[")
            val endIndex = jsonString.lastIndexOf("]") + 1

            if (startIndex == -1 || endIndex <= 0) {
                Log.e("ScannerViewModel", "No JSON array found in response")
                return emptyList()
            }

            val cleanJson = jsonString.substring(startIndex, endIndex)
            val jsonArray = JSONArray(cleanJson)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                foodList.add(
                    FoodItem(
                        name = obj.optString("name", "Unknown"),
                        quantity = obj.optString("quantity", "1"),
                        category = obj.optString("category", "Grains"),
                        storageLocation = obj.optString("storageLocation", "Pantry")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("ScannerViewModel", "JSON Parsing error: ${e.message}", e)
        }
        return foodList
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("ScannerViewModel", "Bitmap conversion error", e)
            null
        }
    }

    companion object {
        private val PROMPT = """
            Analyze this receipt image. Extract all food items.
            Return ONLY a JSON array of objects. 
            Format: [{"name": "item name", "quantity": "amount", "category": "category", "storageLocation": "location"}]
            Categories: Fruits, Vegetables, Dairy, Meat, Grains, Snacks, Beverages.
            Storage: Freezer, Fridge, or Pantry.
            """.trimIndent()
    }
}