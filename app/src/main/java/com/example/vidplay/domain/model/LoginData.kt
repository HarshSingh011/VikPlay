package com.example.vidplay.domain.model

data class LoginData(
    val accessToken: String,
    val tokenType: String,
    val user: User
)
