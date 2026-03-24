@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.vidplay.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vidplay.presentation.pip.PipHandler
import com.example.vidplay.ui.MainScreen
import com.example.vidplay.ui.VideoSection.LocalStorageScreen
import com.example.vidplay.ui.VideoSection.Page1Screen
import com.example.vidplay.ui.VideoSection.VideoPlayerScreen
import com.example.vidplay.ui.streaming.AllStreamShownScreen
import com.example.vidplay.ui.streaming.LiveStreamingScreen
import com.example.vidplay.ui.streaming.TokenTakenPageScreen
import com.example.vidplay.ui.streaming.ViewerStreamingScreen
import com.example.vidplay.ui.CallSection.CallScreen
import com.example.vidplay.ui.auth.LoginScreen
import com.example.vidplay.ui.auth.RegisterScreen
import com.example.vidplay.ui.auth.OtpScreen
import com.example.vidplay.ui.auth.EmailVerifyScreen
import com.example.vidplay.ui.auth.ForgotPasswordScreen
import com.example.vidplay.presentation.viewmodel.StreamViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vidplay.presentation.viewmodel.VideoPlayerViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.example.vidplay.ui.music.NowPlayingBar


@Composable
fun MyAppNavHost(
    navController: NavHostController = rememberNavController(),
    onVideoPlayingStateChanged: (Boolean) -> Unit = {},
    pipHandler: PipHandler,
    onVideoPlayerViewModelCreated: (VideoPlayerViewModel) -> Unit = {}
) {
    val streamViewModel: StreamViewModel = viewModel()

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: ""
    
    // Check if current route is an auth route
    val isAuthRoute = currentRoute == Routes.LOGIN || 
                     currentRoute == Routes.REGISTER || 
                     currentRoute == Routes.EMAIL_VERIFY ||
                     currentRoute.startsWith("otp") ||
                     currentRoute.startsWith("otpForgotPassword") ||
                     currentRoute.startsWith("forgotPassword")

    Scaffold(
        bottomBar = {
            if (!isAuthRoute) {
                Column {
                    NowPlayingBar()
                    NavigationBar {
                    // Video tab
                    NavigationBarItem(
                        selected = currentRoute == Routes.LOCAL_STORAGE || currentRoute == Routes.PAGE1 || currentRoute.startsWith("videoPlayer"),
                        onClick = {
                            navController.navigate(Routes.LOCAL_STORAGE) { launchSingleTop = true }
                        },
                        icon = { Icon(Icons.Default.Videocam, contentDescription = "Video") },
                        label = { Text("Video") }
                    )

                    // Stream tab
                    NavigationBarItem(
                        selected = currentRoute == Routes.STREAMING,
                        onClick = {
                            navController.navigate(Routes.STREAMING) { launchSingleTop = true }
                        },
                        icon = { Icon(Icons.Default.LiveTv, contentDescription = "Stream") },
                        label = { Text("Stream") }
                    )

                    // Call tab
                    NavigationBarItem(
                        selected = currentRoute == Routes.CALL,
                        onClick = {
                            navController.navigate(Routes.CALL) { launchSingleTop = true }
                        },
                        icon = { Icon(Icons.Default.Call, contentDescription = "Call") },
                        label = { Text("Call") }
                    )
                }
                } // end Column wrapping NowPlayingBar + NavigationBar
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController = navController, startDestination = Routes.LOGIN) {
                // Auth Routes
                composable(Routes.LOGIN) {
                    LoginScreen(navController = navController)
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(navController = navController)
                }
                composable(
                    route = Routes.OTP,
                    arguments = listOf(
                        navArgument("email") { type = NavType.StringType },
                        navArgument("flowType") { type = NavType.StringType; defaultValue = "registration" }
                    )
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: ""
                    val flowType = backStackEntry.arguments?.getString("flowType") ?: "registration"
                    OtpScreen(navController = navController, email = email, flowType = flowType)
                }
                
                composable(
                    route = Routes.OTP_FORGOT_PASSWORD,
                    arguments = listOf(
                        navArgument("email") { type = NavType.StringType },
                        navArgument("flowType") { type = NavType.StringType; defaultValue = "forgotPassword" }
                    )
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: ""
                    val flowType = backStackEntry.arguments?.getString("flowType") ?: "forgotPassword"
                    OtpScreen(navController = navController, email = email, flowType = flowType)
                }
                
                composable(Routes.EMAIL_VERIFY) {
                    EmailVerifyScreen(navController = navController)
                }
                
                composable(
                    route = Routes.FORGOT_PASSWORD,
                    arguments = listOf(
                        navArgument("email") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: ""
                    ForgotPasswordScreen(navController = navController, email = email)
                }
                
                // Main App Routes
                composable("main") {
                    MainScreen(navController = navController)
                }
                composable(Routes.LOCAL_STORAGE) {
                    LocalStorageScreen(navController = navController)
                }
                composable("page1") {
                    Page1Screen(navController = navController)
                }
                composable(Routes.TOKEN_PAGE) {
                    TokenTakenPageScreen(navController = navController)
                }
                composable(Routes.STREAMING) {
                    AllStreamShownScreen(navController = navController, viewModel = streamViewModel)
                }
                composable(Routes.LIVE_STREAM) {
                    LiveStreamingScreen(navController = navController, viewModel = streamViewModel)
                }
                composable(Routes.CALL) {
                    CallScreen(navController = navController)
                }
                composable(
                    route = Routes.VIEW_STREAM,
                    arguments = listOf(
                        navArgument("streamCode")  { type = NavType.StringType },
                        navArgument("streamTitle") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val code  = backStackEntry.arguments?.getString("streamCode")  ?: ""
                    val title = URLDecoder.decode(
                        backStackEntry.arguments?.getString("streamTitle") ?: "",
                        StandardCharsets.UTF_8.toString()
                    )
                    ViewerStreamingScreen(
                        navController = navController,
                        streamCode    = code,
                        streamTitle   = title
                    )
                }
                composable(
                    route = "videoPlayer/{videoUri}",
                    arguments = listOf(
                        navArgument("videoUri") {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val encodedUri = backStackEntry.arguments?.getString("videoUri") ?: ""
                    val decodedUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())

                    VideoPlayerScreen(
                        navController = navController,
                        videoUri = decodedUri,
                        pipHandler = pipHandler,
                        onPlayingStateChanged = onVideoPlayingStateChanged,
                        onVideoPlayerViewModelCreated = onVideoPlayerViewModelCreated
                    )
                }
            }
        }
    }
}