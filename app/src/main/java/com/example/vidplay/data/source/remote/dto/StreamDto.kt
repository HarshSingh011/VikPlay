package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.Stream

/**
 * Data Transfer Object that mirrors the JSON shape returned by the API.
 * Field names match the snake_case API contract via @SerializedName.
 */
data class StreamDto(
    @SerializedName("stream_code")  val streamCode: String,
    @SerializedName("title")        val title: String,
    @SerializedName("description")  val description: String,
    @SerializedName("user_id")      val userId: Int,
    @SerializedName("viewer_count") val viewerCount: Int,
    @SerializedName("thumbnail_url") val thumbnailUrl: String,
    @SerializedName("started_at")   val startedAt: String
)

/** Convert a DTO to the domain model understood by the rest of the app. */
fun StreamDto.toDomain(): Stream = Stream(
    streamCode   = streamCode,
    title        = title,
    description  = description,
    userId       = userId,
    viewerCount  = viewerCount,
    thumbnailUrl = thumbnailUrl,
    startedAt    = startedAt
)
