@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.vidplay.Navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.media3.common.util.UnstableApi
import com.example.vidplay.presentation.pip.PipHandler
import com.example.vidplay.ui.MainActivity
import com.example.vidplay.ui.MainScreen
import com.example.vidplay.ui.VideoSection.Page1Screen
import com.example.vidplay.ui.VideoSection.VideoPlayerScreen
import com.example.vidplay.viewmodels.VideoPlayerViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


@Composable
fun MyAppNavHost(
    navController: NavHostController = rememberNavController(),
    onVideoPlayingStateChanged: (Boolean) -> Unit = {},
    pipHandler: PipHandler,
    onVideoPlayerViewModelCreated: (VideoPlayerViewModel) -> Unit = {}
) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController = navController)
        }
        composable("page1") {
            Page1Screen(navController = navController)
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