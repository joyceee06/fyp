package com.fyp.ekopantri.model

data class FoodItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val storageLocation: String = "",
    val quantity: String = "",
    val expiryDate: Long = 0,
    val imageUrl: String = "",
    val reminderDays: Int = 1,
    val ownerId: String = "",
)
