package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.VerificationData

data class VerifyRegistrationResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)

fun VerifyRegistrationResponse.toDomain(): VerificationData = VerificationData(
    message = message,
    success = success
)
