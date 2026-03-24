package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.ForgotPasswordData

data class ForgotPasswordResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)

/**
 * Extension function to convert DTO to domain model.
 * Separates data layer (DTOs) from domain layer (models).
 */
fun ForgotPasswordResponse.toDomain(): ForgotPasswordData = ForgotPasswordData(
    message = message,
    success = success
)
