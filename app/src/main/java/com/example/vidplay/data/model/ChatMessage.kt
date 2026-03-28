package com.example.vidplay.data.model

data class ChatMessage(
    val username: String,
    val message: String,
    val role: String = "viewer", // "broadcaster" or "viewer"
    val timestamp: Long = System.currentTimeMillis()
)
