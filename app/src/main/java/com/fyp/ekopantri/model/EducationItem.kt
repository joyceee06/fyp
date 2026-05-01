package com.fyp.ekopantri.model

data class EducationItem(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "",
    val emoji: String = "📖",
    val baseTips: List<String> = emptyList()
)