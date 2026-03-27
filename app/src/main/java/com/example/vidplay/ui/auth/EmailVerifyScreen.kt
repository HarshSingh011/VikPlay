package com.example.vidplay.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.vidplay.Navigation.Routes
import com.example.vidplay.presentation.viewmodel.EmailVerifyViewModel
import com.example.vidplay.ui.theme.VidPlayTheme
import com.example.vidplay.util.Resource
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren

@Composable
fun EmailVerifyScreen(
    navController: NavController,
    viewModel: EmailVerifyViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cleanup function to cancel scope when back is pressed or screen is destroyed
    val onBackPressed = {
        if (isLoading) {
            // Cancel all ongoing coroutines in this scope
            scope.coroutineContext.cancelChildren()
            isLoading = false
        }
        navController.popBackStack()
    }

    // Ensure cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            // Cancel all coroutines when screen is destroyed
            scope.coroutineContext.cancelChildren()
        }
    }

    // Regex pattern for email validation
    val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateEmail(emailInput: String): String {
        return when {
            emailInput.isEmpty() -> "Email is required"
            !emailInput.matches(emailPattern) -> "Invalid email format"
            else -> ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            // Show skeleton loading screen
            EmailVerifyScreenLoading()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Back Button at Top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onBackPressed() }
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

            // Subtitle
            Text(
                text = "Enter your email address to receive a password reset code",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 32.dp)
            )

            // Email Input Field
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    emailError = ""
                },
                label = { Text("Email Address") },
                placeholder = { Text("Enter your email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = "Email")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = emailError.isNotEmpty()
            )

            // Email Error Message
            if (emailError.isNotEmpty()) {
                Text(
                    text = emailError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp, start = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Verify Button
            Button(
                onClick = {
                    emailError = validateEmail(email)

                    if (emailError.isEmpty()) {
                        isLoading = true
                        scope.launch {
                            // Call the domain layer use case via viewModel
                            val result = viewModel.sendResetCode(email = email)
                            
                            when (result) {
                                is Resource.Success -> {
                                    // Navigate to OTP screen for forgot password flow
                                    navController.navigate(
                                        Routes.OTP_FORGOT_PASSWORD.replace("{email}", email) + "?flowType=forgotPassword"
                                    ) {
                                        popUpTo(Routes.EMAIL_VERIFY) { inclusive = false }
                                    }
                                }
                                is Resource.Error -> {
                                    isLoading = false
                                    // Show error in snackbar
                                    snackbarHostState.showSnackbar(result.message)
                                }
                                is Resource.Loading -> {
                                    // Already handled by isLoading flag
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 16.dp),
                enabled = email.isNotEmpty()
            ) {
                Text("Send Reset Code")
            }

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
                            popUpTo(Routes.EMAIL_VERIFY) { inclusive = true }
                        }
                    }
                ) {
                    Text("Login", color = MaterialTheme.colorScheme.primary)
                }
            }
            }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmailVerifyScreenPreview() {
    VidPlayTheme {
        EmailVerifyScreen(navController = rememberNavController())
    }
}
