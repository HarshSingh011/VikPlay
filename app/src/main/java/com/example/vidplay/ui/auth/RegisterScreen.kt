package com.example.vidplay.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.vidplay.Navigation.Routes
import com.example.vidplay.presentation.viewmodel.RegisterViewModel
import com.example.vidplay.ui.theme.AuthTheme
import com.example.vidplay.ui.theme.DiscordTextInput
import com.example.vidplay.util.PasswordValidator
import com.example.vidplay.util.PreferenceHelper
import com.example.vidplay.util.Resource
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.vidplay.ui.components.CustomOutlinedTextField

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordsMatch by remember { mutableStateOf(true) }
    var usernameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
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

    // Regex patterns
    val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    val usernamePattern = Regex("^[a-zA-Z0-9_]{3,}$")

    fun validateUsername(usernameInput: String): String {
        return when {
            usernameInput.isEmpty() -> "Username is required"
            usernameInput.length < 3 -> "Username must be at least 3 characters"
            !usernameInput.matches(usernamePattern) -> "Username can only contain letters, numbers, and underscores"
            else -> ""
        }
    }

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
            !PasswordValidator.isValidPassword(passwordInput) -> PasswordValidator.getPasswordErrorMessage(
                passwordInput
            )

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
            RegisterScreenLoading()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onBackPressed() }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }

                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 24.dp)
                )

            CustomOutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    usernameError = ""
                },
                placeholder = { Text("Enter username") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Username")
                },
                modifier = Modifier.padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                isError = usernameError.isNotEmpty()
            )

            if (usernameError.isNotEmpty()) {
                Text(
                    text = usernameError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp, start = 16.dp)
                )
            }

            CustomOutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = ""
                },
                placeholder = { Text("Enter your email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = "Email")
                },
                modifier = Modifier.padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = emailError.isNotEmpty()
            )

            if (emailError.isNotEmpty()) {
                Text(
                    text = emailError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp, start = 16.dp)
                )
            }

            CustomOutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = ""
                    if (confirmPassword.isNotEmpty()) {
                        passwordsMatch = password == confirmPassword
                    }
                },
                placeholder = { Text("Enter password") },
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
                modifier = Modifier.padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = passwordError.isNotEmpty()
            )

            if (passwordError.isNotEmpty()) {
                Text(
                    text = passwordError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp, start = 16.dp)
                )
            } else if (password.isNotEmpty() && !PasswordValidator.isValidPassword(password)) {
                Text(
                    text = PasswordValidator.getPasswordErrorMessage(password),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp, start = 16.dp)
                )
            }

            CustomOutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordsMatch = password == it
                },
                placeholder = { Text("Confirm password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "Confirm Password")
                },
                trailingIcon = {
                    IconButton(
                        onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                    ) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = !passwordsMatch && confirmPassword.isNotEmpty(),
                modifier = Modifier.padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                Text(
                    "Passwords don't match",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp, start = 16.dp)
                )
            }

            Button(
                onClick = {
                    usernameError = validateUsername(username)
                    emailError = validateEmail(email)
                    passwordError = validatePassword(password)
                    errorMessage = ""

                    if (usernameError.isEmpty() && emailError.isEmpty() &&
                        passwordError.isEmpty() && passwordsMatch
                    ) {
                        isLoading = true
                        scope.launch {
                            // Call the domain layer use case via viewModel
                            val result = viewModel.register(
                                username = username,
                                email = email,
                                password = password
                            )

                            when (result) {
                                is Resource.Success -> {
                                    // Save username to preferences
                                    PreferenceHelper(context).username = username
                                    // Navigate to OTP screen on successful registration with flowType
                                    navController.navigate(
                                        Routes.OTP.replace(
                                            "{email}",
                                            email
                                        ) + "?flowType=registration"
                                    ) {
                                        popUpTo(Routes.REGISTER) { inclusive = true }
                                    }
                                }

                                is Resource.Error -> {
                                    errorMessage = result.message
                                    isLoading = false
                                    // Show error in snackbar
                                    snackbarHostState.showSnackbar(errorMessage)
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
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                enabled = username.isNotEmpty() && email.isNotEmpty() &&
                        password.isNotEmpty() && confirmPassword.isNotEmpty() &&
                        passwordsMatch
            ) {
                Text("Create Account")
            }

            // Already have an account
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account?", style = MaterialTheme.typography.bodySmall)
                TextButton(
                    onClick = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    },
                    enabled = !isLoading
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

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5",
    backgroundColor = 0xFF1E1E2E
)
@Composable
fun RegisterScreenPreview() {
    AuthTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            CustomOutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Choose username", color = DiscordTextInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(12.dp))
            CustomOutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Enter email", color = DiscordTextInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(12.dp))
            CustomOutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Enter password", color = DiscordTextInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = Color.White
                )
            ) {
                Text("Create Account")
            }
        }
    }
}
