package com.example.vidplay.domain.repository

import com.example.vidplay.domain.model.ActiveStream
import com.example.vidplay.domain.model.MyStream
import com.example.vidplay.domain.model.Stream
import com.example.vidplay.util.Resource

interface StreamRepository {

    
    suspend fun getAllStreams(token: String): Resource<List<Stream>>

    
    suspend fun getMyStreams(token: String): Resource<List<MyStream>>

    
    suspend fun searchStreams(token: String, query: String): Resource<List<MyStream>>

    
    suspend fun startStream(
        token: String,
        title: String,
        description: String?,
        thumbnailUrl: String?
    ): Resource<ActiveStream>

    
    suspend fun endStream(token: String, streamCode: String): Resource<Unit>
}
