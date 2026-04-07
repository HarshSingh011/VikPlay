package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic error response from the backend API.
 * Handles error messages in the "detail" field.
 */
data class ErrorResponse(
    @SerializedName("detail")
    val detail: String
)
