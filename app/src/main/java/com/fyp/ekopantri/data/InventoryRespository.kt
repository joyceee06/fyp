package com.fyp.ekopantri.data

import android.net.Uri
import android.util.Log
import com.fyp.ekopantri.model.FoodItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class InventoryRepository {

    companion object {
        private const val TAG = "InventoryRepository"
        private const val COLLECTION_PATH = "inventory"
        private const val FIELD_STATUS = "status"
        private const val FIELD_QUANTITY = "quantity"
        private const val STATUS_ACTIVE = "active"
        private const val STORAGE_FOLDER = "inventory_images"
    }

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val inventoryCollection = db.collection(COLLECTION_PATH)
    private val userId get() = auth.currentUser?.uid ?: "test_user"

    // --- Read Operations ---
    fun getInventory(onUpdate: (List<FoodItem>) -> Unit): ListenerRegistration {
        return inventoryCollection
            .whereEqualTo("ownerId", userId)
            .whereEqualTo(FIELD_STATUS, STATUS_ACTIVE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(FoodItem::class.java) ?: emptyList()
                onUpdate(items)
            }
    }

    fun getProcessedHistory(onUpdate: (List<FoodItem>) -> Unit): ListenerRegistration {
        return inventoryCollection
            .whereEqualTo("ownerId", userId)
            .whereIn(FIELD_STATUS, listOf("consumed", "discarded"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val items = snapshot?.toObjects(FoodItem::class.java) ?: emptyList()
                onUpdate(items)
            }
    }

    suspend fun getInventoryOnce(): List<FoodItem> {
        return try {
            // We use the same userId logic you already have in the repository
            inventoryCollection
                .whereEqualTo("ownerId", userId)
                .whereEqualTo(FIELD_STATUS, STATUS_ACTIVE)
                .get()           // This fetches once
                .await()         // Wait for Firebase to finish
                .toObjects(FoodItem::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching inventory once", e)
            emptyList()
        }
    }

    // --- Write Operations ---
    suspend fun saveItem(item: FoodItem) {
        try {
            var finalImageUrl = item.imageUrl
            if (item.imageUrl.startsWith("content://")) {
                finalImageUrl = uploadImage(Uri.parse(item.imageUrl))
            }

            val itemToSave = item.copy(imageUrl = finalImageUrl, ownerId = userId)

            if (item.id.isEmpty()) {
                val docRef = inventoryCollection.document()
                inventoryCollection.document(docRef.id).set(itemToSave.copy(id = docRef.id)).await()
            } else {
                inventoryCollection.document(item.id).set(itemToSave).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save item", e)
            throw e
        }
    }

    suspend fun splitAndProcessItem(item: FoodItem, amountToProcess: Int, status: String) {
        val currentQty = item.quantity.toIntOrNull() ?: 0
        val remainingQty = currentQty - amountToProcess

        // 1. Create the history entry with the CURRENT TIME
        val historyEntry = item.copy(
            id = UUID.randomUUID().toString(),
            quantity = amountToProcess.toString(),
            status = status,
            ownerId = userId,
            processedDate = System.currentTimeMillis() // <--- FIX: SETS THE CURRENT DATE
        )
        inventoryCollection.document(historyEntry.id).set(historyEntry).await()

        // 2. Update the original item
        if (remainingQty <= 0) {
            inventoryCollection.document(item.id).delete().await()
        } else {
            inventoryCollection.document(item.id)
                .update("quantity", remainingQty.toString())
                .await()
        }
    }

    suspend fun updateItemStatus(itemId: String, status: String) {
        try {
            val updates = mapOf(
                FIELD_STATUS to status,
                "processedDate" to System.currentTimeMillis()
            )
            inventoryCollection.document(itemId).update(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status", e)
        }
    }

    suspend fun deleteItem(id: String) {
        try {
            inventoryCollection.document(id).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting item", e)
        }
    }

    private suspend fun uploadImage(imageUri: Uri): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("$STORAGE_FOLDER/$fileName")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }
}