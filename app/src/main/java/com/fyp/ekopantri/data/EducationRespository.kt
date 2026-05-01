package com.fyp.ekopantri.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.callbackFlow
import com.fyp.ekopantri.model.EducationItem
import kotlinx.coroutines.channels.awaitClose

class EducationRepository(
    private val firestore: FirebaseFirestore,
    private val generativeModel: GenerativeModel
) {
    // Step 1: Get all articles from Firebase
    fun getAllEducationItems() = callbackFlow {
        val collectionRef = firestore.collection("education")
        val subscription = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(EducationItem::class.java)?.copy(id = it.id) } ?: emptyList()
            trySend(list)
        }
        awaitClose { subscription.remove() }
    }
    // Step 2: Get specific article details from Firebase
    fun getEducationDetail(docId: String) = callbackFlow {
        val docRef = firestore.collection("education").document(docId)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val item = snapshot?.toObject(EducationItem::class.java)
            if (item != null) {
                trySend(item)
            }
        }

        awaitClose { subscription.remove() }
    }

    // Step 3: General AI Response (Used for the Search Bar Chat)
    suspend fun getGeneralAiResponse(userQuestion: String): String {
        val prompt = """
        You are EkoPantri AI, an expert in food sustainability and storage. 
        A user is asking: "$userQuestion".
        
        Provide a helpful, concise answer. 
        If the question is about food storage, safety, or waste reduction, give specific tips.
        Keep the tone friendly and professional. 
        Use bullet points if providing a list of steps.
    """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "I'm sorry, I couldn't process that question."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    // Step 4: Hybrid Enhancement (Used for the Detail Screen)
    suspend fun getAiEnhancement(baseContent: String, title: String): String {
        val prompt = """
            Article: $title. 
            Content: $baseContent. 
            Provide 2-3 extra tips for Malaysian climate (hot/humid) using simple language.
        """.trimIndent()
        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No extra tips available."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}