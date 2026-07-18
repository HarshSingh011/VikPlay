package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.VerifyForgotPasswordOtpData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource
import javax.inject.Inject

class VerifyForgotPasswordOtpUseCase @Inject constructor(private val repository: AuthRepository) {

    suspend operator fun invoke(email: String, otp: String): Resource<VerifyForgotPasswordOtpData> {
        return repository.verifyForgotPasswordOtp(email, otp)
    }
}
