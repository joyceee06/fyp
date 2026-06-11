package com.fyp.ekopantri.data

import android.net.Uri
import android.util.Log
import com.fyp.ekopantri.model.FoodItem
import com.fyp.ekopantri.model.HistoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository responsible for managing inventory and history data in Firestore and Storage.
 * Uses separate collections for active items and processed history to keep the database organized.
 */
class InventoryRepository {

    companion object {
        private const val TAG = "InventoryRepository"
        private const val COLLECTION_INVENTORY = "inventory"
        private const val COLLECTION_HISTORY = "history"
        private const val STORAGE_FOLDER = "inventory_images"
    }

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val inventoryCollection = db.collection(COLLECTION_INVENTORY)
    private val historyCollection = db.collection(COLLECTION_HISTORY)
    
    private val userId get() = auth.currentUser?.uid ?: "test_user"

    // --- READ OPERATIONS ---

    /**
     * Real-time listener for current pantry items.
     */
    fun getInventory(onUpdate: (List<FoodItem>) -> Unit): ListenerRegistration {
        return inventoryCollection
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Inventory listen failed", error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FoodItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onUpdate(items)
            }
    }

    /**
     * Real-time listener for consumed or discarded history items.
     */
    fun getProcessedHistory(onUpdate: (List<HistoryItem>) -> Unit): ListenerRegistration {
        return historyCollection
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "History listen failed", error)
                    return@addSnapshotListener
                }
                // Manually map the ID for stability
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(HistoryItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onUpdate(items)
            }
    }

    /**
     * Fetches current inventory once (useful for background workers).
     */
    suspend fun getInventoryOnce(): List<FoodItem> {
        return try {
            val snapshot = inventoryCollection
                .whereEqualTo("ownerId", userId)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(FoodItem::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching inventory once", e)
            emptyList()
        }
    }

    // --- WRITE OPERATIONS ---

    /**
     * Saves or updates an item in the active inventory.
     */
    suspend fun saveItem(item: FoodItem) {
        try {
            var finalImageUrl = item.imageUrl
            if (item.imageUrl.startsWith("content://")) {
                finalImageUrl = uploadImage(Uri.parse(item.imageUrl))
            }

            val docRef =
                if (item.id.isEmpty())
                    inventoryCollection.document()
                else
                    inventoryCollection.document(item.id)

            val itemToSave = item.copy(
                id = docRef.id,
                imageUrl = finalImageUrl, 
                ownerId = userId
            )

            docRef.set(itemToSave).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save item", e)
            throw e
        }
    }

    /**
     * Moves items from inventory to history. 
     * Handles both full consumption and partial splits.
     */
    suspend fun splitAndProcessItem(item: FoodItem, amountToProcess: Int, status: String) {
        val currentQty = item.quantity.toIntOrNull() ?: 0
        val remainingQty = currentQty - amountToProcess

        // 1. Generate a unique ID for the history document
        val historyDocId = UUID.randomUUID().toString()

        // 2. Create a History Object with the generated ID
        val historyEntry = HistoryItem(
            id = historyDocId,
            ownerId = userId,
            name = item.name,
            category = item.category,
            quantity = amountToProcess.toString(),
            status = status,
            processedDate = System.currentTimeMillis()
        )
        
        historyCollection.document(historyDocId).set(historyEntry).await()

        // 3. Update or Delete the original item in the inventory collection
        if (remainingQty <= 0) {
            inventoryCollection.document(item.id).delete().await()
        } else {
            inventoryCollection.document(item.id)
                .update("quantity", remainingQty.toString())
                .await()
        }
    }

    /**
     * Permanently deletes an item from the active inventory.
     */
    suspend fun deleteItem(id: String) {
        try {
            inventoryCollection.document(id).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting item", e)
        }
    }

    // --- INTERNAL HELPERS ---

    private suspend fun uploadImage(imageUri: Uri): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("$STORAGE_FOLDER/$fileName")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }
}
