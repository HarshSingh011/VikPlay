package com.example.vidplay.domain.model

data class Stream(
    val streamCode: String,
    val title: String,
    val description: String,
    val userId: Int,
    val viewerCount: Int,
    val thumbnailUrl: String?,   
    val startedAt: String
)
