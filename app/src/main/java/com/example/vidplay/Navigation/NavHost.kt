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
import com.example.vidplay.ui.VideoSection.Page1Screen
import com.example.vidplay.ui.VideoSection.VideoPlayerScreen
import com.example.vidplay.ui.streaming.AllStreamShownScreen
import com.example.vidplay.ui.streaming.LiveStreamingScreen
import com.example.vidplay.ui.streaming.TokenTakenPageScreen
import com.example.vidplay.ui.streaming.ViewerStreamingScreen
import com.example.vidplay.ui.CallSection.CallScreen
import com.example.vidplay.presentation.viewmodel.StreamViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vidplay.presentation.viewmodel.VideoPlayerViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


@Composable
fun MyAppNavHost(
    navController: NavHostController = rememberNavController(),
    onVideoPlayingStateChanged: (Boolean) -> Unit = {},
    pipHandler: PipHandler,
    onVideoPlayerViewModelCreated: (VideoPlayerViewModel) -> Unit = {}
) {
    // One shared ViewModel instance scoped to the Activity — both AllStreamShownScreen
    // and LiveStreamingScreen must read from the same state (startStreamState).
    val streamViewModel: StreamViewModel = viewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController = navController)
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