package com.example.vidplay.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.vidplay.Navigation.Routes
import com.example.vidplay.presentation.viewmodel.LoginViewModel
import com.example.vidplay.ui.theme.AuthTheme
import com.example.vidplay.ui.theme.DiscordTextInput
import com.example.vidplay.util.Resource
import com.example.vidplay.util.PreferenceHelper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.makeapp.vikplay.R
import com.example.vidplay.ui.components.CustomOutlinedTextField
import android.util.Log

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    
    DisposableEffect(Unit) {
        onDispose {
            
            scope.coroutineContext.cancelChildren()
        }
    }

    
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
            LoginScreenLoading()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vikicon),
                        contentDescription = "VidPlay app icon",
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "VikPlay",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 24.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        isError = emailError.isNotEmpty(),
                        singleLine = true
                    )

                    if (emailError.isNotEmpty()) {
                        Text(
                            text = emailError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 8.dp, start = 16.dp)
                        )
                    }

                    CustomOutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = ""
                        },
                        placeholder = { Text("Enter your password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password")
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.padding(bottom = 8.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        isError = passwordError.isNotEmpty(),
                        singleLine = true
                    )

                    if (passwordError.isNotEmpty()) {
                        Text(
                            text = passwordError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 8.dp, start = 16.dp)
                        )
                    }

                    if (loginError.isNotEmpty()) {
                        Text(
                            text = loginError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 0.dp, start = 16.dp)
                        )
                    }

                    TextButton(
                        onClick = {
                            navController.navigate(Routes.EMAIL_VERIFY)
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            "Forgot Password?",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            emailError = validateEmail(email)
                            passwordError = validatePassword(password)
                            loginError = ""

                            if (emailError.isEmpty() && passwordError.isEmpty()) {
                                isLoading = true
                                scope.launch {
                                    val result = viewModel.login(email = email, password = password)
                                    
                                    when (result) {
                                        is Resource.Success -> {
                                            val token = result.data?.accessToken
                                            if (token.isNullOrEmpty()) {
                                                Log.e("LoginScreen", "Login returned empty token!")
                                                loginError = "Login failed: No token received from server"
                                                isLoading = false
                                            } else {
                                                
                                                val extractedUsername = email.substringBefore("@")
                                                val preferences = PreferenceHelper(context)
                                                preferences.token = token
                                                preferences.username = extractedUsername
                                                navController.navigate(Routes.LOCAL_STORAGE) {
                                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                                }
                                            }
                                        }
                                        is Resource.Error -> {
                                            loginError = result.message
                                            isLoading = false
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
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Login")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Don't have an account?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                navController.navigate(Routes.REGISTER) {
                                    popUpTo(Routes.LOGIN) { inclusive = false }
                                }
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 0.dp)
                        ) {
                            Text(
                                "Create a New Account",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
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
fun LoginScreenPreview() {
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
                "VidPlay",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            CustomOutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Enter email", color = DiscordTextInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                    .height(48.dp),
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = Color.White
                )
            ) {
                Text("Login")
            }
        }
    }
}
