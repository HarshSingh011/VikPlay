package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.LoginData
import com.example.vidplay.domain.model.User

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("user")
    val user: UserDto
)

data class UserDto(
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("is_verified")
    val isVerified: Boolean,
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * Extension function to convert DTO to domain model.
 * Separates data layer (DTOs) from domain layer (models).
 */
fun LoginResponse.toDomain(): LoginData = LoginData(
    accessToken = accessToken,
    tokenType = tokenType,
    user = user.toDomain()
)

fun UserDto.toDomain(): User = User(
    id = id,
    username = username,
    email = email,
    isActive = isActive,
    isVerified = isVerified,
    createdAt = createdAt
)
