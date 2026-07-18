package com.example.vidplay.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.vidplay.Navigation.Routes
import com.example.vidplay.presentation.viewmodel.ForgotPasswordViewModel
import com.example.vidplay.ui.theme.AuthTheme
import com.example.vidplay.ui.theme.DiscordTextInput
import com.example.vidplay.util.PasswordValidator
import com.example.vidplay.util.Resource
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.vidplay.ui.components.CustomOutlinedTextField

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    email: String,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
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

    
    val onBackPressed = {
        if (isLoading) {
            
            scope.coroutineContext.cancelChildren()
            isLoading = false
        }
        navController.popBackStack()
    }

    
    DisposableEffect(Unit) {
        onDispose {
            
            scope.coroutineContext.cancelChildren()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            ForgotPasswordScreenLoading()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onBackPressed() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Create a new password for\n$email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        )

                        CustomOutlinedTextField(
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
                            placeholder = { Text("Enter new password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = "Password")
                            },
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = passwordError.isNotEmpty(),
                            modifier = Modifier.padding(bottom = 4.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        if (passwordError.isNotEmpty()) {
                            Text(
                                passwordError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(bottom = 16.dp)
                            )
                        }

                        CustomOutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                passwordsMatch = newPassword == it
                                resetError = ""
                            },
                            placeholder = { Text("Confirm new password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = "Confirm Password")
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = !passwordsMatch && confirmPassword.isNotEmpty(),
                            modifier = Modifier.padding(bottom = 8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            enabled = !isLoading
                        )

                        if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                            Text(
                                "Passwords don't match",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(bottom = 24.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (!PasswordValidator.isValidPassword(newPassword)) {
                                    passwordError = PasswordValidator.getPasswordErrorMessage(newPassword)
                                    return@Button
                                }

                                if (newPassword != confirmPassword) {
                                    return@Button
                                }

                                isLoading = true
                                scope.launch {
                                    val result = viewModel.resetPassword(email, newPassword, confirmPassword)
                                    when (result) {
                                        is Resource.Success -> {
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
                            enabled = newPassword.isNotEmpty() &&
                                confirmPassword.isNotEmpty() &&
                                passwordsMatch &&
                                passwordError.isEmpty()
                        ) {
                            Text("Reset Password")
                        }

                        if (newPassword.isNotEmpty()) {
                            PasswordStrengthIndicator(password = newPassword)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                "Remember your password?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.FORGOT_PASSWORD.replace("{email}", email)) { inclusive = true }
                                    }
                                },
                                enabled = !isLoading,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 0.dp)
                            ) {
                                Text(
                                    "Login",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5",
    backgroundColor = 0xFF1E1E2E
)
@Composable
fun ForgotPasswordScreenPreview() {
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
                "Reset Password",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("New Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("Enter new password", color = DiscordTextInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = false,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Confirm Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("Confirm password", color = DiscordTextInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = false,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
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
                Text("Reset Password")
            }
        }
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
