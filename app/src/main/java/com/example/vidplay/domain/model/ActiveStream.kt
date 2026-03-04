package com.example.vidplay.domain.model

/**
 * Domain model for a stream that the current user has just started.
 * Contains stream_key which is required to connect as WebRTC broadcaster.
 */
data class ActiveStream(
    val streamCode: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val streamKey: String,
    val startedAt: String
)
