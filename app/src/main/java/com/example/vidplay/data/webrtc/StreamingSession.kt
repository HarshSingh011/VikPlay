package com.example.vidplay.data.webrtc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-wide singleton that holds the active [WebRtcBroadcastManager].
 * Backed by Compose snapshot state so any composable that reads [manager]
 * automatically recomposes when the service sets or clears it.
 */
object StreamingSession {
    var manager: WebRtcBroadcastManager? by mutableStateOf(null)
}
