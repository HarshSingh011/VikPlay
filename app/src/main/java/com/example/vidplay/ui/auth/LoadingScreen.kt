package com.example.vidplay.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vidplay.ui.theme.AuthTheme

@Composable
fun SkeletonLoading(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

// Reusable skeleton for a text field row (icon + input area)
@Composable
private fun SkeletonTextField(
    modifier: Modifier = Modifier,
    hasTrailingIcon: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        SkeletonLoading(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Reusable skeleton for a button
@Composable
private fun SkeletonButton(
    modifier: Modifier = Modifier,
    height: Dp = 56.dp
) {
    SkeletonLoading(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(8.dp)
    )
}

// Reusable skeleton for a text line
@Composable
private fun SkeletonTextLine(
    width: Dp,
    height: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    SkeletonLoading(
        modifier = modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(4.dp)
    )
}

/**
 * Skeleton for LoginScreen - matches the layout:
 * - App icon (96dp rounded square)
 * - App name text
 * - "Login" heading
 * - Email text field
 * - Password text field
 * - "Forgot Password?" link
 * - Login button
 * - "Don't have an account? Create..." row
 */
@Composable
fun LoginScreenLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon + Name section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App icon
            SkeletonLoading(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // App name "VikPlay"
            SkeletonTextLine(width = 120.dp, height = 32.dp)
        }

        // Center form section
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // "Login" heading
            SkeletonTextLine(
                width = 80.dp,
                height = 28.dp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Email text field
            SkeletonTextField(
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Password text field
            SkeletonTextField(
                modifier = Modifier.padding(bottom = 8.dp),
                hasTrailingIcon = true
            )

            // "Forgot Password?" link
            SkeletonTextLine(
                width = 120.dp,
                height = 14.dp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 16.dp)
            )

            // Login button
            SkeletonButton(
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // "Don't have an account? Create a New Account" row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonTextLine(width = 140.dp, height = 14.dp)
                Spacer(modifier = Modifier.width(8.dp))
                SkeletonTextLine(width = 150.dp, height = 14.dp)
            }
        }
    }
}

/**
 * Skeleton for RegisterScreen - matches the layout:
 * - Back button
 * - "Create Account" heading
 * - Username text field
 * - Email text field
 * - Password text field
 * - Confirm password text field
 * - Create Account button
 * - "Already have an account? Login" row
 */
@Composable
fun RegisterScreenLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonLoading(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Center form area
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
                // "Create Account" heading
                SkeletonTextLine(
                    width = 200.dp,
                    height = 28.dp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 24.dp)
                )

                // Username text field
                SkeletonTextField(
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Email text field
                SkeletonTextField(
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Password text field
                SkeletonTextField(
                    modifier = Modifier.padding(bottom = 8.dp),
                    hasTrailingIcon = true
                )

                // Confirm password text field
                SkeletonTextField(
                    modifier = Modifier.padding(bottom = 8.dp),
                    hasTrailingIcon = true
                )

                // Create Account button
                SkeletonButton(
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // "Already have an account? Login" row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonTextLine(width = 160.dp, height = 14.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    SkeletonTextLine(width = 50.dp, height = 14.dp)
                }
            }
        }
    }
}

/**
 * Skeleton for OtpScreen - matches the layout:
 * - Back button
 * - "Verify Your Email" heading
 * - Subtitle text
 * - 6 OTP input boxes
 * - Verify button
 * - "Didn't receive the code? Resend" row
 */
@Composable
fun OtpScreenLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonLoading(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Center content
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
                // "Verify Your Email" heading
                SkeletonTextLine(
                    width = 200.dp,
                    height = 28.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Subtitle text "Enter the 6-digit code sent to..."
                SkeletonTextLine(
                    width = 240.dp,
                    height = 16.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SkeletonTextLine(
                    width = 180.dp,
                    height = 16.dp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // 6 OTP input boxes
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) {
                        SkeletonLoading(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Verify button
                SkeletonButton(
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // "Didn't receive the code? Resend" row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    SkeletonTextLine(width = 160.dp, height = 14.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    SkeletonTextLine(width = 60.dp, height = 14.dp)
                }
            }
        }
    }
}

/**
 * Skeleton for EmailVerifyScreen - matches the layout:
 * - Back button (transparent background with rounded shape)
 * - Card containing:
 *   - "Reset Password" heading
 *   - Subtitle text
 *   - Email text field
 *   - "Send Reset Code" button
 *   - "Remember your password? Login" row
 */
@Composable
fun EmailVerifyScreenLoading() {
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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonLoading(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Centered card
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
                        // "Reset Password" heading
                        SkeletonTextLine(
                            width = 180.dp,
                            height = 28.dp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Subtitle text
                        SkeletonTextLine(
                            width = 280.dp,
                            height = 16.dp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Email text field
                        SkeletonTextField(
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // "Send Reset Code" button
                        SkeletonButton(
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        // "Remember your password? Login" row
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            SkeletonTextLine(width = 160.dp, height = 14.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            SkeletonTextLine(width = 50.dp, height = 14.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Skeleton for ForgotPasswordScreen - matches the layout:
 * - Back button
 * - "Reset Password" heading
 * - Subtitle text
 * - New password text field
 * - Confirm password text field
 * - Reset Password button
 * - Password strength indicator
 * - "Remember your password? Login" row
 */
@Composable
fun ForgotPasswordScreenLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonLoading(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Center form area
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
                // "Reset Password" heading
                SkeletonTextLine(
                    width = 180.dp,
                    height = 28.dp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                // Subtitle "Create a new password for..."
                SkeletonTextLine(
                    width = 220.dp,
                    height = 16.dp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )
                SkeletonTextLine(
                    width = 180.dp,
                    height = 16.dp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 32.dp)
                )

                // New password text field
                SkeletonTextField(
                    modifier = Modifier.padding(bottom = 4.dp),
                    hasTrailingIcon = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm password text field
                SkeletonTextField(
                    modifier = Modifier.padding(bottom = 8.dp),
                    hasTrailingIcon = true
                )

                // Reset Password button
                SkeletonButton(
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Password strength indicator skeleton
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonTextLine(width = 130.dp, height = 14.dp)
                    SkeletonLoading(
                        modifier = Modifier
                            .height(4.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "Remember your password? Login" row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    SkeletonTextLine(width = 170.dp, height = 14.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    SkeletonTextLine(width = 50.dp, height = 14.dp)
                }
            }
        }
    }
}

@Composable
fun LoadingOverlay(
    isVisible: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
fun LoginScreenLoadingPreview() {
    AuthTheme {
        LoginScreenLoading()
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
fun RegisterScreenLoadingPreview() {
    AuthTheme {
        RegisterScreenLoading()
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
fun OtpScreenLoadingPreview() {
    AuthTheme {
        OtpScreenLoading()
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
fun EmailVerifyScreenLoadingPreview() {
    AuthTheme {
        EmailVerifyScreenLoading()
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
fun ForgotPasswordScreenLoadingPreview() {
    AuthTheme {
        ForgotPasswordScreenLoading()
    }
}
