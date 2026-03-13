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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vidplay.Navigation.Routes

@Composable
fun OtpScreen(navController: NavController, email: String) {
    val focusRequesters = List(6) { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var otpValues by remember { mutableStateOf(listOf("", "", "", "", "", "")) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                IconButton(onClick = { navController.popBackStack() }) {
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
                            if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                                val updatedValues = otpValues.toMutableList()
                                updatedValues[index] = newValue

                                // Move to next field if a digit is entered
                                if (newValue.isNotEmpty() && index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                }

                                otpValues = updatedValues
                            }
                        },
                        onBackspace = {
                            if (otpValues[index].isEmpty() && index > 0) {
                                focusRequesters[index - 1].requestFocus()
                                val updatedValues = otpValues.toMutableList()
                                updatedValues[index - 1] = ""
                                otpValues = updatedValues
                            } else {
                                val updatedValues = otpValues.toMutableList()
                                updatedValues[index] = ""
                                otpValues = updatedValues
                            }
                        },
                        focusRequester = focusRequesters[index],
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Verify Button
            Button(
                onClick = {
                    isLoading = true
                    val otp = otpValues.joinToString("")
                    // TODO: Handle OTP verification
                    // Navigate to main screen after successful verification
                    navController.navigate(Routes.LOCAL_STORAGE) {
                        popUpTo(Routes.OTP.replace("{email}", email)) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 16.dp),
                enabled = otpValues.all { it.isNotEmpty() } && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Verify")
                }
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
                TextButton(onClick = { /* TODO: Handle resend */ }) {
                    Text("Resend", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun OtpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            // Handle backspace
            if (newValue.isEmpty() && value.isNotEmpty()) {
                onBackspace()
            } else {
                onValueChange(newValue)
            }
        },
        modifier = modifier
            .size(56.dp)
            .focusRequester(focusRequester)
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
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
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
