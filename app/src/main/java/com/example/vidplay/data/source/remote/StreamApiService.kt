package com.example.vidplay.data.source.remote

import com.example.vidplay.data.source.remote.dto.StreamDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Retrofit service interface for stream-related endpoints.
 * All methods are suspend functions so they integrate with Kotlin coroutines.
 */
interface StreamApiService {

    /**
     * GET /streams
     * Returns a list of all public live streams.
     */
    @GET("streams")
    suspend fun getAllStreams(
        @Header("Authorization") token: String
    ): Response<List<StreamDto>>

    /**
     * GET /streams/mine
     * Returns only the streams owned by the authenticated user.
     */
    @GET("streams/mine")
    suspend fun getMyStreams(
        @Header("Authorization") token: String
    ): Response<List<StreamDto>>
}
