package com.example.vidplay.domain.model

/**
 * Domain entity for a stream from the authenticated user's history.
 * Distinct from [Stream] because the /history/me endpoint returns
 * extra fields (ended_at, duration, max viewers, is_live) and
 * omits user_id / viewer_count.
 */
data class MyStream(
    val id: Int,
    val streamCode: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val startedAt: String,
    val endedAt: String?,
    val durationSeconds: Int?,
    val maxViewerCount: Int,
    val isLive: Boolean
)
