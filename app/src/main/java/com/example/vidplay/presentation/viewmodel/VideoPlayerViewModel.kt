package com.example.vidplay.presentation.viewmodel

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class VideoPlayerViewModel(
    private val context: Context,
    private val videoUriString: String
) : ViewModel() {

    private val videoUri = Uri.parse(videoUriString)
    private var player: ExoPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var currentVolumeLevel: Float = 0.5f

    val isPlaying: MutableState<Boolean> = mutableStateOf(false)
    val currentPosition: MutableState<Long> = mutableStateOf(0L)
    val duration: MutableState<Long> = mutableStateOf(0L)
    val brightness: MutableState<Float> = mutableStateOf(0.5f)
    val volume: MutableState<Float> = mutableStateOf(0.5f)
    val isFullscreen: MutableState<Boolean> = mutableStateOf(false)
    val videoTitle: MutableState<String> = mutableStateOf("")
    val hasSubtitles: MutableState<Boolean> = mutableStateOf(false)
    val currentSubtitle: MutableState<String> = mutableStateOf("")

    val wasPlayingBeforePip: MutableState<Boolean> = mutableStateOf(false)

    val isInPipMode: MutableState<Boolean> = mutableStateOf(false)

    init {
        setHardwareVolumeControlStream()
        initializePlayer()
        loadVideoTitle()
        startPositionUpdates()
    }

    private fun setHardwareVolumeControlStream() {
        if (context is Activity) {
            context.setVolumeControlStream(AudioManager.STREAM_MUSIC)
        }
    }

    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        player = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            setAudioAttributes(audioAttributes, true)
            prepare()

            applyVolumeToPlayer(currentVolumeLevel)

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val playerDuration = duration
                        val playerIsPlaying = isPlaying

                        this@VideoPlayerViewModel.duration.value = playerDuration
                        this@VideoPlayerViewModel.isPlaying.value = playerIsPlaying
                    } else if (playbackState == Player.STATE_ENDED) {
                        this@VideoPlayerViewModel.isPlaying.value = false
                        wasPlayingBeforePip.value = false
                        Log.d("VideoPlayerViewModel", "Video playback ended")
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    this@VideoPlayerViewModel.isPlaying.value = playing

                    if (playing) {
                        wasPlayingBeforePip.value = true
                    }
                }

                override fun onCues(cueList: List<Cue>) {
                    this@VideoPlayerViewModel.hasSubtitles.value = cueList.isNotEmpty()
                    val subtitleText = cueList.firstOrNull()?.text?.toString() ?: ""
                    this@VideoPlayerViewModel.currentSubtitle.value = subtitleText
                }
            })
        }
    }

    private fun applyVolumeToPlayer(volumeLevel: Float) {
        player?.let { exoPlayer ->
            exoPlayer.volume = volumeLevel
            this.currentVolumeLevel = volumeLevel
            this.volume.value = volumeLevel
        }
    }

    private fun loadVideoTitle() {
        try {
            val projection = arrayOf(MediaStore.Video.Media.DISPLAY_NAME)
            context.contentResolver.query(
                videoUri, projection, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val title = cursor.getString(nameIndex)
                        videoTitle.value = title
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VideoPlayerViewModel", "Error loading video title: ${e.message}", e)
            videoTitle.value = "Unknown Video"
        }
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (isActive) {
                player?.let { exoPlayer ->
                    val position = exoPlayer.currentPosition
                    currentPosition.value = position
                }
                delay(500)
            }
        }
    }

    fun getExoPlayer(): ExoPlayer? {
        return player
    }

    fun getPipPlayerView(context: Context): PlayerView {
        return PlayerView(context).apply {
            player = this@VideoPlayerViewModel.player
            useController = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)

            setOnTouchListener { _, _ -> true }

            setUseController(false)
            controllerAutoShow = false
            controllerHideOnTouch = false

            isFocusable = false
            isClickable = false
        }
    }

    fun setInPipMode(inPipMode: Boolean) {
        val previousPipMode = isInPipMode.value
        isInPipMode.value = inPipMode

        if (inPipMode && !previousPipMode) {
            wasPlayingBeforePip.value = isPlaying.value

            playVideo()
            Log.d("VideoPlayerViewModel", "Entered PiP mode, auto-playing video")
        } else if (!inPipMode && previousPipMode) {
            if (!wasPlayingBeforePip.value && isPlaying.value) {
                pauseVideo()
                Log.d("VideoPlayerViewModel", "Exited PiP mode, restoring previous pause state")
            }
        }

        Log.d("VideoPlayerViewModel", "Set PiP mode: $inPipMode")
    }

    fun getPlayerView(context: Context): PlayerView {
        return PlayerView(context).apply {
            player = this@VideoPlayerViewModel.player
            useController = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)

            if (isInPipMode.value) {
                visibility = View.GONE
                setOnTouchListener { _, _ -> true }
            } else {
                visibility = View.VISIBLE
                setOnTouchListener(null)
            }
        }
    }

    fun replayVideo() {
        player?.let { exoPlayer ->
            exoPlayer.seekTo(0)
            exoPlayer.play()
            isPlaying.value = true
            wasPlayingBeforePip.value = false
            Log.d("VideoPlayerViewModel", "Video replaying from beginning")
        }
    }

    fun togglePlayPause() {
        if (isInPipMode.value) {
            Log.d("VideoPlayerViewModel", "Ignoring play/pause toggle in PiP mode")
            return
        }

        player?.let { exoPlayer ->
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                replayVideo()
                return
            }

            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                wasPlayingBeforePip.value = false
            } else {
                exoPlayer.play()
                wasPlayingBeforePip.value = true
            }
            isPlaying.value = exoPlayer.isPlaying
        }
    }

    fun playVideo() {
        player?.let { exoPlayer ->
            if (!exoPlayer.isPlaying) {
                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                    exoPlayer.seekTo(0)
                    Log.d("VideoPlayerViewModel", "Video ended, restarting from beginning")
                }

                exoPlayer.play()
                isPlaying.value = true
                wasPlayingBeforePip.value = true
                Log.d("VideoPlayerViewModel", "Video playing")
            }
        }
    }

    fun pauseVideo() {
        player?.let { exoPlayer ->
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                isPlaying.value = false
                Log.d("VideoPlayerViewModel", "Video paused")
            }
        }
    }

    fun seekTo(position: Long) {
        if (isInPipMode.value) {
            Log.d("VideoPlayerViewModel", "Ignoring seek in PiP mode")
            return
        }

        player?.seekTo(position)
    }

    fun rewind10Seconds() {
        player?.let { exoPlayer ->
            val newPosition = (exoPlayer.currentPosition - 10_000).coerceAtLeast(0)
            exoPlayer.seekTo(newPosition)
        }
    }

    fun forward10Seconds() {
        player?.let { exoPlayer ->
            val newPosition = (exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration)
            exoPlayer.seekTo(newPosition)
        }
    }

    fun setBrightness(brightnessValue: Float) {
        if (isInPipMode.value) {
            Log.d("VideoPlayerViewModel", "Ignoring brightness change in PiP mode")
            return
        }

        brightness.value = brightnessValue
        (context as? Activity)?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightnessValue
            window.attributes = layoutParams
        }
    }

    fun setVolume(volumeLevel: Float) {
        val normalizedVolume = volumeLevel.coerceIn(0f, 1f)
        applyVolumeToPlayer(normalizedVolume)
    }

    fun increaseVolume() {
        val newVolume = (currentVolumeLevel + 0.1f).coerceAtMost(1.0f)
        applyVolumeToPlayer(newVolume)
    }

    fun decreaseVolume() {
        val newVolume = (currentVolumeLevel - 0.1f).coerceAtLeast(0.0f)
        applyVolumeToPlayer(newVolume)
    }

    fun toggleFullscreen() {
        if (isInPipMode.value) {
            Log.d("VideoPlayerViewModel", "Ignoring fullscreen toggle in PiP mode")
            return
        }

        val newFullscreenState = !isFullscreen.value
        isFullscreen.value = newFullscreenState

        (context as? Activity)?.window?.let { window ->
            if (newFullscreenState) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                        )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
    }
}

@UnstableApi
class VideoPlayerViewModelFactory(
    private val context: Context,
    private val videoUri: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoPlayerViewModel::class.java)) {
            return VideoPlayerViewModel(context, videoUri) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}