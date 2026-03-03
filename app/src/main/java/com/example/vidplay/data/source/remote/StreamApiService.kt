package com.example.vidplay.data.source.remote

import com.example.vidplay.data.source.remote.dto.MyStreamDto
import com.example.vidplay.data.source.remote.dto.SearchStreamDto
import com.example.vidplay.data.source.remote.dto.StreamDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Retrofit service interface for stream-related endpoints.
 * All methods are suspend functions so they integrate with Kotlin coroutines.
 */
interface StreamApiService {

    /**
     * GET streaming/streams/live
     * Returns a list of all currently live streams.
     */
    @GET("streaming/streams/live")
    suspend fun getAllStreams(
        @Header("Authorization") token: String
    ): Response<List<StreamDto>>

    /**
     * GET streaming/streams/history/me
     * Returns the authenticated user's personal stream history.
     */
    @GET("streaming/streams/history/me")
    suspend fun getMyStreams(
        @Header("Authorization") token: String
    ): Response<List<MyStreamDto>>

    /**
     * GET streaming/streams/search?q=...&live_only=true
     * Search streams by title/keyword.
     */
    @GET("streaming/streams/search")
    suspend fun searchStreams(
        @Header("Authorization") token: String,
        @Query("q") query: String,
        @Query("live_only") liveOnly: Boolean = true
    ): Response<List<SearchStreamDto>>
}
