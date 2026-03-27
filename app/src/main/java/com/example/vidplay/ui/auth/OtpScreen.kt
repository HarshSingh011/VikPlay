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
import com.example.vidplay.ui.theme.VidPlayTheme
import com.example.vidplay.util.Resource
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren

/**
 * OTP Verification Screen that handles both registration and forgot password flows
 * @param navController Navigation controller
 * @param email User email address
 * @param flowType "registration" or "forgotPassword" - determines which API to call and where to navigate
 */
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

    val snackbarHostState = remember { SnackbarHostState() }

    // Determine flow type based on route parameter
    val isRegistrationFlow = flowType == "registration"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            // Show skeleton loading screen
            OtpScreenLoading()
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
                text = "Verify Your Email",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Subtitle with email
            Text(
                text = "Enter the 6-digit code sent to\n$email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            // OTP Input Fields
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
                                // User typed a digit when field already has a value
                                newValue.length > 1 && otpValues[index].isNotEmpty() -> {
                                    // Field already has value, put new digit in next empty field
                                    if (index < 5) {
                                        updatedValues[index + 1] = newValue.last().toString()
                                        otpValues = updatedValues
                                        focusRequesters[index + 1].requestFocus()
                                    }
                                    // Don't update current field - keep it as is by not setting it
                                }
                                // User typed a digit in empty field
                                newValue.length == 1 && newValue.all { it.isDigit() } -> {
                                    updatedValues[index] = newValue
                                    otpValues = updatedValues
                                    // Auto-move to next field
                                    if (index < 5) {
                                        focusRequesters[index + 1].requestFocus()
                                    }
                                }
                                // User pressed backspace and field had a value
                                newValue.isEmpty() && otpValues[index].isNotEmpty() -> {
                                    updatedValues[index] = ""
                                    otpValues = updatedValues
                                    // Move focus to previous field
                                    if (index > 0) {
                                        focusRequesters[index - 1].requestFocus()
                                    }
                                }
                            }
                        },
                        onBackspaceOnEmpty = {
                            // Handle backspace when field is already empty
                            if (index > 0) {
                                val updatedValues = otpValues.toMutableList()
                                // Clear previous field
                                updatedValues[index - 1] = ""
                                otpValues = updatedValues
                                // Move focus to previous field
                                focusRequesters[index - 1].requestFocus()
                            }
                        },
                        focusRequester = focusRequesters[index],
                        enabled = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Verify Button
            Button(
                onClick = {
                    isLoading = true
                    val otp = otpValues.joinToString("")
                    
                    scope.launch {
                        val result = if (isRegistrationFlow) {
                            // Registration flow: verify registration OTP via viewModel
                            viewModel.verifyRegistrationOtp(email = email, otp = otp)
                        } else {
                            // Forgot password flow: verify forgot password OTP via viewModel
                            viewModel.verifyForgotPasswordOtp(email = email, otp = otp)
                        }
                        
                        when (result) {
                            is Resource.Success -> {
                                // Navigate based on flow type
                                if (isRegistrationFlow) {
                                    // Registration flow: navigate to LOGIN
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.OTP.replace("{email}", email)) { inclusive = true }
                                    }
                                } else {
                                    // Forgot password flow: navigate to FORGOT_PASSWORD
                                    navController.navigate(Routes.FORGOT_PASSWORD.replace("{email}", email)) {
                                        popUpTo(Routes.OTP_FORGOT_PASSWORD.replace("{email}", email)) { inclusive = true }
                                    }
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 16.dp),
                enabled = otpValues.all { it.isNotEmpty() }
            ) {
                Text("Verify")
            }

            // Resend Code
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Didn't receive the code? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { /* TODO: Handle resend */ }
                ) {
                    Text("Resend", color = MaterialTheme.colorScheme.primary)
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
    // Create TextFieldValue with cursor at the end
    val textFieldValue = TextFieldValue(
        text = value,
        selection = TextRange(value.length)
    )

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            // Allow digit input and empty, pass through for parent to handle logic
            if (newValue.text.isEmpty() || newValue.text.all { it.isDigit() }) {
                onValueChange(newValue.text)
            }
        },
        modifier = modifier
            .size(56.dp)
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                // Detect backspace on empty field
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

@Preview(showBackground = true)
@Composable
fun OtpScreenPreview() {
    VidPlayTheme {
        OtpScreen(navController = rememberNavController(), email = "test@example.com")
    }
}
