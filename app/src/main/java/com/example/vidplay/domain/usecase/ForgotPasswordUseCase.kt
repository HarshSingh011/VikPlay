package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.ForgotPasswordData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(private val repository: AuthRepository) {

    suspend operator fun invoke(email: String): Resource<ForgotPasswordData> {
        return repository.forgotPassword(email)
    }
}
