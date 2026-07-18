package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.RegistrationData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource
import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) {

    suspend operator fun invoke(username: String, email: String, password: String): Resource<RegistrationData> {
        return repository.register(username, email, password)
    }
}
