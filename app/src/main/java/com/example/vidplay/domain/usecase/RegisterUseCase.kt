package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.RegistrationData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource

/**
 * Single-responsibility use case: register a new user with username, email, and password.
 * The ViewModel calls this; the use case talks only to the repository interface.
 */
class RegisterUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(username: String, email: String, password: String): Resource<RegistrationData> {
        return repository.register(username, email, password)
    }
}
