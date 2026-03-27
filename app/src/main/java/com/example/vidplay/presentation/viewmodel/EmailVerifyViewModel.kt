package com.example.vidplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vidplay.domain.usecase.ForgotPasswordUseCase
import com.example.vidplay.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EmailVerifyViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {

    suspend fun sendResetCode(email: String): Resource<Unit> {
        return forgotPasswordUseCase(email = email)
    }
}
