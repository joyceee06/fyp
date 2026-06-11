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

/**
 * ViewModel responsible for processing receipt images using Google Gemini AI.
 * It handles image conversion, AI content generation, and JSON response parsing.
 */
class ScannerViewModel : ViewModel() {

    // --- AI CONFIGURATION ---
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_SCANNER_KEY
    )

    /**
     * Scans a receipt image from the given URI and returns a list of extracted [FoodItem]s.
     * This is a heavy operation performed on the IO dispatcher.
     */
    suspend fun scanReceipt(context: Context, imageUri: Uri): List<FoodItem> = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_SCANNER_KEY.isBlank()) {
            Log.e(TAG, "GEMINI_SCANNER_KEY missing in local.properties")
            return@withContext emptyList()
        }
        try {
            val bitmap = uriToBitmap(context, imageUri) ?: return@withContext emptyList()

            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(SCAN_PROMPT)
                }
            )

            val resultText = response.text ?: ""
            parseJsonResult(resultText)
        } catch (e: Exception) {
            Log.e(TAG, "Receipt scan failed: ${e.message}", e)
            emptyList()
        }
    }

    // --- PRIVATE IMAGE HELPERS ---

    /**
     * Converts a content Uri to a scaled Bitmap for the AI model to process.
     * This prevents OutOfMemory errors by ensuring the image is not unnecessarily large.
     */
    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true // Only get dimensions first
            }
            context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options) 
            }

            // Target max dimension (e.g., 1024px is enough for text recognition)
            val reqSize = 1024
            options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize)
            options.inJustDecodeBounds = false // Now decode for real

            context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options) 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap conversion error: ${e.message}")
            null
        }
    }

    /**
     * Calculates a power-of-two sample size for efficient downscaling.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // --- PRIVATE PARSING HELPERS ---

    /**
     * Extracts and parses the JSON array from the raw AI response text.
     */
    private fun parseJsonResult(jsonString: String): List<FoodItem> {
        val foodList = mutableListOf<FoodItem>()
        try {
            // Find the JSON array within the text to handle any conversational filler from the AI
            val startIndex = jsonString.indexOf("[")
            val endIndex = jsonString.lastIndexOf("]") + 1

            if (startIndex == -1 || endIndex <= 0) {
                Log.e(TAG, "No JSON array found in AI response")
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
            Log.e(TAG, "JSON Parsing error: ${e.message}", e)
        }
        return foodList
    }

    companion object {
        private const val TAG = "ScannerViewModel"

        private val SCAN_PROMPT = """
            Analyze this receipt image. Extract all food items.
            Return ONLY a JSON array of objects. 
            Format: 
            [{
            "name": "item name", 
            "quantity": "amount", 
            "category": "category", 
            "storageLocation": "location"
            }]
            Categories: Fruits, Vegetables, Dairy, Meat, Grains, Snacks, Beverages.
            Storage: Freezer, Fridge or Pantry.
            """.trimIndent()
    }
}
