package com.example.vidplay.util

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()

    data class Success<T>(val data: T) : Resource<T>()

    data class Error(
        val message: String,
        val data: Nothing? = null
    ) : Resource<Nothing>()
}
