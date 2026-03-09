package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.ActiveStream

/**
 * DTO for the POST /api/streaming/streams/start response.
 * Distinct from StreamDto because it includes stream_key which
 * the broadcaster needs to connect to the WebRTC signaling server.
 */
data class ActiveStreamDto(
    @SerializedName("stream_code")   val streamCode: String,
    @SerializedName("title")         val title: String?,
    @SerializedName("description")   val description: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("stream_key")    val streamKey: String,
    @SerializedName("started_at")    val startedAt: String,
    @SerializedName("message")       val message: String?
)

fun ActiveStreamDto.toDomain(): ActiveStream = ActiveStream(
    streamCode   = streamCode,
    title        = title ?: streamCode,
    description  = description ?: "",
    thumbnailUrl = thumbnailUrl,
    streamKey    = streamKey,
    startedAt    = startedAt
)
