package com.example.vidplay.domain.model

/**
 * Domain model for login response containing authentication token and user info.
 */
data class LoginData(
    val accessToken: String,
    val tokenType: String,
    val user: User
)
