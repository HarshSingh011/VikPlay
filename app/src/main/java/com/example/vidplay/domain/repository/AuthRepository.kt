package com.example.vidplay.domain.repository

import com.example.vidplay.domain.model.LoginData
import com.example.vidplay.domain.model.RegistrationData
import com.example.vidplay.domain.model.VerificationData
import com.example.vidplay.domain.model.ForgotPasswordData
import com.example.vidplay.domain.model.VerifyForgotPasswordOtpData
import com.example.vidplay.domain.model.ResetPasswordData
import com.example.vidplay.util.Resource

/**
 * Contract that the data layer must fulfil for authentication operations.
 * The domain layer depends on this abstraction, never on the concrete impl.
 */
interface AuthRepository {

    /**
     * Authenticate user with email and password.
     * Returns the access token and user information if successful.
     */
    suspend fun login(email: String, password: String): Resource<LoginData>

    /**
     * Register a new user with username, email, and password.
     * Returns a registration confirmation message if successful.
     */
    suspend fun register(username: String, email: String, password: String): Resource<RegistrationData>

    /**
     * Verify user registration with email and OTP.
     * Returns a verification confirmation message if successful.
     */
    suspend fun verifyRegistration(email: String, otp: String): Resource<VerificationData>

    /**
     * Request password reset with email.
     * Returns a confirmation message if email exists.
     */
    suspend fun forgotPassword(email: String): Resource<ForgotPasswordData>

    /**
     * Verify forgot password OTP with email and OTP code.
     * Returns a confirmation message if OTP is valid.
     */
    suspend fun verifyForgotPasswordOtp(email: String, otp: String): Resource<VerifyForgotPasswordOtpData>

    /**
     * Reset password with email and new passwords.
     * Returns a confirmation message if password reset is successful.
     */
    suspend fun resetPassword(email: String, newPassword: String, confirmPassword: String): Resource<ResetPasswordData>
}
