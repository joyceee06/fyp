package com.fyp.ekopantri.data

import android.util.Log
import com.fyp.ekopantri.BuildConfig
import com.fyp.ekopantri.model.EducationItem
import com.google.ai.client.generativeai.GenerativeModel
import org.json.JSONArray

/**
 * Repository responsible for fetching and generating educational content about food storage.
 * Uses Google Gemini AI to provide dynamic articles and assistant responses.
 */
class EducationRepository(
    private val generativeModel: GenerativeModel
) {
    
    // --- 1. CONFIGURATION & CONSTANTS ---
    companion object {
        private const val TAG = "EducationRepository"
        private const val ERROR_AI_CONFIG_MISSING = "AI Configuration Missing."
        private const val ERROR_AI_NO_RESPONSE = "I couldn't process that right now."
        private const val LIMIT_MSG = "You've reached EkoPantri AI's current limit. Please try again in a minute or two."
    }

    private fun isAiConfigured(): Boolean = BuildConfig.GEMINI_EDUCATION_KEY.isNotBlank()

    // --- 2. PUBLIC API ---

    /**
     * Interacts with AI to answer general user questions about food storage.
     */
    suspend fun getGeneralAiResponse(userQuestion: String): String {
        if (!isAiConfigured()) return ERROR_AI_CONFIG_MISSING

        val prompt = """
            You are EkoPantri AI, an expert in food sustainability. 
            User Question: "$userQuestion"
            
            Instructions:
            1. Provide clear, actionable food storage tips specifically for the Malaysian context (humidity, temperature).
            2. Limit response to 150 words. 
            3. Use a friendly and encouraging tone.
        """.trimIndent()

        return try {
            generativeModel.generateContent(prompt).text ?: ERROR_AI_NO_RESPONSE
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: ""
            if (errorMsg.contains("429") || errorMsg.contains("quota", true)) {
                LIMIT_MSG
            } else {
                Log.e(TAG, "General AI Error: $errorMsg")
                "Error: $errorMsg"
            }
        }
    }

    /**
     * Generates a list of education articles based on current pantry inventory.
     */
    suspend fun generateAiArticles(pantryItems: List<String>): List<EducationItem> {
        if (!isAiConfigured()) return emptyList()

        val context = if (pantryItems.isEmpty()) {
            "general food sustainability and zero-waste kitchen habits"
        } else {
            "optimizing storage for: ${pantryItems.take(5).joinToString(", ")}"
        }

        val prompt = """
            Generate 10 expert food storage articles about $context.
            Return ONLY a JSON array of objects with the following structure:
            [
              {
                "id": "ai_[unique_id]",
                "title": "Article Title",
                "category": "Storage" | "Preservation" | "Awareness",
                "emoji": "Relevant Emoji",
                "content": "3-4 sentences of core advice.",
                "baseTips": ["Tip 1", "Tip 2", "Tip 3", "Tip 4", ...]
              }
            ]
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt).text ?: ""
            Log.d(TAG, "AI Raw Response: $response")
            
            val articles = parseAiArticles(response)
            articles.ifEmpty { getFallbackArticles() }
        } catch (e: Exception) {
            Log.e(TAG, "Article Generation Error: ${e.message}")
            getFallbackArticles()
        }
    }

    // --- 3. PRIVATE PARSING & FALLBACKS ---

    private fun parseAiArticles(jsonText: String): List<EducationItem> {
        val jsonStart = jsonText.indexOf("[")
        val jsonEnd = jsonText.lastIndexOf("]") + 1
        if (jsonStart == -1 || jsonEnd == -1) return emptyList()

        return try {
            val jsonArray = JSONArray(jsonText.substring(jsonStart, jsonEnd))
            List(jsonArray.length()) { i ->
                val obj = jsonArray.getJSONObject(i)
                EducationItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    category = obj.getString("category"),
                    emoji = obj.getString("emoji"),
                    content = obj.getString("content"),
                    baseTips = obj.getJSONArray("baseTips").let { arr ->
                        List(arr.length()) { j -> arr.getString(j) }
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON Parsing Error: ${e.message}")
            emptyList()
        }
    }

    private fun getFallbackArticles(): List<EducationItem> {
        return listOf(
            EducationItem(
                id = "fb_1",
                title = "The Golden Rule of Freshness",
                category = "Storage",
                emoji = "🥬",
                content = "Keep your leafy greens dry! Store them in a container with a paper towel to absorb moisture.",
                baseTips = listOf("Wash only before use", "Use airtight containers", "Replace towels regularly")
            ),
            EducationItem(
                id = "fb_2",
                title = "Ethylene: The Ripening Gas",
                category = "Awareness",
                emoji = "🍎",
                content = "Some fruits release gas that makes others rot faster. Keep apples and bananas separate from vegetables.",
                baseTips = listOf("Separate gas producers", "Check ripeness daily", "Freeze overripe fruit")
            ),
            EducationItem(
                id = "fb_3",
                title = "Freezer Friendly Habits",
                category = "Preservation",
                emoji = "❄️",
                content = "Almost anything can be frozen! Blanch vegetables before freezing to lock in nutrients and color.",
                baseTips = listOf("Label with dates", "Use freezer-safe bags", "Blanch for 2 minutes")
            )
        )
    }
}
