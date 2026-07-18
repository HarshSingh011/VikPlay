package com.example.vidplay.ui.music

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.makeapp.vikplay.BuildConfig
import com.makeapp.vikplay.R

@UnstableApi
class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null
    private var player: ExoPlayer? = null
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        
        val pendingIntent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        
        mediaSession = MediaSession.Builder(this, player!!)
            .apply { if (pendingIntent != null) setSessionActivity(pendingIntent) }
            .build()

        
        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        )
            .setChannelNameResourceId(R.string.app_name)
            .setSmallIconResourceId(R.drawable.ic_launcher_foreground)
            .setMediaDescriptionAdapter(createMediaDescriptionAdapter())
            .setNotificationListener(createNotificationListener())
            .build()
            .apply { setPlayer(player) }

        setupPlayerListener()
        debugLog("Music service initialized successfully")
    }

    private fun createMediaDescriptionAdapter() = object : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence =
            player.mediaMetadata.title ?: "VidPlay Music"

        override fun getCurrentContentText(player: Player): CharSequence? =
            player.mediaMetadata.artist ?: "Unknown Artist"

        override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): android.graphics.Bitmap? =
            null

        override fun getCurrentSubText(player: Player): CharSequence? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                PendingIntent.getActivity(
                    this@MusicPlayerService, 0, it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
    }

    private fun createNotificationListener() = object : PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(notificationId: Int, notification: android.app.Notification, ongoing: Boolean) {
            val isPlaying = player?.isPlaying == true || player?.playWhenReady == true
            debugLog("Notification posted: ongoing=$ongoing, isPlaying=$isPlaying")
            
            
            if (isPlaying && !isForeground) {
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(
                            this@MusicPlayerService,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        startForeground(notificationId, notification)
                        isForeground = true
                        debugLog("Started foreground service")
                    } else {
                        debugLog("POST_NOTIFICATIONS permission not granted, cannot start foreground")
                        return
                    }
                } else {
                    startForeground(notificationId, notification)
                    isForeground = true
                    debugLog("Started foreground service")
                }
            }
            
            else if (isPlaying && isForeground) {
                
                getSystemService(NotificationManager::class.java)?.notify(notificationId, notification)
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            debugLog("Notification cancelled by user: $dismissedByUser")
            if (isForeground) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                isForeground = false
            }
        }
    }

    private fun setupPlayerListener() {
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                debugLog("Playback state: ${getStateString(playbackState)}, isPlaying: ${player?.isPlaying}")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                debugLog("Playback changed: isPlaying=$isPlaying, playWhenReady=${player?.playWhenReady}")
            }

            override fun onPlayerError(error: PlaybackException) {
                debugLog("Player error: ${error.message}")
            }
        })
    }

    private fun getStateString(state: Int): String = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN"
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing music"
                getSystemService(NotificationManager::class.java).createNotificationChannel(this)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        try {
            if (isForeground) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                isForeground = false
            }

            notificationManager?.setPlayer(null)
            notificationManager = null

            mediaSession?.release()
            mediaSession = null

            player?.release()
            player = null

            debugLog("Service destroyed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during onDestroy", e)
        }
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "MusicPlayerService"
    }
}
