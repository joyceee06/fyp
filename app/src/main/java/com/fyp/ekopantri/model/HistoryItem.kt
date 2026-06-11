package com.fyp.ekopantri.model

data class HistoryItem(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val category: String = "",
    val quantity: String = "1",
    val status: String = "consumed",
    val processedDate: Long = 0
)
