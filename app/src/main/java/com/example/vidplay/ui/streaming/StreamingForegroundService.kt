package com.example.vidplay.ui.streaming

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.vidplay.data.source.remote.RetrofitClient
import com.example.vidplay.data.webrtc.StreamingSession
import com.example.vidplay.data.webrtc.WebRtcBroadcastManager
import com.example.vidplay.ui.MainActivity
import com.example.vidplay.util.PreferenceHelper
import kotlinx.coroutines.runBlocking

class StreamingForegroundService : Service() {

    companion object {
        private const val TAG        = "StreamingFgService"
        private const val NOTIF_ID   = 1001
        private const val CHANNEL_ID = "vidplay_streaming"

        const val EXTRA_STREAM_CODE = "stream_code"
        const val EXTRA_STREAM_KEY  = "stream_key"
        const val EXTRA_TITLE       = "title"

        fun buildStartIntent(
            ctx: Context,
            streamCode: String,
            streamKey: String,
            title: String
        ) = Intent(ctx, StreamingForegroundService::class.java).apply {
            putExtra(EXTRA_STREAM_CODE, streamCode)
            putExtra(EXTRA_STREAM_KEY,  streamKey)
            putExtra(EXTRA_TITLE,       title)
        }
    }

    private var streamCode: String? = null

    
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code  = intent?.getStringExtra(EXTRA_STREAM_CODE) ?: return START_NOT_STICKY
        val key   = intent.getStringExtra(EXTRA_STREAM_KEY)   ?: return START_NOT_STICKY
        val title = intent.getStringExtra(EXTRA_TITLE)        ?: "Live Stream"
        streamCode = code

        
        val notif = buildNotification(title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            else
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            startForeground(NOTIF_ID, notif, type)
        } else {
            startForeground(NOTIF_ID, notif)
        }

        
        if (StreamingSession.manager == null) {
            val token = PreferenceHelper(this).token
            val mgr   = WebRtcBroadcastManager(this, code, key, token)
            mgr.start()
            StreamingSession.manager = mgr
            Log.d(TAG, "WebRTC broadcaster started — stream $code")
        }

        return START_STICKY
    }

    

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved — ending stream $streamCode")
        val code = streamCode
        if (code != null) {
            try {
                val token = PreferenceHelper(this).token
                runBlocking {
                    RetrofitClient.streamApiService.endStream("Bearer $token", code)
                }
            } catch (e: Exception) {
                Log.e(TAG, "endStream (task removed) failed: ${e.message}")
            }
        }
        releaseManager()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        releaseManager()
        releaseWakeLocks()
        super.onDestroy()
    }

    

    private fun releaseManager() {
        StreamingSession.manager?.release()
        StreamingSession.manager = null
    }

    @Suppress("DEPRECATION")
    private fun acquireWakeLocks() {
        
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VidPlay:StreamingWakeLock")
            .apply { acquire() }

        
        wifiLock = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "VidPlay:StreamingWifiLock")
            .apply { acquire() }

        Log.d(TAG, "WakeLock + WifiLock acquired")
    }

    private fun releaseWakeLocks() {
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) {}
        try { if (wifiLock?.isHeld == true) wifiLock?.release() } catch (_: Exception) {}
        Log.d(TAG, "WakeLock + WifiLock released")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your live stream running in the background"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String) = run {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("● LIVE — $title")
            .setContentText("Tap to return to stream controls")
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    
    override fun onBind(intent: Intent?): IBinder? = null
}
