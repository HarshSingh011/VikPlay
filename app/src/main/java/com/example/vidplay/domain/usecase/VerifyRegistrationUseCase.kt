package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.VerificationData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource
import javax.inject.Inject

/**
 * Single-responsibility use case: verify user registration with email and OTP.
 * The ViewModel calls this; the use case talks only to the repository interface.
 */
class VerifyRegistrationUseCase @Inject constructor(private val repository: AuthRepository) {

    suspend operator fun invoke(email: String, otp: String): Resource<VerificationData> {
        return repository.verifyRegistration(email, otp)
    }
}
