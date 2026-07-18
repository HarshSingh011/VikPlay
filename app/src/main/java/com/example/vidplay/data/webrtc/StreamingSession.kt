package com.example.vidplay.data.webrtc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object StreamingSession {
    var manager: WebRtcBroadcastManager? by mutableStateOf(null)
}
