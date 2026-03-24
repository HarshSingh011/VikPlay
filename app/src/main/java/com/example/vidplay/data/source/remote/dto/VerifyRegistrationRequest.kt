package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName

data class VerifyRegistrationRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("otp")
    val otp: String
)
