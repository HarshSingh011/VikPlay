package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.VerifyForgotPasswordOtpData

data class VerifyForgotPasswordOtpResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("success")
    val success: Boolean
)

fun VerifyForgotPasswordOtpResponse.toDomain(): VerifyForgotPasswordOtpData = VerifyForgotPasswordOtpData(
    message = message,
    success = success
)
