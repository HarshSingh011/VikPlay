package com.example.vidplay.domain.repository

import com.example.vidplay.domain.model.LoginData
import com.example.vidplay.domain.model.RegistrationData
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
}
