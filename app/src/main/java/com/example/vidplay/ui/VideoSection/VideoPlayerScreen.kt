@file:OptIn(androidx.media3.common.util.UnstableApi::class)

    package com.example.vidplay.ui.VideoSection

    import android.view.View
    import androidx.compose.foundation.background
    import androidx.compose.foundation.gestures.detectTapGestures
    import androidx.compose.foundation.layout.*
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.rounded.ArrowBack
    import androidx.compose.material.icons.rounded.BrightnessHigh
    import androidx.compose.material.icons.rounded.Forward10
    import androidx.compose.material.icons.rounded.Fullscreen
    import androidx.compose.material.icons.rounded.FullscreenExit
    import androidx.compose.material.icons.rounded.Pause
    import androidx.compose.material.icons.rounded.PlayArrow
    import androidx.compose.material.icons.rounded.Replay10
    import androidx.compose.material.icons.rounded.VolumeDown
    import androidx.compose.material.icons.rounded.VolumeUp
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.input.pointer.pointerInput
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.viewinterop.AndroidView
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.media3.ui.PlayerView
    import androidx.navigation.NavController
    import com.example.vidplay.presentation.pip.PipHandler
    import com.example.vidplay.presentation.viewmodel.VideoPlayerViewModel
    import com.example.vidplay.presentation.viewmodel.VideoPlayerViewModelFactory
    import kotlinx.coroutines.launch
    import java.util.concurrent.TimeUnit

    @Composable
    fun VideoPlayerScreen(
        navController: NavController,
        videoUri: String,
        pipHandler: PipHandler,
        onPlayingStateChanged: (Boolean) -> Unit = {},
        onVideoPlayerViewModelCreated: (VideoPlayerViewModel) -> Unit = {}
    ) {
        val context = LocalContext.current

        val viewModel: VideoPlayerViewModel = viewModel(
            factory = VideoPlayerViewModelFactory(LocalContext.current, videoUri)
        )

        LaunchedEffect(viewModel) {
            onVideoPlayerViewModelCreated(viewModel)
            android.util.Log.d("VideoPlayerScreen", "Notified about ViewModel creation")
        }

        android.util.Log.d("VideoPlayerScreen", "Received video URI: $videoUri")

        val isInPipMode = pipHandler.isInPipMode.collectAsState().value

        LaunchedEffect(Unit) {
            pipHandler.onPlayVideo = {
                viewModel.playVideo()
            }

            pipHandler.onPauseVideo = {
                viewModel.pauseVideo()
            }

            pipHandler.onForward = {
                viewModel.forward10Seconds()
            }

            pipHandler.onRewind = {
                viewModel.rewind10Seconds()
            }

            pipHandler.onReplayVideo = {
                viewModel.replayVideo()
            }
        }

        LaunchedEffect(viewModel.isPlaying.value) {
            onPlayingStateChanged(viewModel.isPlaying.value)
        }

        LaunchedEffect(viewModel.getExoPlayer()?.playbackState) {
            viewModel.getExoPlayer()?.let { player ->
                val isCompleted = player.playbackState == androidx.media3.common.Player.STATE_ENDED
                pipHandler.updateVideoCompletedState(isCompleted)
                android.util.Log.d("VideoPlayerScreen", "Video completion state: $isCompleted")
            }
        }

        if (isInPipMode) {
            PipModeVideoPlayer(viewModel)
        } else {
            NormalModeVideoPlayer(
                navController = navController,
                viewModel = viewModel,
                pipHandler = pipHandler
            )
        }
    }

    @Composable
    private fun PipModeVideoPlayer(viewModel: VideoPlayerViewModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    val pipPlayerView = PlayerView(ctx).apply {
                        player = viewModel.getExoPlayer()
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        hideController()

                        setOnTouchListener { _, _ -> true }

                        controllerAutoShow = false
                        controllerHideOnTouch = false
                        controllerShowTimeoutMs = 0

                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                        findViewById<View>(androidx.media3.ui.R.id.exo_controller)?.visibility = View.GONE
                        findViewById<View>(androidx.media3.ui.R.id.exo_overlay)?.visibility = View.GONE

                        overlayFrameLayout?.removeAllViews()
                        overlayFrameLayout?.isClickable = false
                        overlayFrameLayout?.isFocusable = false

                        android.util.Log.d("VideoPlayerScreen", "Created specialized PiP player view with no UI")
                    }

                    pipPlayerView.alpha = 0f

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        pipPlayerView.alpha = 1f
                    }, 100)

                    pipPlayerView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @Composable
    private fun NormalModeVideoPlayer(
        navController: NavController,
        viewModel: VideoPlayerViewModel,
        pipHandler: PipHandler
    ) {
        val showControls = remember { mutableStateOf(true) }
        val showBrightnessControl = remember { mutableStateOf(false) }
        val showVolumeControl = remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(key1 = Unit) {
            kotlinx.coroutines.delay(3000)
            if (showControls.value) {
                showControls.value = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    viewModel.getPlayerView(ctx).apply {
                        useController = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls.value = !showControls.value
                                if (showControls.value) {
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(3000) 
                                        showControls.value = false
                                    }
                                }
                            }
                        )
                    }
            )

            if (viewModel.hasSubtitles.value && viewModel.currentSubtitle.value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                ) {
                    Text(
                        text = viewModel.currentSubtitle.value,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(8.dp)
                            .fillMaxWidth()
                    )
                }
            }

            if (showControls.value) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = viewModel.videoTitle.value,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (showBrightnessControl.value) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BrightnessHigh,
                                contentDescription = "Brightness",
                                tint = Color.White
                            )
                            Slider(
                                value = viewModel.brightness.value,
                                onValueChange = { viewModel.setBrightness(it) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    if (showVolumeControl.value) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = "Volume",
                                tint = Color.White
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.decreaseVolume() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.VolumeDown,
                                        contentDescription = "Decrease Volume",
                                        tint = Color.White
                                    )
                                }

                                Text(
                                    text = "${(viewModel.volume.value * 100).toInt()}%",
                                    color = Color.White
                                )

                                IconButton(onClick = { viewModel.increaseVolume() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.VolumeUp,
                                        contentDescription = "Increase Volume",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.rewind10Seconds() }) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.togglePlayPause()
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (viewModel.isPlaying.value) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (viewModel.isPlaying.value) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.forward10Seconds() }) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(viewModel.currentPosition.value),
                            color = Color.White
                        )

                        Slider(
                            value = viewModel.currentPosition.value.toFloat(),
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..viewModel.duration.value.toFloat(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        Text(
                            text = formatDuration(viewModel.duration.value),
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { showBrightnessControl.value = !showBrightnessControl.value }) {
                            Icon(
                                imageVector = Icons.Rounded.BrightnessHigh,
                                contentDescription = "Brightness",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = { showVolumeControl.value = !showVolumeControl.value }) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = "Volume",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = { viewModel.toggleFullscreen() }) {
                            Icon(
                                imageVector = if (viewModel.isFullscreen.value) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }