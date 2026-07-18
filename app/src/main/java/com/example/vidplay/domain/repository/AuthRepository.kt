package com.example.vidplay.domain.repository

import com.example.vidplay.domain.model.LoginData
import com.example.vidplay.domain.model.RegistrationData
import com.example.vidplay.domain.model.VerificationData
import com.example.vidplay.domain.model.ForgotPasswordData
import com.example.vidplay.domain.model.VerifyForgotPasswordOtpData
import com.example.vidplay.domain.model.ResetPasswordData
import com.example.vidplay.util.Resource

interface AuthRepository {

    

    suspend fun login(email: String, password: String): Resource<LoginData>

    

    suspend fun register(username: String, email: String, password: String): Resource<RegistrationData>

    

    suspend fun verifyRegistration(email: String, otp: String): Resource<VerificationData>

    

    suspend fun forgotPassword(email: String): Resource<ForgotPasswordData>

    

    suspend fun verifyForgotPasswordOtp(email: String, otp: String): Resource<VerifyForgotPasswordOtpData>

    

    suspend fun resetPassword(email: String, newPassword: String, confirmPassword: String): Resource<ResetPasswordData>
}
