package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.LoginData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource
import javax.inject.Inject

/**
 * Single-responsibility use case: authenticate user with email and password.
 * The ViewModel calls this; the use case talks only to the repository interface.
 */
class LoginUseCase @Inject constructor(private val repository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): Resource<LoginData> {
        return repository.login(email, password)
    }
}
