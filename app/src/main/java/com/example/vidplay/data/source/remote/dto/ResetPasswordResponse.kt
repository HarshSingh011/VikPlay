package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.ResetPasswordData

data class ResetPasswordResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)

fun ResetPasswordResponse.toDomain(): ResetPasswordData = ResetPasswordData(
    message = message,
    success = success
)
