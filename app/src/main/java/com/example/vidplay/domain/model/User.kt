package com.example.vidplay.domain.model

/**
 * Pure Kotlin domain entity for a user.
 * No Android or framework dependencies — safe to unit-test without Robolectric.
 */
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val isActive: Boolean,
    val isVerified: Boolean,
    val createdAt: String
)
