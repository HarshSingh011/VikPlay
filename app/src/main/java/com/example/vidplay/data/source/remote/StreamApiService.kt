package com.example.vidplay.data.source.remote

import com.example.vidplay.data.source.remote.dto.ActiveStreamDto
import com.example.vidplay.data.source.remote.dto.MyStreamDto
import com.example.vidplay.data.source.remote.dto.SearchStreamDto
import com.example.vidplay.data.source.remote.dto.StartStreamRequest
import com.example.vidplay.data.source.remote.dto.StreamDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StreamApiService {

    

    @GET("api/streaming/streams/live")
    suspend fun getAllStreams(
        @Header("Authorization") token: String
    ): Response<List<StreamDto>>

    

    @GET("api/streaming/streams/history/me")
    suspend fun getMyStreams(
        @Header("Authorization") token: String
    ): Response<List<MyStreamDto>>

    

    @GET("api/streaming/streams/search")
    suspend fun searchStreams(
        @Header("Authorization") token: String,
        @Query("q") query: String,
        @Query("live_only") liveOnly: Boolean = true
    ): Response<List<SearchStreamDto>>

    

    @POST("api/streaming/streams/start")
    suspend fun startStream(
        @Header("Authorization") token: String,
        @Body body: StartStreamRequest
    ): Response<ActiveStreamDto>

    

    @POST("api/streaming/streams/end/{stream_code}")
    suspend fun endStream(
        @Header("Authorization") token: String,
        @Path("stream_code") streamCode: String
    ): Response<Unit>
}
