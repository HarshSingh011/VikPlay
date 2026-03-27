package com.example.vidplay.ui.auth

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Skeleton loading shimmer effect
 */
@Composable
fun SkeletonLoading(
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.surface,
    highlightColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeletonShimmer")
    val color by infiniteTransition.animateColor(
        initialValue = baseColor,
        targetValue = highlightColor,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonColor"
    )

    Box(
        modifier = modifier
            .background(
                color = color,
                shape = MaterialTheme.shapes.medium
            )
    )
}

/**
 * Login Screen Skeleton Loading
 */
@Composable
fun LoginScreenLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // VidPlay Title skeleton (displaySmall)
        SkeletonLoading(
            modifier = Modifier
                .width(120.dp)
                .height(44.dp)
                .padding(bottom = 32.dp)
        )

        // Login Title skeleton (headlineMedium)
        SkeletonLoading(
            modifier = Modifier
                .align(Alignment.Start)
                .width(100.dp)
                .height(32.dp)
                .padding(bottom = 24.dp)
        )

        // Email field skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp)
        )

        // Email error message space (or empty)
        Spacer(modifier = Modifier.height(16.dp))

        // Password field skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp)
        )

        // Password error message space (or empty)
        Spacer(modifier = Modifier.height(16.dp))

        // Forgot password link skeleton
        SkeletonLoading(
            modifier = Modifier
                .align(Alignment.End)
                .width(110.dp)
                .height(20.dp)
                .padding(bottom = 24.dp)
        )

        // Login button skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(bottom = 16.dp)
        )

        // Divider with text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonLoading(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
            )
            SkeletonLoading(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(120.dp)
                    .height(16.dp)
            )
            SkeletonLoading(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
            )
        }

        // Create account link skeleton
        SkeletonLoading(
            modifier = Modifier
                .width(150.dp)
                .height(20.dp)
        )
    }
}

/**
 * Register Screen Skeleton Loading
 */
@Composable
fun RegisterScreenLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Back button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonLoading(
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
            )
        }

        // Title skeleton
        SkeletonLoading(
            modifier = Modifier
                .align(Alignment.Start)
                .width(150.dp)
                .height(32.dp)
                .padding(bottom = 24.dp)
        )

        // Username field skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp)
        )

        // Username error space
        Spacer(modifier = Modifier.height(16.dp))

        // Email field skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp)
        )

        // Email error space
        Spacer(modifier = Modifier.height(16.dp))

        // Password field skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp)
        )

        // Password error space
        Spacer(modifier = Modifier.height(16.dp))

        // Confirm password field skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 24.dp)
        )

        // Register button skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}

/**
 * OTP Screen Skeleton Loading
 */
@Composable
fun OtpScreenLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Back button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonLoading(
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
            )
        }

        // Title skeleton
        SkeletonLoading(
            modifier = Modifier
                .width(150.dp)
                .height(32.dp)
                .padding(bottom = 8.dp)
        )

        // Subtitle skeleton
        SkeletonLoading(
            modifier = Modifier
                .width(200.dp)
                .height(40.dp)
                .padding(bottom = 32.dp)
        )

        // OTP fields skeleton
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
                        .size(56.dp)
                        .weight(1f)
                )
            }
        }

        // Verify button skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(bottom = 16.dp)
        )

        // Resend code skeleton
        SkeletonLoading(
            modifier = Modifier
                .width(150.dp)
                .height(20.dp)
        )
    }
}

/**
 * Email Verify Screen Skeleton Loading
 */
@Composable
fun EmailVerifyScreenLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Back button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonLoading(
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
            )
        }

        // Title skeleton
        SkeletonLoading(
            modifier = Modifier
                .align(Alignment.Start)
                .width(150.dp)
                .height(32.dp)
                .padding(bottom = 8.dp)
        )

        // Subtitle skeleton
        SkeletonLoading(
            modifier = Modifier
                .align(Alignment.Start)
                .width(250.dp)
                .height(40.dp)
                .padding(bottom = 32.dp)
        )

        // Email field skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp)
        )

        // Email error space
        Spacer(modifier = Modifier.height(16.dp))

        // Send button skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}

/**
 * Forgot Password Screen Skeleton Loading
 */
@Composable
fun ForgotPasswordScreenLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Back button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonLoading(
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
            )
        }

        // Title skeleton
        SkeletonLoading(
            modifier = Modifier
                .align(Alignment.Start)
                .width(150.dp)
                .height(32.dp)
                .padding(bottom = 8.dp)
        )

        // Subtitle skeleton
        SkeletonLoading(
            modifier = Modifier
                .align(Alignment.Start)
                .width(200.dp)
                .height(40.dp)
                .padding(bottom = 32.dp)
        )

        // New password field skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 4.dp)
        )

        // Password error space
        Spacer(modifier = Modifier.height(16.dp))

        // Confirm password field skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 24.dp)
        )

        // Reset button skeleton
        SkeletonLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}

/**
 * Generic loading overlay that prevents interaction
 */
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
