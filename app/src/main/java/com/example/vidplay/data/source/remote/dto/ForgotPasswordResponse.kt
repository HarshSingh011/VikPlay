package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.ForgotPasswordData

data class ForgotPasswordResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)

fun ForgotPasswordResponse.toDomain(): ForgotPasswordData = ForgotPasswordData(
    message = message,
    success = success
)
