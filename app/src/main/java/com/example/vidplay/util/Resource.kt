package com.example.vidplay.util

/**
 * A generic wrapper for UI-facing data that can be in one of three states:
 *  - Loading  : a network/db operation is in progress
 *  - Success  : operation completed and data is available
 *  - Error    : operation failed with an optional message and optional stale data
 */
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()

    data class Success<T>(val data: T) : Resource<T>()

    data class Error(
        val message: String,
        val data: Nothing? = null
    ) : Resource<Nothing>()
}
