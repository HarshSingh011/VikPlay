package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.MyStream

/**
 * DTO for items returned by GET /api/streaming/streams/search
 * Slightly different from MyStreamDto: no [id], has [viewerCount].
 */
data class SearchStreamDto(
    @SerializedName("stream_code")      val streamCode: String,
    @SerializedName("title")            val title: String?,
    @SerializedName("description")      val description: String?,
    @SerializedName("is_live")          val isLive: Boolean,
    @SerializedName("viewer_count")     val viewerCount: Int,
    @SerializedName("max_viewer_count") val maxViewerCount: Int,
    @SerializedName("thumbnail_url")    val thumbnailUrl: String?,
    @SerializedName("started_at")       val startedAt: String,
    @SerializedName("ended_at")         val endedAt: String?,
    @SerializedName("duration_seconds") val durationSeconds: Int?
)

/** Reuses [MyStream] as the domain entity — viewer_count mapped to maxViewerCount. */
fun SearchStreamDto.toDomain(): MyStream = MyStream(
    id              = 0,           // search endpoint doesn't return id
    streamCode      = streamCode,
    title           = title ?: streamCode,
    description     = description ?: "",
    thumbnailUrl    = thumbnailUrl,
    startedAt       = startedAt,
    endedAt         = endedAt,
    durationSeconds = durationSeconds,
    maxViewerCount  = maxViewerCount,
    isLive          = isLive
)
