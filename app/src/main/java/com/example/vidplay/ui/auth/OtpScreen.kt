package com.example.vidplay.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.vidplay.Navigation.Routes
import com.example.vidplay.presentation.viewmodel.OtpViewModel
import com.example.vidplay.ui.theme.AuthTheme
import com.example.vidplay.ui.theme.DiscordTextInput
import com.example.vidplay.util.Resource
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.cancelChildren
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun OtpScreen(
    navController: NavController,
    email: String,
    flowType: String = "registration",
    viewModel: OtpViewModel = hiltViewModel()
) {
    val focusRequesters = List(6) { FocusRequester() }
    var otpValues by remember { mutableStateOf(listOf("", "", "", "", "", "")) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    
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

    val snackbarHostState = remember { SnackbarHostState() }

    
    val isRegistrationFlow = flowType == "registration"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            
            OtpScreenLoading()
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Verify Your Email",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Enter the 6-digit code sent to\n$email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 32.dp),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .padding(bottom = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(6) { index ->
                                OtpTextField(
                                    value = otpValues[index],
                                    onValueChange = { newValue ->
                                        val updatedValues = otpValues.toMutableList()

                                        when {
                                            newValue.length > 1 && otpValues[index].isNotEmpty() -> {
                                                if (index < 5) {
                                                    updatedValues[index + 1] = newValue.last().toString()
                                                    otpValues = updatedValues
                                                    focusRequesters[index + 1].requestFocus()
                                                }
                                            }

                                            newValue.length == 1 && newValue.all { it.isDigit() } -> {
                                                updatedValues[index] = newValue
                                                otpValues = updatedValues

                                                if (index < 5) {
                                                    focusRequesters[index + 1].requestFocus()
                                                }
                                            }

                                            newValue.isEmpty() && otpValues[index].isNotEmpty() -> {
                                                updatedValues[index] = ""
                                                otpValues = updatedValues

                                                if (index > 0) {
                                                    focusRequesters[index - 1].requestFocus()
                                                }
                                            }
                                        }
                                    },
                                    onBackspaceOnEmpty = {
                                        if (index > 0) {
                                            val updatedValues = otpValues.toMutableList()

                                            updatedValues[index - 1] = ""
                                            otpValues = updatedValues

                                            focusRequesters[index - 1].requestFocus()
                                        }
                                    },
                                    focusRequester = focusRequesters[index],
                                    enabled = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isLoading = true
                                val otp = otpValues.joinToString("")

                                scope.launch {
                                    val result = if (isRegistrationFlow) {
                                        viewModel.verifyRegistrationOtp(email = email, otp = otp)
                                    } else {
                                        viewModel.verifyForgotPasswordOtp(email = email, otp = otp)
                                    }

                                    when (result) {
                                        is Resource.Success -> {
                                            if (isRegistrationFlow) {
                                                navController.navigate(Routes.LOGIN) {
                                                    popUpTo(Routes.OTP.replace("{email}", email)) { inclusive = true }
                                                }
                                            } else {
                                                navController.navigate(Routes.FORGOT_PASSWORD.replace("{email}", email)) {
                                                    popUpTo(Routes.OTP_FORGOT_PASSWORD.replace("{email}", email)) { inclusive = true }
                                                }
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
                            enabled = otpValues.all { it.isNotEmpty() }
                        ) {
                            Text("Verify")
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                "Didn't receive the code?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 0.dp)
                            ) {
                                Text(
                                    "Resend",
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

@Composable
private fun OtpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onBackspaceOnEmpty: () -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val textFieldValue = TextFieldValue(
        text = value,
        selection = TextRange(value.length)
    )

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            
            if (newValue.text.isEmpty() || newValue.text.all { it.isDigit() }) {
                onValueChange(newValue.text)
            }
        },
        modifier = modifier
            .size(56.dp)
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                
                if (keyEvent.key == Key.Backspace && value.isEmpty()) {
                    onBackspaceOnEmpty()
                    true
                } else {
                    false
                }
            }
            .border(
                width = 2.dp,
                color = if (value.isNotEmpty()) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ),
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        enabled = enabled,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    )
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5",
    backgroundColor = 0xFF1E1E2E
)
@Composable
fun OtpScreenPreview() {
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
                "Enter OTP",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text("6-digit code", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                repeat(6) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("*", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
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
                Text("Verify")
            }
        }
    }
}
