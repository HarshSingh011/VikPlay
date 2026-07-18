package com.example.vidplay.domain.model

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val isActive: Boolean,
    val isVerified: Boolean,
    val createdAt: String
)
