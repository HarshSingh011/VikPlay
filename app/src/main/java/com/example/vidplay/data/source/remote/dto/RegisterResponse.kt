package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.RegistrationData

data class RegisterResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)

fun RegisterResponse.toDomain(): RegistrationData = RegistrationData(
    message = message,
    success = success
)
