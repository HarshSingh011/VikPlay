package com.example.vidplay.data.source.remote

import com.example.vidplay.data.source.remote.dto.LoginRequest
import com.example.vidplay.data.source.remote.dto.LoginResponse
import com.example.vidplay.data.source.remote.dto.RegisterRequest
import com.example.vidplay.data.source.remote.dto.RegisterResponse
import com.example.vidplay.data.source.remote.dto.VerifyRegistrationRequest
import com.example.vidplay.data.source.remote.dto.VerifyRegistrationResponse
import com.example.vidplay.data.source.remote.dto.ForgotPasswordRequest
import com.example.vidplay.data.source.remote.dto.ForgotPasswordResponse
import com.example.vidplay.data.source.remote.dto.VerifyForgotPasswordOtpRequest
import com.example.vidplay.data.source.remote.dto.VerifyForgotPasswordOtpResponse
import com.example.vidplay.data.source.remote.dto.ResetPasswordRequest
import com.example.vidplay.data.source.remote.dto.ResetPasswordResponse
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

    @POST("auth/verify-registration")
    suspend fun verifyRegistration(
        @Body request: VerifyRegistrationRequest
    ): Response<VerifyRegistrationResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<ForgotPasswordResponse>

    @POST("auth/verify-forgot-password-otp")
    suspend fun verifyForgotPasswordOtp(
        @Body request: VerifyForgotPasswordOtpRequest
    ): Response<VerifyForgotPasswordOtpResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ResetPasswordResponse>
}
