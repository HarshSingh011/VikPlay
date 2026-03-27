package com.example.vidplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vidplay.domain.model.LoginData
import com.example.vidplay.domain.usecase.LoginUseCase
import com.example.vidplay.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    suspend fun login(email: String, password: String): Resource<LoginData> {
        return loginUseCase(email = email, password = password)
    }
}
