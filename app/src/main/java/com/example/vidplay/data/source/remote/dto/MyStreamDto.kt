package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.vidplay.domain.model.MyStream

/** DTO for the /api/streaming/streams/history/me response items. */
data class MyStreamDto(
    @SerializedName("id")               val id: Int,
    @SerializedName("stream_code")      val streamCode: String,
    @SerializedName("title")            val title: String?,
    @SerializedName("description")      val description: String?,
    @SerializedName("thumbnail_url")    val thumbnailUrl: String?,
    @SerializedName("started_at")       val startedAt: String,
    @SerializedName("ended_at")         val endedAt: String?,
    @SerializedName("duration_seconds") val durationSeconds: Int?,
    @SerializedName("max_viewer_count") val maxViewerCount: Int,
    @SerializedName("is_live")          val isLive: Boolean
)

fun MyStreamDto.toDomain(): MyStream = MyStream(
    id             = id,
    streamCode     = streamCode,
    title          = title ?: streamCode,
    description    = description ?: "",
    thumbnailUrl   = thumbnailUrl,
    startedAt      = startedAt,
    endedAt        = endedAt,
    durationSeconds = durationSeconds,
    maxViewerCount = maxViewerCount,
    isLive         = isLive
)
