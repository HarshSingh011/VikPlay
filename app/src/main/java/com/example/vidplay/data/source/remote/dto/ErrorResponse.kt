package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("detail")
    val detail: String
)
