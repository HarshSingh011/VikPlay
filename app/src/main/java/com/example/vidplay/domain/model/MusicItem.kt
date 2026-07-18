package com.example.vidplay.domain.model

import android.net.Uri

data class MusicItem(
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,       
    val size: Long,
    val albumArtUri: Uri?
)
