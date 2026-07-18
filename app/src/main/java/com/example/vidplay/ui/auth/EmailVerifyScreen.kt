package com.example.vidplay.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.vidplay.Navigation.Routes
import com.example.vidplay.presentation.viewmodel.EmailVerifyViewModel
import com.example.vidplay.ui.theme.AuthTheme
import com.example.vidplay.ui.theme.DiscordTextInput
import com.example.vidplay.util.Resource
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.vidplay.ui.components.CustomOutlinedTextField

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
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        if (isLoading) {
            
            EmailVerifyScreenLoading()
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onBackPressed() },
                        modifier = Modifier.background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.96f),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Reset Password",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Enter your email address to receive a password reset code",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

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
                                        .padding(bottom = 16.dp, start = 8.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    emailError = validateEmail(email)

                                    if (emailError.isEmpty()) {
                                        isLoading = true
                                        scope.launch {
                                            val result = viewModel.sendResetCode(email = email)

                                            when (result) {
                                                is Resource.Success -> {
                                                    navController.navigate(
                                                        Routes.OTP_FORGOT_PASSWORD.replace(
                                                            "{email}",
                                                            email
                                                        ) + "?flowType=forgotPassword"
                                                    ) {
                                                        popUpTo(Routes.EMAIL_VERIFY) { inclusive = false }
                                                    }
                                                }

                                                is Resource.Error -> {
                                                    isLoading = false
                                                    snackbarHostState.showSnackbar(result.message)
                                                }

                                                is Resource.Loading -> {
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                enabled = email.isNotEmpty()
                            ) {
                                Text("Send Reset Code", fontWeight = FontWeight.SemiBold)
                            }

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Text(
                                    "Remember your password?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        navController.navigate(Routes.LOGIN) {
                                            popUpTo(Routes.EMAIL_VERIFY) { inclusive = true }
                                        }
                                    },
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
            }

            
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
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
fun EmailVerifyScreenPreview() {
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
                "Verify Email",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Enter your email address",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            CustomOutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Enter email", color = DiscordTextInput) },
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
                Text("Send Code")
            }
        }
    }
}
