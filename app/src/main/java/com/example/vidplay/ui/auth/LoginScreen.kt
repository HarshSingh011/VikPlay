package com.example.vidplay.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.vidplay.Navigation.Routes
import com.example.vidplay.presentation.viewmodel.LoginViewModel
import com.example.vidplay.ui.theme.VidPlayTheme
import com.example.vidplay.util.Resource
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Ensure cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            // Cancel all coroutines when screen is destroyed
            scope.coroutineContext.cancelChildren()
        }
    }

    // Regex patterns
    val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$")

    fun validateEmail(emailInput: String): String {
        return when {
            emailInput.isEmpty() -> "Email is required"
            !emailInput.matches(emailPattern) -> "Invalid email format"
            else -> ""
        }
    }

    fun validatePassword(passwordInput: String): String {
        return when {
            passwordInput.isEmpty() -> "Password is required"
            passwordInput.length < 8 -> "Password must be at least 8 characters"
            !passwordInput.matches(passwordPattern) -> "Password must contain uppercase, lowercase, number, and special character"
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
            LoginScreenLoading()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Title
                Text(
                text = "VidPlay",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Login Title
            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 24.dp)
            )

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    emailError = ""
                },
                label = { Text("Email") },
                placeholder = { Text("Enter your email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = "Email")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
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

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordError = ""
                },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "Password")
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                isError = passwordError.isNotEmpty()
            )

            // Password Error Message
            if (passwordError.isNotEmpty()) {
                Text(
                    text = passwordError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp, start = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Login Error Message
            if (loginError.isNotEmpty()) {
                Text(
                    text = loginError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp, start = 16.dp)
                )
            }

            // Forgot Password Link
            TextButton(
                onClick = {
                    navController.navigate(Routes.EMAIL_VERIFY)
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "Forgot Password?",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Login Button
            Button(
                onClick = {
                    emailError = validateEmail(email)
                    passwordError = validatePassword(password)
                    loginError = ""

                    if (emailError.isEmpty() && passwordError.isEmpty()) {
                        isLoading = true
                        scope.launch {
                            // Call the domain layer use case via viewModel
                            val result = viewModel.login(email = email, password = password)
                            
                            when (result) {
                                is Resource.Success -> {
                                    // Navigate to LOCAL_STORAGE screen on successful login
                                    navController.navigate(Routes.LOCAL_STORAGE) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                }
                                is Resource.Error -> {
                                    loginError = result.message
                                    isLoading = false
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
                    .padding(bottom = 16.dp)
            ) {
                Text("Login")
            }

            // Divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    "Don't have an account?",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                Divider(modifier = Modifier.weight(1f))
            }

            // Create Account Link
            TextButton(
                onClick = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.LOGIN) { inclusive = false }
                    }
                }
            ) {
                Text(
                    "Create a New Account",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5"
)
@Composable
fun LoginScreenPreview() {
    VidPlayTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "VidPlay",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = false
            ) {
                Text("Login")
            }
        }
    }
}
