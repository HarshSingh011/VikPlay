package com.example.vidplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vidplay.domain.usecase.ResetPasswordUseCase
import com.example.vidplay.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    suspend fun resetPassword(email: String, newPassword: String): Resource<Unit> {
        return resetPasswordUseCase(email = email, newPassword = newPassword)
    }
}
