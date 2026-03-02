@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.vidplay.ui

import android.os.Bundle
import android.util.Log
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vidplay.Navigation.MyAppNavHost
import com.example.vidplay.ui.theme.VidPlayTheme
import com.example.vidplay.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.vidplay.presentation.pip.PipHandler
import androidx.media3.common.util.UnstableApi
import com.example.vidplay.viewmodels.VideoPlayerViewModel

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    private val _isOnVideoPlayerScreen = MutableStateFlow(false)
    val isOnVideoPlayerScreen: StateFlow<Boolean> = _isOnVideoPlayerScreen.asStateFlow()

    private lateinit var pipHandler: PipHandler

    private var currentVideoPlayerViewModel: VideoPlayerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pipHandler = PipHandler(this).apply {
            onPipModeChanged = { isInPipMode ->
                Log.d(TAG, "PiP mode changed to $isInPipMode, notifying ViewModel")
                notifyViewModelOfPipMode(isInPipMode)
            }
        }

        val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            results.forEach { (permission, isGranted) ->
                Log.d(TAG, "Permission $permission: ${if (isGranted) "Granted" else "Denied"}")
            }
        }

        setContent {
            VidPlayTheme {
                val navController = rememberNavController()

                val currentBackStackEntry = navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry.value?.destination?.route ?: ""

                LaunchedEffect(currentRoute) {
                    _isOnVideoPlayerScreen.value = currentRoute.startsWith("videoPlayer")
                    Log.d(TAG, "Current route: $currentRoute, isOnVideoPlayerScreen: ${_isOnVideoPlayerScreen.value}")
                }

                MyAppNavHost(
                    navController = navController,
                    onVideoPlayingStateChanged = { isPlaying ->
                        pipHandler.updatePlayingState(isPlaying)
                        Log.d(TAG, "Video playing state changed: $isPlaying")
                    },
                    pipHandler = pipHandler,
                    onVideoPlayerViewModelCreated = { viewModel ->
                        currentVideoPlayerViewModel = viewModel
                        Log.d(TAG, "Stored reference to VideoPlayerViewModel")
                    }
                )

                LaunchedEffect(Unit) {
                    Log.d(TAG, "Requesting media permissions")
                    PermissionUtils.requestMediaPermissions(requestPermissions)
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        pipHandler.handleIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (_isOnVideoPlayerScreen.value) {
            Log.d(TAG, "User leaving app while on video player screen, entering PiP mode")
            currentVideoPlayerViewModel?.let { viewModel ->
                pipHandler.updatePlayingState(viewModel.isPlaying.value)
            }
            pipHandler.enterPictureInPictureMode()
            currentVideoPlayerViewModel?.playVideo()
            pipHandler.updatePlayingState(true)
        } else {
            Log.d(TAG, "Not entering PiP mode: not on video player screen")
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        Log.d(TAG, "PiP mode changed: $isInPictureInPictureMode")

        notifyViewModelOfPipMode(isInPictureInPictureMode)

        pipHandler.setPipMode(isInPictureInPictureMode)

        Log.d(TAG, "PiP transition complete, UI should now reflect PiP state: $isInPictureInPictureMode")
    }

    override fun onDestroy() {
        super.onDestroy()
        currentVideoPlayerViewModel = null
    }

    override fun onStop() {
        super.onStop()
        if (!pipHandler.isInPipMode.value && _isOnVideoPlayerScreen.value) {
            pipHandler.updatePlayingState(false)
            Log.d(TAG, "App stopped (not PiP), pausing video")
        }
    }

    override fun onResume() {
        super.onResume()
        if (pipHandler.isInPipMode.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pipHandler.updatePipActions()
        }
    }

    private fun notifyViewModelOfPipMode(isInPipMode: Boolean) {
        currentVideoPlayerViewModel?.setInPipMode(isInPipMode)
    }
}