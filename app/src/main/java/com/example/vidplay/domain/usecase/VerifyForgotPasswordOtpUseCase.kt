package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.VerifyForgotPasswordOtpData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource

/**
 * Single-responsibility use case: verify forgot password OTP with email and OTP code.
 * The ViewModel calls this; the use case talks only to the repository interface.
 */
class VerifyForgotPasswordOtpUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(email: String, otp: String): Resource<VerifyForgotPasswordOtpData> {
        return repository.verifyForgotPasswordOtp(email, otp)
    }
}
