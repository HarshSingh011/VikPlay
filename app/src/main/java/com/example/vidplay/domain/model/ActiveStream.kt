package com.example.vidplay.domain.model

data class ActiveStream(
    val streamCode: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val streamKey: String,
    val startedAt: String
)
