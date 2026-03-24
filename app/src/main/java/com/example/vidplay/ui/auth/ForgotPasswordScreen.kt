package com.example.vidplay.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vidplay.Navigation.Routes
import com.example.vidplay.data.repository.AuthRepositoryImpl
import com.example.vidplay.data.source.remote.RetrofitClient
import com.example.vidplay.domain.usecase.ResetPasswordUseCase
import com.example.vidplay.util.PasswordValidator
import com.example.vidplay.util.Resource
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(navController: NavController, email: String) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordsMatch by remember { mutableStateOf(true) }
    var passwordError by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize the use case (domain layer)
    val authRepository = AuthRepositoryImpl(RetrofitClient.authApiService)
    val resetPasswordUseCase = ResetPasswordUseCase(authRepository)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }

            // Title
            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            // Subtitle with email
            Text(
                text = "Create a new password for\n$email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 32.dp)
            )

            // New Password Field
            OutlinedTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    passwordError = if (it.isNotEmpty() && !PasswordValidator.isValidPassword(it)) {
                        PasswordValidator.getPasswordErrorMessage(it)
                    } else {
                        ""
                    }
                    resetError = ""
                },
                label = { Text("New Password") },
                placeholder = { Text("Enter new password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "Password")
                },
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = passwordError.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !isLoading
            )

            // Password error message
            if (passwordError.isNotEmpty()) {
                Text(
                    passwordError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordsMatch = newPassword == it
                    resetError = ""
                },
                label = { Text("Confirm Password") },
                placeholder = { Text("Confirm new password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "Confirm Password")
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = !passwordsMatch && confirmPassword.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !isLoading
            )

            // Error message for password mismatch
            if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                Text(
                    "Passwords don't match",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 24.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Reset Password Button
            Button(
                onClick = {
                    // Validate before making API call
                    if (!PasswordValidator.isValidPassword(newPassword)) {
                        passwordError = PasswordValidator.getPasswordErrorMessage(newPassword)
                        return@Button
                    }

                    if (newPassword != confirmPassword) {
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        val result = resetPasswordUseCase(email, newPassword, confirmPassword)
                        when (result) {
                            is Resource.Success -> {
                                // Navigate to login on success
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.FORGOT_PASSWORD.replace("{email}", email)) { inclusive = true }
                                }
                            }
                            is Resource.Error -> {
                                isLoading = false
                                snackbarHostState.showSnackbar(
                                    message = result.message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                            is Resource.Loading -> {
                                // Already showing loading state
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 16.dp),
                enabled = newPassword.isNotEmpty() && 
                         confirmPassword.isNotEmpty() && 
                         passwordsMatch && 
                         passwordError.isEmpty() &&
                         !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Reset Password")
                }
            }

            // Password strength indicator
            if (newPassword.isNotEmpty()) {
                PasswordStrengthIndicator(password = newPassword)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to Login Link
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Remember your password? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.FORGOT_PASSWORD.replace("{email}", email)) { inclusive = true }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Login", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Snackbar for error messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun PasswordStrengthIndicator(password: String) {
    val strength = when {
        password.length < 6 -> PasswordStrength.WEAK
        password.length < 10 || !password.any { it.isDigit() } -> PasswordStrength.MEDIUM
        else -> PasswordStrength.STRONG
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Password Strength:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Box(
            modifier = Modifier
                .height(4.dp)
                .weight(1f)
                .background(strength.color, MaterialTheme.shapes.small)
        )
    }
}

enum class PasswordStrength(val color: androidx.compose.ui.graphics.Color) {
    WEAK(androidx.compose.ui.graphics.Color.Red),
    MEDIUM(androidx.compose.ui.graphics.Color.Yellow),
    STRONG(androidx.compose.ui.graphics.Color.Green)
}
