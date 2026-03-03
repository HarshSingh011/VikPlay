package com.example.vidplay.domain.model

/**
 * Pure Kotlin domain entity for a live stream.
 * No Android or framework dependencies — safe to unit-test without Robolectric.
 */
data class Stream(
    val streamCode: String,
    val title: String,
    val description: String,
    val userId: Int,
    val viewerCount: Int,
    val thumbnailUrl: String?,   // may be null when server hasn't set a thumbnail
    val startedAt: String
)
