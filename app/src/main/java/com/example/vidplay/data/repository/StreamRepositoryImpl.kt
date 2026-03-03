package com.example.vidplay.data.repository

import com.example.vidplay.data.source.remote.StreamApiService
import com.example.vidplay.data.source.remote.dto.MyStreamDto
import com.example.vidplay.data.source.remote.dto.SearchStreamDto
import com.example.vidplay.data.source.remote.dto.StreamDto
import com.example.vidplay.data.source.remote.dto.toDomain
import com.example.vidplay.domain.model.MyStream
import com.example.vidplay.domain.model.Stream
import com.example.vidplay.domain.repository.StreamRepository
import com.example.vidplay.util.Resource

/**
 * Concrete implementation of [StreamRepository].
 * Lives in the data layer — the domain layer only ever sees the interface.
 *
 * All network calls are wrapped in try/catch so the ViewModel always
 * receives a typed [Resource] and never an unhandled exception.
 */
class StreamRepositoryImpl(
    private val apiService: StreamApiService
) : StreamRepository {

    override suspend fun getAllStreams(token: String): Resource<List<Stream>> =
        safeApiCall { apiService.getAllStreams("Token $token") }

    override suspend fun getMyStreams(token: String): Resource<List<MyStream>> =
        safeMyStreamApiCall { apiService.getMyStreams("Token $token") }

    override suspend fun searchStreams(token: String, query: String): Resource<List<MyStream>> =
        safeSearchApiCall { apiService.searchStreams("Token $token", query, liveOnly = true) }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private suspend fun safeApiCall(
        call: suspend () -> retrofit2.Response<List<StreamDto>>
    ): Resource<List<Stream>> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Resource.Success(body.map { it.toDomain() })
                else Resource.Error("Empty response from server")
            } else {
                Resource.Error("API error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    private suspend fun safeMyStreamApiCall(
        call: suspend () -> retrofit2.Response<List<MyStreamDto>>
    ): Resource<List<MyStream>> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Resource.Success(body.map { it.toDomain() })
                else Resource.Error("Empty response from server")
            } else {
                Resource.Error("API error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    private suspend fun safeSearchApiCall(
        call: suspend () -> retrofit2.Response<List<SearchStreamDto>>
    ): Resource<List<MyStream>> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Resource.Success(body.map { it.toDomain() })
                else Resource.Error("Empty response from server")
            } else {
                Resource.Error("API error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unexpected error")
        }
    }
}
