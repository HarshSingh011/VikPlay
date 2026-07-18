package com.example.vidplay.data.model

data class ChatMessage(
    val username: String,
    val message: String,
    val role: String = "viewer", 
    val timestamp: Long = System.currentTimeMillis()
)
