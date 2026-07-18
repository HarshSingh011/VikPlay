package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.LoginData
import com.example.vidplay.domain.repository.AuthRepository
import com.example.vidplay.util.Resource
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): Resource<LoginData> {
        return repository.login(email, password)
    }
}
