package com.example.vidplay.presentation.pip

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("UnspecifiedRegisterReceiverFlag")
class PipHandler(private val activity: Activity) {
    private val TAG = "PipHandler"
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    private val _isVideoCompleted = MutableStateFlow(false)
    val isVideoCompleted: StateFlow<Boolean> = _isVideoCompleted.asStateFlow()

    var onPlayVideo: () -> Unit = {}
    var onPauseVideo: () -> Unit = {}
    var onForward: () -> Unit = {}
    var onRewind: () -> Unit = {}
    var onReplayVideo: () -> Unit = {}

    var onPipModeChanged: (Boolean) -> Unit = {}

    companion object {
        private const val ACTION_TYPE_PLAY = 1
        private const val ACTION_TYPE_PAUSE = 2
        private const val ACTION_TYPE_FORWARD = 3
        private const val ACTION_TYPE_REWIND = 4
        private const val ACTION_TYPE_REPLAY = 5

        private const val EXTRA_PIP_ACTION = "pip_action_type"
        private const val ACTION_PIP_CONTROL = "com.example.vidplay.ACTION_PIP_CONTROL"

        private const val REQUEST_PLAY = 100
        private const val REQUEST_PAUSE = 101
        private const val REQUEST_FORWARD = 102
        private const val REQUEST_REWIND = 103
        private const val REQUEST_REPLAY = 104
    }

    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Broadcast received: ${intent?.action}, extras: ${intent?.extras}")
            if (intent?.action == ACTION_PIP_CONTROL) {
                mainHandler.post {
                    handlePipAction(intent)
                }
            }
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(
                pipActionReceiver,
                IntentFilter(ACTION_PIP_CONTROL),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            activity.registerReceiver(
                pipActionReceiver,
                IntentFilter(ACTION_PIP_CONTROL)
            )
        }
        Log.d(TAG, "PipHandler initialized and receiver registered")
    }

    fun cleanup() {
        try {
            activity.unregisterReceiver(pipActionReceiver)
            Log.d(TAG, "PipHandler cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
    }

    fun updatePlayingState(isPlaying: Boolean) {
        _isVideoPlaying.value = isPlaying
        if (_isInPipMode.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updatePipActions()
        }
    }

    fun updateVideoCompletedState(isCompleted: Boolean) {
        _isVideoCompleted.value = isCompleted
        if (_isInPipMode.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updatePipActions()
        }
    }

    fun setPipMode(isInPipMode: Boolean) {
        val wasInPipMode = _isInPipMode.value
        _isInPipMode.value = isInPipMode

        if (isInPipMode != wasInPipMode) {
            onPipModeChanged(isInPipMode)

            if (isInPipMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updatePipActions()
            }
        }
    }

    fun enterPictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                try {
                    val params = buildPipParams()
                    activity.enterPictureInPictureMode(params)
                    Log.d(TAG, "Entered PiP mode")
                    setPipMode(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error entering PiP mode: ${e.message}", e)
                }
            } else {
                Log.d(TAG, "PiP not supported on this device")
            }
        } else {
            Log.d(TAG, "PiP requires Android O or higher")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePipActions() {
        if (_isInPipMode.value) {
            try {
                val params = buildPipParams()
                activity.setPictureInPictureParams(params)
                Log.d(TAG, "PiP actions updated: playing=${_isVideoPlaying.value}, completed=${_isVideoCompleted.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating PiP actions: ${e.message}", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(): PictureInPictureParams {
        val aspectRatio = Rational(16, 9)
        val paramsBuilder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)

        val actions = createPipActions()
        paramsBuilder.setActions(actions)

        return paramsBuilder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPipActions(): List<RemoteAction> {
        val actions = mutableListOf<RemoteAction>()

        actions.add(createRemoteAction(
            android.R.drawable.ic_media_rew,
            "Rewind",
            "Rewind 10 seconds",
            ACTION_TYPE_REWIND,
            REQUEST_REWIND
        ))

        if (_isVideoCompleted.value) {
            actions.add(createRemoteAction(
                android.R.drawable.ic_popup_sync,
                "Replay",
                "Replay video",
                ACTION_TYPE_REPLAY,
                REQUEST_REPLAY
            ))
        } else if (_isVideoPlaying.value) {
            actions.add(createRemoteAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                "Pause video",
                ACTION_TYPE_PAUSE,
                REQUEST_PAUSE
            ))
        } else {
            actions.add(createRemoteAction(
                android.R.drawable.ic_media_play,
                "Play",
                "Play video",
                ACTION_TYPE_PLAY,
                REQUEST_PLAY
            ))
        }

        actions.add(createRemoteAction(
            android.R.drawable.ic_media_ff,
            "Forward",
            "Forward 10 seconds",
            ACTION_TYPE_FORWARD,
            REQUEST_FORWARD
        ))

        return actions
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        @DrawableRes iconResId: Int,
        title: String,
        contentDescription: String,
        actionType: Int,
        requestCode: Int
    ): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL).apply {
            putExtra(EXTRA_PIP_ACTION, actionType)
            setPackage(activity.packageName)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            activity.applicationContext,
            requestCode,
            intent,
            flags
        )

        val icon = Icon.createWithResource(activity, iconResId)

        return RemoteAction(
            icon,
            title,
            contentDescription,
            pendingIntent
        )
    }

    private fun handlePipAction(intent: Intent) {
        val actionType = intent.getIntExtra(EXTRA_PIP_ACTION, -1)
        if (actionType != -1) {
            Log.d(TAG, "Received PiP action: $actionType")

            mainHandler.post {
                when (actionType) {
                    ACTION_TYPE_PLAY -> {
                        Log.d(TAG, "PiP Action received: Play")
                        _isVideoPlaying.value = true
                        _isVideoCompleted.value = false
                        onPlayVideo()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            updatePipActions()
                        }
                    }
                    ACTION_TYPE_PAUSE -> {
                        Log.d(TAG, "PiP Action received: Pause")
                        _isVideoPlaying.value = false
                        onPauseVideo()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            updatePipActions()
                        }
                    }
                    ACTION_TYPE_FORWARD -> {
                        Log.d(TAG, "PiP Action received: Forward")
                        onForward()
                    }
                    ACTION_TYPE_REWIND -> {
                        Log.d(TAG, "PiP Action received: Rewind")
                        onRewind()
                    }
                    ACTION_TYPE_REPLAY -> {
                        Log.d(TAG, "PiP Action received: Replay")
                        _isVideoPlaying.value = true
                        _isVideoCompleted.value = false
                        onReplayVideo()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            updatePipActions()
                        }
                    }
                }
            }
        }
    }

    fun handleIntent(intent: Intent?) {
        intent?.let {
            val actionType = it.getIntExtra(EXTRA_PIP_ACTION, -1)
            if (actionType != -1) {
                val pipIntent = Intent(ACTION_PIP_CONTROL).apply {
                    putExtra(EXTRA_PIP_ACTION, actionType)
                    setPackage(activity.packageName)
                }
                mainHandler.post {
                    handlePipAction(pipIntent)
                }
            }
        }
    }
}