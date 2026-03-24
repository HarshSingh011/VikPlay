package com.example.vidplay.data.source.remote

import com.example.vidplay.data.source.remote.dto.LoginRequest
import com.example.vidplay.data.source.remote.dto.LoginResponse
import com.example.vidplay.data.source.remote.dto.RegisterRequest
import com.example.vidplay.data.source.remote.dto.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>
}
