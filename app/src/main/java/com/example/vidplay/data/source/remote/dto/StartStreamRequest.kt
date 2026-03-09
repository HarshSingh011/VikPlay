package com.example.vidplay.data.source.remote.dto

import com.google.gson.annotations.SerializedName

/** JSON body sent to POST /api/streaming/streams/start */
data class StartStreamRequest(
    @SerializedName("title")         val title: String,
    @SerializedName("description")   val description: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?
)
