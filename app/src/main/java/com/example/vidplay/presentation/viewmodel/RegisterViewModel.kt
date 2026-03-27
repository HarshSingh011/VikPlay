package com.example.vidplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vidplay.domain.usecase.RegisterUseCase
import com.example.vidplay.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    suspend fun register(username: String, email: String, password: String): Resource<Unit> {
        return registerUseCase(username = username, email = email, password = password)
    }
}
