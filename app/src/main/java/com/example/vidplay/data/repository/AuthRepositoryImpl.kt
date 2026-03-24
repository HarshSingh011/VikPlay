package com.example.vidplay.data.repository

import com.example.vidplay.data.source.remote.AuthApiService
import com.example.vidplay.data.source.remote.dto.ErrorResponse
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
import com.example.vidplay.data.source.remote.dto.toDomain
import com.example.vidplay.domain.model.LoginData
import com.example.vidplay.domain.model.RegistrationData
import com.example.vidplay.domain.model.VerificationData
import com.example.vidplay.domain.model.ForgotPasswordData
import com.example.vidplay.domain.model.VerifyForgotPasswordOtpData
import com.example.vidplay.domain.model.ResetPasswordData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource
import com.google.gson.Gson

/**
 * Concrete implementation of [AuthRepository].
 * Lives in the data layer — the domain layer only ever sees the interface.
 *
 * All network calls are wrapped in try/catch so the ViewModel always
 * receives a typed [Resource] and never an unhandled exception.
 */
class AuthRepositoryImpl(
    private val apiService: AuthApiService
) : AuthRepository {

    private val gson = Gson()

    override suspend fun login(email: String, password: String): Resource<LoginData> {
        return safeLoginApiCall {
            apiService.login(LoginRequest(email = email, password = password))
        }
    }

    override suspend fun register(username: String, email: String, password: String): Resource<RegistrationData> {
        return safeRegisterApiCall {
            apiService.register(RegisterRequest(username = username, email = email, password = password))
        }
    }

    override suspend fun verifyRegistration(email: String, otp: String): Resource<VerificationData> {
        return safeVerifyApiCall {
            apiService.verifyRegistration(VerifyRegistrationRequest(email = email, otp = otp))
        }
    }

    override suspend fun verifyForgotPasswordOtp(email: String, otp: String): Resource<VerifyForgotPasswordOtpData> {
        return safeVerifyForgotPasswordOtpApiCall {
            apiService.verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequest(email = email, otp = otp))
        }
    }

    override suspend fun forgotPassword(email: String): Resource<ForgotPasswordData> {
        return safeForgotPasswordApiCall {
            apiService.forgotPassword(ForgotPasswordRequest(email = email))
        }
    }

    override suspend fun resetPassword(email: String, newPassword: String, confirmPassword: String): Resource<ResetPasswordData> {
        return safeResetPasswordApiCall {
            apiService.resetPassword(ResetPasswordRequest(email = email, newPassword = newPassword, confirmPassword = confirmPassword))
        }
    }

    // Helpers
    // ---------------------------------------------------------------------------

    private suspend fun safeLoginApiCall(
        call: suspend () -> retrofit2.Response<LoginResponse>
    ): Resource<LoginData> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Resource.Success(body.toDomain())
                else Resource.Error("Empty response from server")
            } else {
                // Extract error detail from API response
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        errorResponse.detail
                    } else {
                        "API error ${response.code()}: ${response.message()}"
                    }
                } catch (e: Exception) {
                    "API error ${response.code()}: ${response.message()}"
                }
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    private suspend fun safeRegisterApiCall(
        call: suspend () -> retrofit2.Response<RegisterResponse>
    ): Resource<RegistrationData> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Resource.Success(body.toDomain())
                else Resource.Error("Empty response from server")
            } else {
                // Extract error detail from API response
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        errorResponse.detail
                    } else {
                        "API error ${response.code()}: ${response.message()}"
                    }
                } catch (e: Exception) {
                    "API error ${response.code()}: ${response.message()}"
                }
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    private suspend fun safeVerifyApiCall(
        call: suspend () -> retrofit2.Response<VerifyRegistrationResponse>
    ): Resource<VerificationData> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Resource.Success(body.toDomain())
                else Resource.Error("Empty response from server")
            } else {
                // Extract error detail from API response
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        errorResponse.detail
                    } else {
                        "API error ${response.code()}: ${response.message()}"
                    }
                } catch (e: Exception) {
                    "API error ${response.code()}: ${response.message()}"
                }
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    private suspend fun safeForgotPasswordApiCall(
        call: suspend () -> retrofit2.Response<ForgotPasswordResponse>
    ): Resource<ForgotPasswordData> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Resource.Success(body.toDomain())
                else Resource.Error("Empty response from server")
            } else {
                // Extract error detail from API response
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        errorResponse.detail
                    } else {
                        "API error ${response.code()}: ${response.message()}"
                    }
                } catch (e: Exception) {
                    "API error ${response.code()}: ${response.message()}"
                }
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    private suspend fun safeVerifyForgotPasswordOtpApiCall(
        call: suspend () -> retrofit2.Response<VerifyForgotPasswordOtpResponse>
    ): Resource<VerifyForgotPasswordOtpData> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Resource.Success(body.toDomain())
                else Resource.Error("Empty response from server")
            } else {
                // Extract error detail from API response
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        errorResponse.detail
                    } else {
                        "API error ${response.code()}: ${response.message()}"
                    }
                } catch (e: Exception) {
                    "API error ${response.code()}: ${response.message()}"
                }
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    private suspend fun safeResetPasswordApiCall(
        call: suspend () -> retrofit2.Response<ResetPasswordResponse>
    ): Resource<ResetPasswordData> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Resource.Success(body.toDomain())
                else Resource.Error("Empty response from server")
            } else {
                // Extract error detail from API response
                val errorMessage = try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        errorResponse.detail
                    } else {
                        "API error ${response.code()}: ${response.message()}"
                    }
                } catch (e: Exception) {
                    "API error ${response.code()}: ${response.message()}"
                }
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unexpected error")
        }
    }
}
