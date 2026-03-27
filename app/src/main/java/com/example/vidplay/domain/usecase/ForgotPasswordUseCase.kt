package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.ForgotPasswordData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource
import javax.inject.Inject

/**
 * Single-responsibility use case: request password reset with email.
 * The ViewModel calls this; the use case talks only to the repository interface.
 */
class ForgotPasswordUseCase @Inject constructor(private val repository: AuthRepository) {

    suspend operator fun invoke(email: String): Resource<ForgotPasswordData> {
        return repository.forgotPassword(email)
    }
}
