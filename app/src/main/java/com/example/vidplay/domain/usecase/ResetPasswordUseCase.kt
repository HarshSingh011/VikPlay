package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.ResetPasswordData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource
import javax.inject.Inject

/**
 * Use case for resetting user password.
 * Encapsulates the business logic for password reset operations.
 */
class ResetPasswordUseCase @Inject constructor(private val authRepository: AuthRepository) {

    suspend operator fun invoke(
        email: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<ResetPasswordData> {
        return authRepository.resetPassword(email, newPassword, confirmPassword)
    }
}
