package com.example.vidplay.presentation.viewmodel

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.vidplay.domain.model.MusicItem
import com.example.vidplay.ui.music.MusicPlayerService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MusicPlayerState(
    val currentTrack: MusicItem? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentIndex: Int = -1
)

class MusicPlayerViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var currentPlaylist: List<MusicItem> = emptyList()

    init {
        connectToService()
        startPositionUpdates()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlayerService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.addListener(playerListener)
                syncState()
            } catch (_: Exception) { /* service not yet available */ }
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = controller?.currentMediaItemIndex ?: return
            _state.value = _state.value.copy(
                currentTrack = currentPlaylist.getOrNull(index),
                currentIndex = index,
                duration = controller?.duration?.coerceAtLeast(0L) ?: 0L
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val index = controller?.currentMediaItemIndex ?: -1
                _state.value = _state.value.copy(
                    duration = controller?.duration?.coerceAtLeast(0L) ?: 0L,
                    isPlaying = controller?.isPlaying ?: false,
                    currentIndex = index,
                    currentTrack = currentPlaylist.getOrNull(index)
                )
            }
        }
    }

    private fun syncState() {
        val c = controller ?: return
        val index = c.currentMediaItemIndex
        _state.value = _state.value.copy(
            isPlaying = c.isPlaying,
            currentPosition = c.currentPosition.coerceAtLeast(0L),
            duration = c.duration.coerceAtLeast(0L),
            currentTrack = currentPlaylist.getOrNull(index),
            currentIndex = index
        )
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (isActive) {
                val c = controller
                if (c != null && c.isPlaying) {
                    _state.value = _state.value.copy(
                        currentPosition = c.currentPosition.coerceAtLeast(0L)
                    )
                }
                delay(500)
            }
        }
    }

    fun playTrack(tracks: List<MusicItem>, startIndex: Int) {
        val c = controller ?: return
        currentPlaylist = tracks
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(track.albumArtUri)
                        .build()
                )
                .build()
        }
        c.setMediaItems(mediaItems, startIndex, 0L)
        c.prepare()
        c.play()
        _state.value = _state.value.copy(
            currentTrack = tracks.getOrNull(startIndex),
            currentIndex = startIndex
        )
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun skipToNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MusicPlayerViewModel(context.applicationContext) as T
    }
}
