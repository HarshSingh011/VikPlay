package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.VerificationData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource
import javax.inject.Inject

class VerifyRegistrationUseCase @Inject constructor(private val repository: AuthRepository) {

    suspend operator fun invoke(email: String, otp: String): Resource<VerificationData> {
        return repository.verifyRegistration(email, otp)
    }
}
