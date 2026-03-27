package com.example.vidplay.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vidplay.domain.model.VerificationData
import com.example.vidplay.domain.model.VerifyForgotPasswordOtpData
import com.example.vidplay.domain.usecase.VerifyRegistrationUseCase
import com.example.vidplay.domain.usecase.VerifyForgotPasswordOtpUseCase
import com.example.vidplay.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val verifyRegistrationUseCase: VerifyRegistrationUseCase,
    private val verifyForgotPasswordOtpUseCase: VerifyForgotPasswordOtpUseCase
) : ViewModel() {

    suspend fun verifyRegistrationOtp(email: String, otp: String): Resource<VerificationData> {
        return verifyRegistrationUseCase(email = email, otp = otp)
    }

    suspend fun verifyForgotPasswordOtp(email: String, otp: String): Resource<VerifyForgotPasswordOtpData> {
        return verifyForgotPasswordOtpUseCase(email = email, otp = otp)
    }
}
